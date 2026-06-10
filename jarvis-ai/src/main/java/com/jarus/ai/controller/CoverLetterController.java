package com.jarus.ai.controller;

import com.jarus.ai.model.*;
import com.jarus.ai.repository.*;
import com.jarus.ai.security.EncryptionService;
import com.jarus.ai.service.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.*;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/cover-letter")
public class CoverLetterController {

    @Autowired private GeminiService geminiService;
    @Autowired private ResumeBuilderService builderService;
    @Autowired private GcsStorageService gcsService;
    @Autowired private CoverLetterRepository coverLetterRepository;
    @Autowired private ResumeRepository resumeRepository;
    @Autowired private JobRepository jobRepository;
    @Autowired private UserRepository userRepository;
    @Autowired private EncryptionService encryptionService;

    @PostMapping("/generate")
    public ResponseEntity<?> generate(@RequestBody Map<String, String> req, Authentication authentication) throws IOException {
        String userId = getUserId(authentication);
        String resumeId = req.get("resumeId");
        String jobId = req.get("jobId");
        if (resumeId == null || jobId == null) return ResponseEntity.badRequest().body("resumeId and jobId required");

        ParsedResume resume = resumeRepository.findById(userId, resumeId);
        JobPost job = jobRepository.findById(userId, jobId);
        if (resume == null || job == null) return ResponseEntity.notFound().build();

        UserSettings settings = userRepository.getSettings(userId);
        if (settings.getEncryptedGeminiKey() == null) {
            return ResponseEntity.status(HttpStatus.PAYMENT_REQUIRED).body("Gemini API key not configured");
        }
        String apiKey;
        try { apiKey = encryptionService.decrypt(settings.getEncryptedGeminiKey()); }
        catch (Exception e) { return ResponseEntity.status(500).body("Failed to retrieve API key"); }

        String content = geminiService.generateCoverLetter(resume, job, apiKey);

        // Build a minimal TailoredResume wrapper to reuse ResumeBuilderService for PDF/DOCX
        TailoredResume wrapper = new TailoredResume();
        wrapper.setId(UUID.randomUUID().toString());
        ResumeSection cl = new ResumeSection();
        cl.setName("COVER LETTER");
        cl.setOriginalContent(content);
        cl.setModifiedContent(content);
        cl.setWasModified(false);
        wrapper.setModifiedSections(List.of(cl));

        byte[] pdfBytes = builderService.generatePdf(wrapper);
        byte[] docxBytes = builderService.generateDocx(wrapper);

        String pdfPath = userId + "/cover-letters/" + wrapper.getId() + ".pdf";
        String docxPath = userId + "/cover-letters/" + wrapper.getId() + ".docx";
        gcsService.upload(pdfBytes, pdfPath, "application/pdf");
        gcsService.upload(docxBytes, docxPath, "application/vnd.openxmlformats-officedocument.wordprocessingml.document");

        CoverLetter cl2 = new CoverLetter();
        cl2.setUserId(userId);
        cl2.setJobId(jobId);
        cl2.setResumeId(resumeId);
        cl2.setContent(content);
        cl2.setGcsPdfPath(pdfPath);
        cl2.setGcsDocxPath(docxPath);
        coverLetterRepository.save(userId, cl2);
        return ResponseEntity.ok(cl2);
    }

    @GetMapping("/download/{id}")
    public ResponseEntity<byte[]> download(@PathVariable String id,
                                            @RequestParam(defaultValue = "pdf") String format,
                                            Authentication authentication) {
        String userId = getUserId(authentication);
        CoverLetter cl = coverLetterRepository.findById(userId, id);
        if (cl == null) return ResponseEntity.notFound().build();

        String path = "pdf".equals(format) ? cl.getGcsPdfPath() : cl.getGcsDocxPath();
        if (path == null) return ResponseEntity.notFound().build();
        byte[] data = gcsService.download(path);
        String mime = "pdf".equals(format) ? "application/pdf"
                : "application/vnd.openxmlformats-officedocument.wordprocessingml.document";
        String ext = "pdf".equals(format) ? ".pdf" : ".docx";
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(mime))
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"cover-letter" + ext + "\"")
                .body(data);
    }

    @GetMapping("/list")
    public ResponseEntity<List<CoverLetter>> list(Authentication authentication) {
        String userId = getUserId(authentication);
        List<CoverLetter> letters = coverLetterRepository.findByUserId(userId);
        letters.forEach(l -> l.setContent(null)); // Don't return full content in list
        return ResponseEntity.ok(letters);
    }

    @GetMapping("/{id}")
    public ResponseEntity<CoverLetter> getOne(@PathVariable String id, Authentication authentication) {
        String userId = getUserId(authentication);
        CoverLetter cl = coverLetterRepository.findById(userId, id);
        if (cl == null) return ResponseEntity.notFound().build();
        return ResponseEntity.ok(cl);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable String id, Authentication authentication) {
        String userId = getUserId(authentication);
        CoverLetter cl = coverLetterRepository.findById(userId, id);
        if (cl == null) return ResponseEntity.notFound().build();
        // Clean up stored files
        if (cl.getGcsPdfPath() != null) try { gcsService.delete(cl.getGcsPdfPath()); } catch (Exception ignored) {}
        if (cl.getGcsDocxPath() != null) try { gcsService.delete(cl.getGcsDocxPath()); } catch (Exception ignored) {}
        coverLetterRepository.delete(userId, id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/write")
    public ResponseEntity<?> write(@RequestBody Map<String, String> req, Authentication authentication) throws IOException {
        String userId = getUserId(authentication);
        String content = req.get("content");
        if (content == null || content.isBlank()) return ResponseEntity.badRequest().body("content required");
        String jobId = req.get("jobId");
        String resumeId = req.get("resumeId");

        TailoredResume wrapper = new TailoredResume();
        wrapper.setId(UUID.randomUUID().toString());
        ResumeSection sec = new ResumeSection();
        sec.setName("COVER LETTER");
        sec.setOriginalContent(content);
        sec.setModifiedContent(content);
        sec.setWasModified(false);
        wrapper.setModifiedSections(List.of(sec));

        byte[] pdfBytes = builderService.generatePdf(wrapper);
        byte[] docxBytes = builderService.generateDocx(wrapper);

        String pdfPath = userId + "/cover-letters/" + wrapper.getId() + ".pdf";
        String docxPath = userId + "/cover-letters/" + wrapper.getId() + ".docx";
        gcsService.upload(pdfBytes, pdfPath, "application/pdf");
        gcsService.upload(docxBytes, docxPath, "application/vnd.openxmlformats-officedocument.wordprocessingml.document");

        CoverLetter cl = new CoverLetter();
        cl.setUserId(userId);
        cl.setJobId(jobId);
        cl.setResumeId(resumeId);
        cl.setContent(content);
        cl.setGcsPdfPath(pdfPath);
        cl.setGcsDocxPath(docxPath);
        coverLetterRepository.save(userId, cl);
        return ResponseEntity.ok(cl);
    }

    private String getUserId(Authentication auth) {
        return ((OAuth2AuthenticationToken) auth).getPrincipal().getAttributes().get("sub").toString();
    }
}
