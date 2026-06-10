package com.jarus.ai.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.jarus.ai.model.CoverLetter;
import com.jarus.ai.model.ParsedResume;
import com.jarus.ai.model.UserSettings;
import com.jarus.ai.repository.CoverLetterRepository;
import com.jarus.ai.repository.JobRepository;
import com.jarus.ai.repository.ResumeRepository;
import com.jarus.ai.repository.UserRepository;
import com.jarus.ai.security.EncryptionService;
import com.jarus.ai.service.GcsStorageService;
import com.jarus.ai.service.GeminiService;
import com.jarus.ai.service.ResumeBuilderService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.oauth2Login;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Unit / slice tests for {@link CoverLetterController}.
 *
 * <h2>Purpose</h2>
 * Tests all five cover-letter endpoints:
 * <ul>
 *   <li>{@code POST /api/cover-letter/write}    — manual cover letter (no Gemini key needed)</li>
 *   <li>{@code GET  /api/cover-letter/{id}}      — retrieve full content</li>
 *   <li>{@code DELETE /api/cover-letter/{id}}    — soft-delete + Cloudinary cleanup</li>
 *   <li>{@code GET  /api/cover-letter/list}      — list (content stripped)</li>
 *   <li>{@code POST /api/cover-letter/generate}  — AI-generated (Gemini key required)</li>
 * </ul>
 *
 * <h2>How to run</h2>
 * <pre>
 *   ./gradlew test --tests "com.jarus.ai.controller.CoverLetterControllerTest"
 * </pre>
 *
 * <h2>Testing guide for future contributors</h2>
 * <pre>
 * Step 1 — Understand the test slice
 *   {@code @WebMvcTest(CoverLetterController.class)} loads ONLY the web layer.
 *   All services and repositories are mocked with {@code @MockBean}.
 *
 * Step 2 — Set up a mock cover letter in @BeforeEach
 *   A reusable {@link CoverLetter} object is created with a known ID so every
 *   test can reference it via {@code CL_ID}.
 *
 * Step 3 — Mock the repository/service call that the endpoint invokes
 *   Use {@code when(repo.findById(USER_SUB, CL_ID)).thenReturn(mockCl)}.
 *
 * Step 4 — Perform the request with oauth2Login() to bypass Spring Security
 *   The {@code attributes("sub", USER_SUB)} sets the OAuth2 subject that
 *   {@code getUserId(authentication)} will return.
 *
 * Step 5 — Assert HTTP status + JSON body fields with {@code jsonPath()}
 *
 * Step 6 — Verify side effects (saves, deletes, Cloudinary calls) with {@code verify()}
 * </pre>
 */
@WebMvcTest(CoverLetterController.class)
class CoverLetterControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean private GeminiService geminiService;
    @MockBean private ResumeBuilderService builderService;
    @MockBean private GcsStorageService gcsService;
    @MockBean private CoverLetterRepository coverLetterRepository;
    @MockBean private ResumeRepository resumeRepository;
    @MockBean private JobRepository jobRepository;
    @MockBean private UserRepository userRepository;
    @MockBean private EncryptionService encryptionService;
    @MockBean private ClientRegistrationRepository clientRegistrationRepository;

    private static final String USER_SUB = "test-google-sub-12345";
    private static final String CL_ID = UUID.randomUUID().toString();
    private static final String JOB_ID = UUID.randomUUID().toString();
    private static final String RESUME_ID = UUID.randomUUID().toString();
    private static final String CONTENT = "Dear Hiring Manager,\n\nI am interested in the role…";

    private CoverLetter mockCl;

    @BeforeEach
    void setUp() {
        mockCl = new CoverLetter();
        mockCl.setId(CL_ID);
        mockCl.setUserId(USER_SUB);
        mockCl.setJobId(JOB_ID);
        mockCl.setResumeId(RESUME_ID);
        mockCl.setContent(CONTENT);
        mockCl.setGcsPdfPath(USER_SUB + "/cover-letters/" + CL_ID + ".pdf");
        mockCl.setGcsDocxPath(USER_SUB + "/cover-letters/" + CL_ID + ".docx");
    }

    private RequestPostProcessor mockOAuth2User() {
        return oauth2Login().attributes(attrs -> attrs.put("sub", USER_SUB));
    }

    // ── POST /api/cover-letter/write ─────────────────────────────────────────

    @Test
    @DisplayName("POST /write with valid content → 200, cover letter saved, PDF generated")
    void write_validContent_returns200AndSaves() throws Exception {
        // Arrange
        when(builderService.generatePdf(any())).thenReturn(new byte[]{1, 2, 3});
        when(builderService.generateDocx(any())).thenReturn(new byte[]{4, 5, 6});
        when(gcsService.upload(any(), anyString(), anyString())).thenReturn("mocked-public-id");
        when(coverLetterRepository.save(eq(USER_SUB), any())).thenAnswer(inv -> {
            CoverLetter cl = inv.getArgument(1);
            cl.setId(CL_ID);
            return cl;
        });

        Map<String, String> body = Map.of("content", CONTENT, "jobId", JOB_ID);

        // Act + Assert
        mockMvc.perform(post("/api/cover-letter/write")
                        .with(mockOAuth2User()).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").value(CONTENT))
                .andExpect(jsonPath("$.jobId").value(JOB_ID));

        // Verify both PDF and DOCX were generated and uploaded
        verify(builderService, times(1)).generatePdf(any());
        verify(builderService, times(1)).generateDocx(any());
        verify(gcsService, times(2)).upload(any(), anyString(), anyString());
        verify(coverLetterRepository, times(1)).save(eq(USER_SUB), any());
    }

    @Test
    @DisplayName("POST /write with blank content → 400 Bad Request, nothing saved")
    void write_blankContent_returns400() throws Exception {
        Map<String, String> body = Map.of("content", "   ");

        mockMvc.perform(post("/api/cover-letter/write")
                        .with(mockOAuth2User()).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isBadRequest());

        verify(coverLetterRepository, never()).save(anyString(), any());
    }

    @Test
    @DisplayName("POST /write with missing content field → 400 Bad Request")
    void write_missingContent_returns400() throws Exception {
        mockMvc.perform(post("/api/cover-letter/write")
                        .with(mockOAuth2User()).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest());

        verify(coverLetterRepository, never()).save(anyString(), any());
    }

    // ── GET /api/cover-letter/{id} ────────────────────────────────────────────

    @Test
    @DisplayName("GET /{id} returns full cover letter with content")
    void getOne_existingId_returnsFullContent() throws Exception {
        when(coverLetterRepository.findById(USER_SUB, CL_ID)).thenReturn(mockCl);

        mockMvc.perform(get("/api/cover-letter/" + CL_ID)
                        .with(mockOAuth2User()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(CL_ID))
                .andExpect(jsonPath("$.content").value(CONTENT))
                .andExpect(jsonPath("$.jobId").value(JOB_ID));
    }

    @Test
    @DisplayName("GET /{id} returns 404 when not found or belongs to another user")
    void getOne_notFound_returns404() throws Exception {
        when(coverLetterRepository.findById(USER_SUB, CL_ID)).thenReturn(null);

        mockMvc.perform(get("/api/cover-letter/" + CL_ID)
                        .with(mockOAuth2User()))
                .andExpect(status().isNotFound());
    }

    // ── DELETE /api/cover-letter/{id} ─────────────────────────────────────────

    @Test
    @DisplayName("DELETE /{id} deletes from DB and cleans up Cloudinary files → 204")
    void delete_existingId_returns204AndCleansUp() throws Exception {
        when(coverLetterRepository.findById(USER_SUB, CL_ID)).thenReturn(mockCl);
        doNothing().when(gcsService).delete(anyString());
        doNothing().when(coverLetterRepository).delete(USER_SUB, CL_ID);

        mockMvc.perform(delete("/api/cover-letter/" + CL_ID)
                        .with(mockOAuth2User()).with(csrf()))
                .andExpect(status().isNoContent());

        // Both PDF and DOCX should be deleted from Cloudinary
        verify(gcsService, times(1)).delete(mockCl.getGcsPdfPath());
        verify(gcsService, times(1)).delete(mockCl.getGcsDocxPath());
        verify(coverLetterRepository, times(1)).delete(USER_SUB, CL_ID);
    }

    @Test
    @DisplayName("DELETE /{id} returns 404 when cover letter not found")
    void delete_notFound_returns404() throws Exception {
        when(coverLetterRepository.findById(USER_SUB, CL_ID)).thenReturn(null);

        mockMvc.perform(delete("/api/cover-letter/" + CL_ID)
                        .with(mockOAuth2User()).with(csrf()))
                .andExpect(status().isNotFound());

        verify(gcsService, never()).delete(anyString());
        verify(coverLetterRepository, never()).delete(anyString(), anyString());
    }

    // ── GET /api/cover-letter/list ────────────────────────────────────────────

    @Test
    @DisplayName("GET /list returns cover letters with content stripped to null")
    void list_returnsLettersWithoutContent() throws Exception {
        // Arrange — two letters in DB
        CoverLetter cl2 = new CoverLetter();
        cl2.setId(UUID.randomUUID().toString());
        cl2.setUserId(USER_SUB);
        cl2.setContent("Some long cover letter text that should NOT be returned in the list.");
        when(coverLetterRepository.findByUserId(USER_SUB)).thenReturn(List.of(mockCl, cl2));

        mockMvc.perform(get("/api/cover-letter/list")
                        .with(mockOAuth2User()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                // Content should be null in the list response (saves bandwidth)
                .andExpect(jsonPath("$[0].content").doesNotExist())
                .andExpect(jsonPath("$[1].content").doesNotExist());
    }

    @Test
    @DisplayName("GET /list returns empty array when user has no cover letters")
    void list_empty_returnsEmptyArray() throws Exception {
        when(coverLetterRepository.findByUserId(USER_SUB)).thenReturn(List.of());

        mockMvc.perform(get("/api/cover-letter/list")
                        .with(mockOAuth2User()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));
    }

    // ── POST /api/cover-letter/generate ──────────────────────────────────────

    @Test
    @DisplayName("POST /generate returns 402 when Gemini key not configured")
    void generate_noGeminiKey_returns402() throws Exception {
        // Arrange — user has no encrypted key
        UserSettings settings = new UserSettings();
        settings.setEncryptedGeminiKey(null);
        when(userRepository.getSettings(USER_SUB)).thenReturn(settings);

        // Provide valid resume + job (key check happens before Gemini call)
        when(resumeRepository.findById(USER_SUB, RESUME_ID)).thenReturn(new ParsedResume());
        when(jobRepository.findById(USER_SUB, JOB_ID)).thenReturn(new com.jarus.ai.model.JobPost());

        Map<String, String> body = Map.of("resumeId", RESUME_ID, "jobId", JOB_ID);

        mockMvc.perform(post("/api/cover-letter/generate")
                        .with(mockOAuth2User()).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isPaymentRequired());

        // Gemini should NOT be called when key is missing
        verify(geminiService, never()).generateCoverLetter(any(), any(), anyString());
    }

    @Test
    @DisplayName("POST /generate returns 404 when resume or job not found")
    void generate_resumeNotFound_returns404() throws Exception {
        // Arrange — resume doesn't exist
        when(resumeRepository.findById(USER_SUB, RESUME_ID)).thenReturn(null);
        when(jobRepository.findById(USER_SUB, JOB_ID)).thenReturn(new com.jarus.ai.model.JobPost());

        Map<String, String> body = Map.of("resumeId", RESUME_ID, "jobId", JOB_ID);

        mockMvc.perform(post("/api/cover-letter/generate")
                        .with(mockOAuth2User()).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("POST /generate returns 400 when resumeId or jobId missing from request body")
    void generate_missingIds_returns400() throws Exception {
        mockMvc.perform(post("/api/cover-letter/generate")
                        .with(mockOAuth2User()).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("resumeId", RESUME_ID))))
                .andExpect(status().isBadRequest());
    }
}
