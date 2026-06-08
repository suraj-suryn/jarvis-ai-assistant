package com.jarus.ai.filter;

import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import java.time.Duration;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class RateLimitInterceptor implements HandlerInterceptor {

    // Separate buckets per user per tier
    private final ConcurrentHashMap<String, Bucket> tailorBuckets = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, Bucket> researchBuckets = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, Bucket> captureBuckets = new ConcurrentHashMap<>();

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        String uri = request.getRequestURI();
        String userId = extractUserId();
        if (userId == null) return true; // Let security handle unauthenticated requests

        Bucket bucket = null;
        if (uri.contains("/resume/tailor") || uri.contains("/cover-letter/generate")) {
            bucket = tailorBuckets.computeIfAbsent(userId, k -> buildBucket(10)); // 10/hr
        } else if (uri.contains("/company/research")) {
            bucket = researchBuckets.computeIfAbsent(userId, k -> buildBucket(20)); // 20/hr
        } else if (uri.contains("/jobs/capture")) {
            bucket = captureBuckets.computeIfAbsent(userId, k -> buildBucket(100)); // 100/hr
        }

        if (bucket != null && !bucket.tryConsume(1)) {
            response.setStatus(429);
            response.setContentType("application/json");
            response.getWriter().write("{\"error\":\"Rate limit exceeded. Please try again later.\"}");
            return false;
        }
        return true;
    }

    private Bucket buildBucket(int requestsPerHour) {
        return Bucket.builder()
                .addLimit(Bandwidth.builder()
                        .capacity(requestsPerHour)
                        .refillGreedy(requestsPerHour, Duration.ofHours(1))
                        .build())
                .build();
    }

    private String extractUserId() {
        try {
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            if (auth instanceof OAuth2AuthenticationToken token) {
                Object sub = token.getPrincipal().getAttributes().get("sub");
                return sub != null ? sub.toString() : null;
            }
        } catch (Exception ignored) {}
        return null;
    }
}
