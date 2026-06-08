package com.jarus.ai.service;

import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.json.jackson2.JacksonFactory;
import com.google.api.services.gmail.Gmail;
import com.google.api.services.gmail.model.*;
import com.google.auth.http.HttpCredentialsAdapter;
import com.google.auth.oauth2.AccessToken;
import com.google.auth.oauth2.GoogleCredentials;
import com.jarus.ai.model.GmailMessage;
import com.jarus.ai.model.JobPost;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClient;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientService;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.util.*;
import java.util.regex.Pattern;

@Service
public class GmailService {

    private final OAuth2AuthorizedClientService authorizedClientService;

    private static final String RECRUITER_PATTERN = "(?i)(recruiter|hiring|opportunity|reach out|job opening)";
    private static final String APPLIED_PATTERN = "(?i)(application received|applied|thank you for applying|we received your)";
    private static final String INTERVIEW_PATTERN = "(?i)(interview|schedule a call|phone screen|technical round|coding test)";
    private static final String REJECTION_PATTERN = "(?i)(not moving forward|unfortunately|other candidates|position has been filled|not a fit)";

    public GmailService(OAuth2AuthorizedClientService authorizedClientService) {
        this.authorizedClientService = authorizedClientService;
    }

    public List<GmailMessage> fetchJobEmails(Authentication authentication) throws IOException, GeneralSecurityException {
        Gmail gmail = buildGmailClient(authentication);
        String userId = "me";
        String query = "subject:(job OR interview OR opportunity OR recruiter OR hiring OR application)";
        ListMessagesResponse listResponse = gmail.users().messages()
                .list(userId)
                .setQ(query)
                .setMaxResults(50L)
                .execute();

        List<GmailMessage> messages = new ArrayList<>();
        if (listResponse.getMessages() == null) return messages;

        for (Message msgRef : listResponse.getMessages()) {
            try {
                Message fullMsg = gmail.users().messages()
                        .get(userId, msgRef.getId())
                        .setFormat("full")
                        .execute();
                messages.add(convertMessage(fullMsg));
            } catch (Exception e) {
                // Skip failed messages
            }
        }
        return messages;
    }

    public List<GmailMessage> getThread(Authentication authentication, String threadId) throws IOException, GeneralSecurityException {
        Gmail gmail = buildGmailClient(authentication);
        Thread thread = gmail.users().threads().get("me", threadId).setFormat("full").execute();
        List<GmailMessage> messages = new ArrayList<>();
        if (thread.getMessages() == null) return messages;
        for (Message msg : thread.getMessages()) {
            messages.add(convertMessage(msg));
        }
        return messages;
    }

    public void sendDailyDigest(Authentication authentication, List<JobPost> newJobs) throws IOException, GeneralSecurityException {
        if (newJobs == null || newJobs.isEmpty()) return;
        Gmail gmail = buildGmailClient(authentication);
        String userEmail = getUserEmail(authentication);

        StringBuilder body = new StringBuilder("JARUS Daily Job Digest\n\n");
        body.append(newJobs.size()).append(" new jobs found today:\n\n");
        for (JobPost job : newJobs) {
            body.append("• ").append(job.getTitle()).append(" at ").append(job.getCompany()).append("\n");
            body.append("  ").append(job.getUrl()).append("\n\n");
        }

        String rawEmail = "To: " + userEmail + "\r\n"
                + "Subject: JARUS: " + newJobs.size() + " new jobs found today\r\n"
                + "Content-Type: text/plain; charset=UTF-8\r\n\r\n"
                + body;

        Message message = new Message();
        message.setRaw(Base64.getUrlEncoder().encodeToString(rawEmail.getBytes()));
        gmail.users().messages().send("me", message).execute();
    }

    private GmailMessage convertMessage(Message msg) {
        GmailMessage gm = new GmailMessage();
        gm.setId(msg.getId());
        gm.setThreadId(msg.getThreadId());
        gm.setSnippet(msg.getSnippet());
        gm.setReceivedAt(msg.getInternalDate() != null ? msg.getInternalDate() : 0L);

        if (msg.getPayload() != null && msg.getPayload().getHeaders() != null) {
            for (MessagePartHeader header : msg.getPayload().getHeaders()) {
                if ("From".equalsIgnoreCase(header.getName())) gm.setFrom(header.getValue());
                if ("Subject".equalsIgnoreCase(header.getName())) gm.setSubject(header.getValue());
            }
        }

        // Extract body
        gm.setBody(extractBody(msg.getPayload()));

        // Auto-tag
        gm.setTag(detectTag(gm.getSubject(), gm.getSnippet()));
        return gm;
    }

    private String extractBody(MessagePart part) {
        if (part == null) return "";
        if (part.getBody() != null && part.getBody().getData() != null) {
            return new String(Base64.getUrlDecoder().decode(part.getBody().getData()));
        }
        if (part.getParts() != null) {
            for (MessagePart subPart : part.getParts()) {
                String body = extractBody(subPart);
                if (!body.isEmpty()) return body;
            }
        }
        return "";
    }

    private String detectTag(String subject, String snippet) {
        String text = (subject != null ? subject : "") + " " + (snippet != null ? snippet : "");
        if (Pattern.compile(INTERVIEW_PATTERN).matcher(text).find()) return "INTERVIEW";
        if (Pattern.compile(REJECTION_PATTERN).matcher(text).find()) return "REJECTION";
        if (Pattern.compile(APPLIED_PATTERN).matcher(text).find()) return "APPLIED";
        if (Pattern.compile(RECRUITER_PATTERN).matcher(text).find()) return "RECRUITER";
        return "OTHER";
    }

    private Gmail buildGmailClient(Authentication authentication) throws IOException, GeneralSecurityException {
        OAuth2AuthenticationToken oauthToken = (OAuth2AuthenticationToken) authentication;
        OAuth2AuthorizedClient client = authorizedClientService.loadAuthorizedClient(
                oauthToken.getAuthorizedClientRegistrationId(), oauthToken.getName());

        String accessToken = client.getAccessToken().getTokenValue();
        GoogleCredentials credentials = GoogleCredentials.create(new AccessToken(accessToken, null));

        return new Gmail.Builder(
                GoogleNetHttpTransport.newTrustedTransport(),
                JacksonFactory.getDefaultInstance(),
                new HttpCredentialsAdapter(credentials))
                .setApplicationName("JARUS")
                .build();
    }

    private String getUserEmail(Authentication authentication) {
        OAuth2AuthenticationToken token = (OAuth2AuthenticationToken) authentication;
        Map<String, Object> attrs = token.getPrincipal().getAttributes();
        return (String) attrs.get("email");
    }
}
