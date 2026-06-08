package com.jarus.ai.filter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.MDC;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
public class LoggingFilter extends OncePerRequestFilter {

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        String userId = null;
        try {
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            if (auth instanceof OAuth2AuthenticationToken token) {
                Object sub = token.getPrincipal().getAttributes().get("sub");
                if (sub != null) userId = sub.toString();
            }
            if (userId != null) MDC.put("userId", userId);
            MDC.put("requestUri", request.getRequestURI());
            filterChain.doFilter(request, response);
        } finally {
            MDC.remove("userId");
            MDC.remove("requestUri");
        }
    }
}
