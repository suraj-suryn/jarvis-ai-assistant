package com.jarus.ai.controller;

import com.jarus.ai.model.ParsedResume;
import com.jarus.ai.model.TailoredResume;
import com.jarus.ai.model.UserSettings;
import com.jarus.ai.repository.JobRepository;
import com.jarus.ai.repository.ResumeRepository;
import com.jarus.ai.repository.TailoredResumeRepository;
import com.jarus.ai.repository.UserRepository;
import com.jarus.ai.service.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.*;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/resume")
public class ResumeController {

    @Autowired private ResumeParserService parserService;
    @Autowired private ResumeBuilderService builderService;
    @Autowired private GeminiService geminiService;
    @Autowired private GcsStorageService gcsService;
    @Autowired private ResumeRepository resumeRepository;
    @Autowired private TailoredResumeRepository tailoredRepository;
    @Autowired private JobRepository jobRepository;
    @Autowired private UserRepository userRepository;

    @PostMapping("/upload")
    public ResponseEntity<?> upload(@RequestParam("file") MultipartFile file,
                                     Authentication authentication) throws Exception {
        String userId = getUserId(authentication);
        byte[] bytes = file.getBytes();
        ParsedResume resume = parserService.parse(bytes, file.getOriginalFilename(), file.getContentType());
        resume.setUserId(userId);
        // Store original in GCS
        String gcsPath = userId + "/resumes/" + System.currentTimeMillis() + "_" + file.getOriginalFilename();
        gcsService.upload(bytes, gcsPath, file.getContentType());
        resume.setGcsPath(gcsPath);
        resumeRepository.save(userId, resume);
        resume.setFullText(null); // Don't return full text in response
        return ResponseEntity.ok(resume);
    }

    @PostMapping("/tailor")
    public ResponseEntity<?> tailor(@RequestBody Map<String, String> req,
                                     Authentication authentication) throws Exception {
        String userId = getUserId(authentication);
        String resumeId = req.get("resumeId");
        String jobId = req.get("jobId");
        if (resumeId == null || jobId == null) return ResponseEntity.badRequest().body("resumeId and jobId required");

        var resume = resumeRepository.findById(userId, resumeId);
        var job = jobRepository.findById(userId, jobId);
        if (resume == null || job == null) return ResponseEntity.notFound().build();

        UserSettings settings = userRepository.getSettings(userId);
        String apiKey = getDecryptedGeminiKey(settings);
        if (apiKey == null) return ResponseEntity.status(HttpStatus.PAYMENT_REQUIRED).body("Gemini API key not configured");

        TailoredResume tailored;
        try {
            tailored = geminiService.tailorResume(resume, job, apiKey);
        } catch (com.jarus.ai.exception.GeminiRateLimitException e) {
            return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS).body(e.getMessage());
        }
        tailored.setUserId(userId);

        // Generate files and store in GCS
        byte[] pdfBytes = builderService.generatePdf(tailored);
        byte[] docxBytes = builderService.generateDocx(tailored);
        String pdfPath = userId + "/tailored/" + tailored.getId() + ".pdf";
        String docxPath = userId + "/tailored/" + tailored.getId() + ".docx";
        gcsService.upload(pdfBytes, pdfPath, "application/pdf");
        gcsService.upload(docxBytes, docxPath, "application/vnd.openxmlformats-officedocument.wordprocessingml.document");
        tailored.setGcsPdfPath(pdfPath);
        tailored.setGcsDocxPath(docxPath);

        tailoredRepository.save(userId, tailored);
        return ResponseEntity.ok(tailored);
    }

    @GetMapping("/download/{tailoredId}")
    public ResponseEntity<byte[]> download(@PathVariable String tailoredId,
                                            @RequestParam(defaultValue = "pdf") String format,
                                            Authentication authentication) {
        String userId = getUserId(authentication);
        TailoredResume tailored = tailoredRepository.findById(userId, tailoredId);
        if (tailored == null) return ResponseEntity.notFound().build();

        String path = "pdf".equals(format) ? tailored.getGcsPdfPath() : tailored.getGcsDocxPath();
        if (path == null) return ResponseEntity.notFound().build();
        byte[] data = gcsService.download(path);
        String mime = "pdf".equals(format) ? "application/pdf"
                : "application/vnd.openxmlformats-officedocument.wordprocessingml.document";
        String ext = "pdf".equals(format) ? ".pdf" : ".docx";
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(mime))
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"tailored-resume" + ext + "\"")
                .body(data);
    }

    @GetMapping("/list")
    public ResponseEntity<List<ParsedResume>> list(Authentication authentication) {
        String userId = getUserId(authentication);
        List<ParsedResume> resumes = resumeRepository.findByUserId(userId);
        resumes.forEach(r -> r.setFullText(null));
        return ResponseEntity.ok(resumes);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable String id, Authentication authentication) {
        String userId = getUserId(authentication);
        ParsedResume resume = resumeRepository.findById(userId, id);
        if (resume != null && resume.getGcsPath() != null) {
            try { gcsService.delete(resume.getGcsPath()); } catch (Exception ignored) {}
        }
        resumeRepository.delete(userId, id);
        return ResponseEntity.noContent().build();
    }

    private String getUserId(Authentication auth) {
        return ((OAuth2AuthenticationToken) auth).getPrincipal().getAttributes().get("sub").toString();
    }

    @Autowired
    private com.jarus.ai.security.EncryptionService encryptionService;

    private String getDecryptedGeminiKey(UserSettings settings) {
        if (settings == null || settings.getEncryptedGeminiKey() == null) return null;
        try { return encryptionService.decrypt(settings.getEncryptedGeminiKey()); }
        catch (Exception e) { return null; }
    }
}
