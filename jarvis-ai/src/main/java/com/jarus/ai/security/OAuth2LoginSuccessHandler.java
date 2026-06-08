package com.jarus.ai.security;

import com.jarus.ai.model.UserProfile;
import com.jarus.ai.repository.AdminConfigRepository;
import com.jarus.ai.repository.UserRepository;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.core.user.DefaultOAuth2User;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.List;
import java.util.Map;

@Component
public class OAuth2LoginSuccessHandler extends SimpleUrlAuthenticationSuccessHandler {

    @Autowired
    private AdminConfigRepository adminConfigRepository;

    @Autowired
    private UserRepository userRepository;

    @Value("${admin.email}")
    private String adminEmail;

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response,
                                        Authentication authentication) throws IOException {
        OAuth2AuthenticationToken token = (OAuth2AuthenticationToken) authentication;
        Map<String, Object> attributes = token.getPrincipal().getAttributes();
        String email = (String) attributes.get("email");
        String userId = (String) attributes.get("sub");
        String name = (String) attributes.get("name");
        String picture = (String) attributes.get("picture");

        // Check whitelist
        boolean isAdmin = email != null && email.equals(adminEmail);
        List<String> allowedEmails = adminConfigRepository.getAllowedEmails();
        if (!isAdmin && (allowedEmails == null || !allowedEmails.contains(email))) {
            getRedirectStrategy().sendRedirect(request, response, "/access-denied.html");
            return;
        }

        // Upsert user profile in Firestore
        try {
            UserProfile existing = userRepository.getProfile(userId);
            UserProfile profile = existing != null ? existing : new UserProfile();
            profile.setUserId(userId);
            profile.setEmail(email);
            profile.setDisplayName(name);
            profile.setPictureUrl(picture);
            profile.setLastLoginAt(System.currentTimeMillis());
            if (profile.getCreatedAt() == 0) profile.setCreatedAt(System.currentTimeMillis());
            userRepository.saveProfile(userId, profile);
        } catch (Exception e) {
            // Log but don't fail login
        }

        // Elevate to ROLE_ADMIN if admin email
        if (isAdmin) {
            var authorities = List.of(
                    new SimpleGrantedAuthority("ROLE_USER"),
                    new SimpleGrantedAuthority("ROLE_ADMIN"));
            var newPrincipal = new DefaultOAuth2User(authorities, attributes, "sub");
            var newToken = new OAuth2AuthenticationToken(newPrincipal, authorities,
                    token.getAuthorizedClientRegistrationId());
            SecurityContextHolder.getContext().setAuthentication(newToken);
        }

        getRedirectStrategy().sendRedirect(request, response, "/");
    }
}
