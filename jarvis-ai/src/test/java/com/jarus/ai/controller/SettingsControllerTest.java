package com.jarus.ai.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.jarus.ai.model.UserSettings;
import com.jarus.ai.repository.UserRepository;
import com.jarus.ai.security.EncryptionService;
import com.jarus.ai.service.GeminiService;
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

import java.util.Map;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.oauth2Login;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Unit / slice tests for {@link SettingsController}.
 *
 * <h2>Purpose</h2>
 * Verifies that the {@code POST /api/settings/gemini-key} endpoint correctly:
 * <ul>
 *   <li>Rejects blank / missing API keys with 400</li>
 *   <li>Returns 400 + INVALID_KEY when GeminiService reports an invalid key (and does NOT save)</li>
 *   <li>Returns 200 + VERIFIED when the key passes verification</li>
 *   <li>Returns 200 + RATE_LIMITED when the key is valid but currently throttled (and DOES save)</li>
 *   <li>Returns 200 + SAVED when verification cannot connect (key saved anyway)</li>
 *   <li>Verifies the {@code GET /api/settings/gemini-key/status} endpoint reports configured state</li>
 * </ul>
 *
 * <h2>How to run</h2>
 * <pre>
 *   ./gradlew test --tests "com.jarus.ai.controller.SettingsControllerTest"
 * </pre>
 *
 * <h2>Step-by-step guide for adding new key-validation scenarios</h2>
 * <ol>
 *   <li>Add a new {@code when(geminiService.verifyKey(any())).thenReturn("YOUR_STATUS")} stub.</li>
 *   <li>POST to {@code /api/settings/gemini-key} with a valid JSON body.</li>
 *   <li>Assert the HTTP status and JSON response fields with {@code jsonPath}.</li>
 *   <li>Verify {@code userRepository.saveSettings()} was or was not called via {@code verify(...)}.</li>
 * </ol>
 */
@WebMvcTest(SettingsController.class)
class SettingsControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean private UserRepository userRepository;
    @MockBean private EncryptionService encryptionService;
    @MockBean private GeminiService geminiService;
    @MockBean private ClientRegistrationRepository clientRegistrationRepository;

    private UserSettings emptySettings;

    private static final String USER_SUB = "test-google-sub-12345";
    private static final String ENCRYPTED_KEY = "encrypted-api-key-abc";

    @BeforeEach
    void setUp() {
        emptySettings = new UserSettings();
        when(userRepository.getSettings(anyString())).thenReturn(emptySettings);
        when(encryptionService.encrypt(anyString())).thenReturn(ENCRYPTED_KEY);
    }

    // ── Helper ────────────────────────────────────────────────────────────────

    private RequestPostProcessor mockOAuth2User() {
        return oauth2Login()
                .attributes(attrs -> attrs.put("sub", USER_SUB));
    }

    // ── POST /api/settings/gemini-key ─────────────────────────────────────────

    @Test
    @DisplayName("POST /gemini-key with blank apiKey → 400 Bad Request")
    void saveGeminiKey_rejectsBlankKey() throws Exception {
        mockMvc.perform(post("/api/settings/gemini-key")
                        .with(mockOAuth2User()).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("apiKey", ""))))
                .andExpect(status().isBadRequest());

        // Key must NOT be saved
        verify(userRepository, never()).saveSettings(anyString(), any());
    }

    @Test
    @DisplayName("POST /gemini-key with missing apiKey field → 400 Bad Request")
    void saveGeminiKey_rejectsMissingKey() throws Exception {
        mockMvc.perform(post("/api/settings/gemini-key")
                        .with(mockOAuth2User()).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest());

        verify(userRepository, never()).saveSettings(anyString(), any());
    }

    @Test
    @DisplayName("POST /gemini-key — INVALID_KEY from Gemini → 400, key NOT saved")
    void saveGeminiKey_invalidKey_returns400AndDoesNotSave() throws Exception {
        // Arrange — GeminiService says the key is invalid
        when(geminiService.verifyKey(anyString())).thenReturn("INVALID_KEY");

        // Act + Assert
        mockMvc.perform(post("/api/settings/gemini-key")
                        .with(mockOAuth2User()).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("apiKey", "INVALID_KEY_xyz"))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value("INVALID_KEY"))
                .andExpect(jsonPath("$.message").isNotEmpty());

        // Critical: key must NOT be persisted when invalid
        verify(userRepository, never()).saveSettings(anyString(), any());
    }

    @Test
    @DisplayName("POST /gemini-key — VERIFIED by Gemini → 200, key saved, status=VERIFIED")
    void saveGeminiKey_validKey_saves_returnsVerified() throws Exception {
        // Arrange — GeminiService confirms key works
        when(geminiService.verifyKey(anyString())).thenReturn("VERIFIED");

        // Act + Assert
        mockMvc.perform(post("/api/settings/gemini-key")
                        .with(mockOAuth2User()).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("apiKey", "AIzaSyValidKeyABC123"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("VERIFIED"))
                .andExpect(jsonPath("$.message").isNotEmpty());

        // Key must be saved exactly once
        verify(userRepository, times(1)).saveSettings(eq(USER_SUB), any());
    }

    @Test
    @DisplayName("POST /gemini-key — RATE_LIMITED by Gemini → 200, key saved, status=RATE_LIMITED")
    void saveGeminiKey_rateLimited_savesKey_returnsRateLimited() throws Exception {
        // Arrange — key is valid but quota exhausted right now
        when(geminiService.verifyKey(anyString())).thenReturn("RATE_LIMITED");

        // Act + Assert
        mockMvc.perform(post("/api/settings/gemini-key")
                        .with(mockOAuth2User()).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("apiKey", "AQ.validOauthToken"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("RATE_LIMITED"))
                .andExpect(jsonPath("$.message").isNotEmpty());

        // Rate-limited key should still be saved (key IS valid)
        verify(userRepository, times(1)).saveSettings(eq(USER_SUB), any());
    }

    @Test
    @DisplayName("POST /gemini-key — NETWORK_ERROR from Gemini → 200, key saved, status=SAVED")
    void saveGeminiKey_networkError_savesKey_returnsSaved() throws Exception {
        // Arrange — couldn't reach Gemini to verify (key saved with warning)
        when(geminiService.verifyKey(anyString())).thenReturn("NETWORK_ERROR");

        // Act + Assert
        mockMvc.perform(post("/api/settings/gemini-key")
                        .with(mockOAuth2User()).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("apiKey", "AIzaSyMaybeValid"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("SAVED"))
                .andExpect(jsonPath("$.message").isNotEmpty());

        // Key should be saved despite network error (user can retry AI calls later)
        verify(userRepository, times(1)).saveSettings(eq(USER_SUB), any());
    }

    // ── GET /api/settings/gemini-key/status ───────────────────────────────────

    @Test
    @DisplayName("GET /gemini-key/status → configured:false when no key stored")
    void geminiKeyStatus_returnsNotConfigured_whenNoKey() throws Exception {
        // Arrange — fresh settings with no key
        emptySettings.setEncryptedGeminiKey(null);

        mockMvc.perform(get("/api/settings/gemini-key/status")
                        .with(mockOAuth2User()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.configured").value(false));
    }

    @Test
    @DisplayName("GET /gemini-key/status → configured:true when key is stored")
    void geminiKeyStatus_returnsConfigured_whenKeyPresent() throws Exception {
        // Arrange — settings already have an encrypted key
        emptySettings.setEncryptedGeminiKey(ENCRYPTED_KEY);

        mockMvc.perform(get("/api/settings/gemini-key/status")
                        .with(mockOAuth2User()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.configured").value(true));
    }
}
