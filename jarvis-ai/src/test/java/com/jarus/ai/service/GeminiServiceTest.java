package com.jarus.ai.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for {@link GeminiService}.
 *
 * <h2>Purpose</h2>
 * Verifies that the Gemini API interaction layer handles all HTTP response
 * scenarios correctly — especially the {@link GeminiService#verifyKey(String)}
 * method which drives the key-validation UX in the Settings tab.
 *
 * <h2>How to run</h2>
 * <pre>
 *   # From the jarvis-ai/ folder:
 *   ./gradlew test --tests "com.jarus.ai.service.GeminiServiceTest"
 * </pre>
 *
 * <h2>What each test proves</h2>
 * <ul>
 *   <li>{@code verifyKey_returnsVerified}   — valid key + success response → "VERIFIED"</li>
 *   <li>{@code verifyKey_returnsRateLimited} — valid key but 429 throttle → "RATE_LIMITED"</li>
 *   <li>{@code verifyKey_returnsInvalidKey_on401} — wrong key (401) → "INVALID_KEY"</li>
 *   <li>{@code verifyKey_returnsInvalidKey_on403} — wrong key (403) → "INVALID_KEY"</li>
 *   <li>{@code verifyKey_returnsNetworkError} — IOException / connection failure → "NETWORK_ERROR"</li>
 *   <li>{@code verifyKey_returns400AsInvalidKey} — malformed request (400) → "INVALID_KEY"</li>
 * </ul>
 */
@ExtendWith(MockitoExtension.class)
class GeminiServiceTest {

    // ── Mocks ─────────────────────────────────────────────────────────────────

    /**
     * Use a real ObjectMapper so JSON building (createObjectNode) and parsing (readTree) work correctly.
     * @InjectMocks will inject this spy into GeminiService.objectMapper.
     */
    @Spy
    private ObjectMapper objectMapper = new ObjectMapper();

    @Mock
    private WebClient geminiWebClient;

    @Mock
    private WebClient.RequestBodyUriSpec requestBodyUriSpec;

    @Mock
    private WebClient.RequestBodySpec requestBodySpec;

    @Mock
    private WebClient.RequestHeadersSpec<?> requestHeadersSpec;

    @Mock
    private WebClient.ResponseSpec responseSpec;

    @InjectMocks
    private GeminiService geminiService;

    // ── Helpers ───────────────────────────────────────────────────────────────

    private static final String TEST_KEY = "test-api-key-123";

    /** Minimal valid Gemini API JSON response for a "Say OK" prompt. */
    private static final String GEMINI_OK_RESPONSE = """
            {
              "candidates": [{
                "content": {
                  "parts": [{"text": "OK"}]
                }
              }]
            }
            """;

    @SuppressWarnings("unchecked")
    private void stubWebClientSuccess() {
        when(geminiWebClient.post()).thenReturn(requestBodyUriSpec);
        when(requestBodyUriSpec.uri(anyString(), any(), any())).thenReturn(requestBodySpec);
        when(requestBodySpec.header(anyString(), anyString())).thenReturn(requestBodySpec);
        when(requestBodySpec.bodyValue(any())).thenReturn((WebClient.RequestHeadersSpec) requestHeadersSpec);
        when(requestHeadersSpec.retrieve()).thenReturn(responseSpec);
        when(responseSpec.bodyToMono(String.class)).thenReturn(Mono.just(GEMINI_OK_RESPONSE));
    }

    @SuppressWarnings("unchecked")
    private void stubWebClientError(int httpStatus) {
        WebClientResponseException ex = WebClientResponseException.create(
                httpStatus, "HTTP " + httpStatus, null,
                "error body".getBytes(StandardCharsets.UTF_8), StandardCharsets.UTF_8);
        when(geminiWebClient.post()).thenReturn(requestBodyUriSpec);
        when(requestBodyUriSpec.uri(anyString(), any(), any())).thenReturn(requestBodySpec);
        when(requestBodySpec.header(anyString(), anyString())).thenReturn(requestBodySpec);
        when(requestBodySpec.bodyValue(any())).thenReturn((WebClient.RequestHeadersSpec) requestHeadersSpec);
        when(requestHeadersSpec.retrieve()).thenReturn(responseSpec);
        when(responseSpec.bodyToMono(String.class)).thenReturn(Mono.error(ex));
    }

    @SuppressWarnings("unchecked")
    private void stubWebClientRuntimeError() {
        when(geminiWebClient.post()).thenReturn(requestBodyUriSpec);
        when(requestBodyUriSpec.uri(anyString(), any(), any())).thenReturn(requestBodySpec);
        when(requestBodySpec.header(anyString(), anyString())).thenReturn(requestBodySpec);
        when(requestBodySpec.bodyValue(any())).thenReturn((WebClient.RequestHeadersSpec) requestHeadersSpec);
        when(requestHeadersSpec.retrieve()).thenReturn(responseSpec);
        when(responseSpec.bodyToMono(String.class)).thenReturn(Mono.error(new RuntimeException("connection refused")));
    }

    // ── Tests ─────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("verifyKey returns VERIFIED when Gemini responds with 200")
    void verifyKey_returnsVerified() {
        // Arrange
        stubWebClientSuccess();

        // Act
        String result = geminiService.verifyKey(TEST_KEY);

        // Assert
        assertThat(result).isEqualTo("VERIFIED");
    }

    @Test
    @DisplayName("verifyKey returns RATE_LIMITED when Gemini responds with 429")
    void verifyKey_returnsRateLimited() {
        // Arrange — simulate a valid key that is currently rate-limited
        stubWebClientError(429);

        // Act
        String result = geminiService.verifyKey(TEST_KEY);

        // Assert
        // Key should be saved (caller decides); status tells frontend to show amber warning
        assertThat(result).isEqualTo("RATE_LIMITED");
    }

    @Test
    @DisplayName("verifyKey returns INVALID_KEY when Gemini responds with 401 Unauthorized")
    void verifyKey_returnsInvalidKey_on401() {
        // Arrange — Gemini rejects key with 401
        stubWebClientError(401);

        // Act
        String result = geminiService.verifyKey(TEST_KEY);

        // Assert — frontend should show red "❌ Invalid key" and NOT save it
        assertThat(result).isEqualTo("INVALID_KEY");
    }

    @Test
    @DisplayName("verifyKey returns INVALID_KEY when Gemini responds with 403 Forbidden")
    void verifyKey_returnsInvalidKey_on403() {
        // Arrange — Gemini rejects key with 403
        stubWebClientError(403);

        // Act
        String result = geminiService.verifyKey(TEST_KEY);

        // Assert
        assertThat(result).isEqualTo("INVALID_KEY");
    }

    @Test
    @DisplayName("verifyKey returns INVALID_KEY when Gemini responds with 400 Bad Request")
    void verifyKey_returns400AsInvalidKey() {
        // Arrange — malformed key format → 400
        stubWebClientError(400);

        // Act
        String result = geminiService.verifyKey(TEST_KEY);

        // Assert — treat 400 as invalid (not a network/timeout issue)
        assertThat(result).isEqualTo("INVALID_KEY");
    }

    @Test
    @DisplayName("verifyKey returns NETWORK_ERROR on unexpected exception (connection refused, timeout)")
    void verifyKey_returnsNetworkError() {
        // Arrange — simulate network-level failure (not an HTTP error)
        stubWebClientRuntimeError();

        // Act
        String result = geminiService.verifyKey(TEST_KEY);

        // Assert — key should still be saved; user is warned verification failed
        assertThat(result).isEqualTo("NETWORK_ERROR");
    }
}
