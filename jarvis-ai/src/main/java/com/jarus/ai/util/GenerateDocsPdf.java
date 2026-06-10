package com.jarus.ai.util;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.encryption.AccessPermission;
import org.apache.pdfbox.pdmodel.encryption.StandardProtectionPolicy;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;

import java.awt.Color;
import java.io.File;
import java.io.IOException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

/**
 * Generates a password-protected PDF documenting JARUS architecture,
 * features, API reference, and the complete unit-test suite.
 *
 * <h2>Usage (via Gradle)</h2>
 * <pre>
 *   # Default password (Jarus@2024)
 *   gradlew generateDocs
 *
 *   # Custom password
 *   gradlew generateDocs -Ppdf.password=MySecretPass
 * </pre>
 *
 * Output: docs/JARUS_Documentation.pdf
 */
public class GenerateDocsPdf {

    // ── Page / Layout ─────────────────────────────────────────────────────────
    private static final float PW = PDRectangle.A4.getWidth();   // 595.28
    private static final float PH = PDRectangle.A4.getHeight();  // 841.89
    private static final float ML = 56f;
    private static final float MR = 56f;
    private static final float MT = 56f;
    private static final float MB = 50f;
    private static final float CW = PW - ML - MR;               // 483.28

    // ── Colours ───────────────────────────────────────────────────────────────
    private static final Color PRIMARY   = new Color(21, 101, 192);
    private static final Color ACCENT    = new Color(0, 121, 107);
    private static final Color SUCCESS   = new Color(46, 125, 50);
    private static final Color BODY      = new Color(33, 33, 33);
    private static final Color MUTED     = new Color(110, 110, 110);
    private static final Color LIGHT_BG  = new Color(243, 243, 243);
    private static final Color DIVIDER   = new Color(200, 200, 200);
    private static final Color WHITE     = Color.WHITE;

    // ── State ─────────────────────────────────────────────────────────────────
    private final PDDocument doc;
    private PDPage            page;
    private PDPageContentStream cs;
    private float y;
    private int   pageNum = 0;

    // ── Fonts ─────────────────────────────────────────────────────────────────
    private final PDType1Font fBold;
    private final PDType1Font fReg;
    private final PDType1Font fItalic;
    private final PDType1Font fCourier;

    // ════════════════════════════════════════════════════════════════════════════
    //  Entry point
    // ════════════════════════════════════════════════════════════════════════════

    public static void main(String[] args) throws Exception {
        String password = args.length > 0 ? args[0] : "Jarus@2024";
        String outPath  = args.length > 1 ? args[1]
                : "docs" + File.separator + "JARUS_Documentation.pdf";

        new File(outPath).getParentFile().mkdirs();
        GenerateDocsPdf gen = new GenerateDocsPdf();
        gen.generate(outPath, password);
        System.out.println("========================================");
        System.out.println("  PDF saved  : " + new File(outPath).getAbsolutePath());
        System.out.println("  Password   : " + password);
        System.out.println("  Pages      : " + gen.pageNum);
        System.out.println("========================================");
    }

    public GenerateDocsPdf() throws IOException {
        doc      = new PDDocument();
        fBold    = new PDType1Font(Standard14Fonts.FontName.HELVETICA_BOLD);
        fReg     = new PDType1Font(Standard14Fonts.FontName.HELVETICA);
        fItalic  = new PDType1Font(Standard14Fonts.FontName.HELVETICA_OBLIQUE);
        fCourier = new PDType1Font(Standard14Fonts.FontName.COURIER);
    }

    // ════════════════════════════════════════════════════════════════════════════
    //  Document assembly
    // ════════════════════════════════════════════════════════════════════════════

    public void generate(String outPath, String password) throws IOException {
        buildCoverPage();
        buildTableOfContents();
        buildSection1Overview();
        buildSection2TechStack();
        buildSection3Architecture();
        buildSection4Features();
        buildSection5UnitTests();
        buildSection6BuildDeploy();
        buildSection7Workflow();
        closeStream();
        addPageNumbers();
        protect(password);
        doc.save(outPath);
        doc.close();
    }

    // ════════════════════════════════════════════════════════════════════════════
    //  Cover Page
    // ════════════════════════════════════════════════════════════════════════════

    private void buildCoverPage() throws IOException {
        newPage();
        // Dark blue header band
        fillRect(0, PH - 230, PW, 230, PRIMARY);
        fillRect(0, PH - 235, PW, 6, ACCENT);

        // Title
        textCentred("JARUS", fBold, 56, PH - 100, WHITE);
        textCentred("AI Job Hunt Assistant", fReg, 20, PH - 148, WHITE);
        textCentred("Technical Documentation & Testing Guide", fItalic, 12,
                PH - 175, new Color(180, 210, 255));
        textCentred("v1.0  |  Confidential  |  Password Protected", fItalic, 10,
                PH - 200, new Color(140, 180, 240));

        // Meta info card
        float bx = ML, by = PH - 430;
        fillRect(bx, by, CW, 140, LIGHT_BG);
        fillRect(bx, by + 136, CW, 4, ACCENT);

        String date = LocalDate.now().format(DateTimeFormatter.ofPattern("MMMM dd, yyyy"));
        String[][] meta = {
            {"Version",    "1.0.0"},
            {"Date",       date},
            {"Framework",  "Spring Boot 3.5.11 / Java 21"},
            {"Deployment", "Docker on Render (jarvis-ai-assistant-0yby.onrender.com)"},
            {"Repository", "github.com/suraj-suryn/jarvis-ai-assistant"},
        };
        float my = by + 116;
        for (String[] row : meta) {
            text(row[0], fBold, 10, bx + 14, my, MUTED);
            text(row[1], fReg,  10, bx + 110, my, BODY);
            my -= 22;
        }

        // Bottom band
        fillRect(0, 0, PW, 45, new Color(240, 240, 240));
        fillRect(0, 45, PW, 1, DIVIDER);
        textCentred("CONFIDENTIAL — For authorised personnel only", fItalic, 9, 18, MUTED);
        textCentred("Open with password supplied by the project owner", fItalic, 8, 6, MUTED);
    }

    // ════════════════════════════════════════════════════════════════════════════
    //  Table of Contents
    // ════════════════════════════════════════════════════════════════════════════

    private void buildTableOfContents() throws IOException {
        newPage();
        sectionHeader("Table of Contents");
        y -= 8;

        String[][] toc = {
            {"1", "JARUS Overview",             "Capabilities, goals, and user journey"},
            {"2", "Technology Stack",           "Languages, frameworks, and third-party services"},
            {"3", "Application Architecture",   "Frontend, backend, storage, and AI layers"},
            {"4", "Features & API Reference",   "All REST endpoints with request/response detail"},
            {"5", "Unit Test Suite",            "26 tests across 3 classes — full reference"},
            {"6", "Build & Deploy Guide",       "Local build, Docker, and Render deployment"},
            {"7", "Development Workflow",       "Git flow, conventions, and contribution guide"},
        };

        for (int i = 0; i < toc.length; i++) {
            ensureSpace(28);
            Color bg = (i % 2 == 0) ? LIGHT_BG : WHITE;
            fillRect(ML, y - 22, CW, 26, bg);
            fillRect(ML, y - 22, 3, 26, PRIMARY);
            text(toc[i][0] + ".", fBold, 11, ML + 12, y - 14, PRIMARY);
            text(toc[i][1],       fBold, 11, ML + 34,  y - 14, BODY);
            text(toc[i][2],       fReg,  9,  ML + 210, y - 14, MUTED);
            y -= 26;
        }
    }

    // ════════════════════════════════════════════════════════════════════════════
    //  Section 1 — Overview
    // ════════════════════════════════════════════════════════════════════════════

    private void buildSection1Overview() throws IOException {
        newPage();
        sectionTitle("1", "JARUS Overview");

        para("JARUS (Job Automation & Resume Upgrade System) is a full-stack Spring Boot web application "
           + "that helps job seekers streamline their entire job-hunting workflow using Google Gemini AI. "
           + "Users sign in with their Google account, upload a resume, capture job postings, and let AI "
           + "tailor their resume and generate cover letters for each application.");

        subHeader("Core Capabilities");
        bulletList(new String[]{
            "Resume upload (PDF/DOCX), automatic text parsing, and structured storage",
            "Job capture via manual entry (title, company, description)",
            "AI-powered resume tailoring per job description using Gemini 2.0 Flash",
            "Cover letter generation (AI) or manual writing, exported to PDF and DOCX",
            "Job-match score analysis: matched skills, missing skills, recommendation",
            "Company research and interview question generation via AI",
            "Gemini API key management with live validation and colour-coded feedback",
            "Admin panel: view registered users with Google avatars, manage access allowlist",
            "Ambient music player — Lo-Fi Pad, Focus Drone, Rain (Web Audio API, zero external files)",
            "Web push notifications (VAPID) and Progressive Web App (PWA) support",
        });

        subHeader("User Journey");
        String[] steps = {
            "1  Login via Google OAuth2 (Google consent screen)",
            "2  Upload Resume  (PDF or DOCX — parsed to structured text)",
            "3  Capture Jobs   (paste job title, company, and description)",
            "4  Tailor Resume  (AI rewrites relevant sections for the specific job)",
            "5  Download       (tailored resume as PDF or DOCX)",
            "6  Cover Letter   (AI-generate or write manually)",
            "7  Download       (cover letter as PDF or DOCX)",
            "8  Repeat         (track multiple applications per resume)",
        };
        y -= 4;
        for (int i = 0; i < steps.length; i++) {
            ensureSpace(22);
            Color bg = (i % 2 == 0) ? new Color(232, 245, 233) : new Color(227, 242, 253);
            fillRect(ML, y - 16, CW, 20, bg);
            text(steps[i], fReg, 10, ML + 10, y - 11, BODY);
            y -= 20;
        }
    }

    // ════════════════════════════════════════════════════════════════════════════
    //  Section 2 — Tech Stack
    // ════════════════════════════════════════════════════════════════════════════

    private void buildSection2TechStack() throws IOException {
        ensureSpace(120);
        sectionTitle("2", "Technology Stack");

        String[][] rows = {
            {"Category",            "Technology",                    "Version / Notes"},
            {"Language",            "Java",                          "21 — sourceCompatibility, any JDK >= 17 can build"},
            {"Framework",           "Spring Boot",                   "3.5.11"},
            {"Security",            "Spring Security + OAuth2",      "Google OAuth2 (testing mode), session-based auth"},
            {"Persistence",         "Spring Data JPA + PostgreSQL",  "postgresql 42.7.3 driver, Render free DB"},
            {"HTTP Client",         "Spring WebFlux WebClient",      "Reactive, used exclusively for Gemini API calls"},
            {"AI",                  "Google Gemini",                 "gemini-2.0-flash model, user-supplied API key"},
            {"File Storage",        "Cloudinary",                    "cloudinary-http44:1.39.0 — raw uploads, signed URLs"},
            {"PDF Generation",      "Apache PDFBox",                 "3.0.1"},
            {"DOCX Generation",     "Apache POI",                    "5.2.5"},
            {"Encryption",          "JDK AES-256-GCM",               "EncryptionService wraps javax.crypto"},
            {"Rate Limiting",       "Bucket4j",                      "8.10.1 — per-user token bucket"},
            {"Push Notifications",  "web-push (VAPID)",              "nl.martijndwars:web-push:5.1.1"},
            {"Logging",             "Logstash Logback Encoder",      "7.4 — JSON structured logs"},
            {"Build Tool",          "Gradle",                        "9.2.0 (gradlew wrapper — no local install needed)"},
            {"Container",           "Docker (multi-stage)",          "Build: gradle:8-jdk21-alpine | Runtime: eclipse-temurin:21-jre"},
            {"Deployment",          "Render",                        "Free tier, auto-deploy on push to main"},
            {"Testing",             "JUnit 5 + Mockito",             "spring-boot-starter-test, spring-security-test"},
        };
        drawTable(rows, new float[]{110, 160, CW - 278}, ML, true);
    }

    // ════════════════════════════════════════════════════════════════════════════
    //  Section 3 — Architecture
    // ════════════════════════════════════════════════════════════════════════════

    private void buildSection3Architecture() throws IOException {
        newPage();
        sectionTitle("3", "Application Architecture");

        para("JARUS is a monolithic Spring Boot application with a Vanilla JS Single Page Application (SPA) "
           + "frontend. All API calls go to the same origin (no CORS). Authentication is enforced at the "
           + "Spring Security filter chain before any controller logic executes. Every database query is "
           + "scoped to the authenticated user's Google sub ID, preventing cross-user data access.");

        subHeader("Layer Overview");
        String[][] layers = {
            {"Layer",         "Technology",             "Responsibility"},
            {"Presentation",  "Vanilla JS SPA",         "index.html — tabs: Resume, Jobs, Settings, Admin"},
            {"Auth",          "Spring Security OAuth2", "Google login, session, user identity extraction"},
            {"Controller",    "Spring MVC REST",        "Validation, auth, response mapping (6 controllers)"},
            {"Service",       "Spring @Service",        "Business logic: AI, file processing, encryption"},
            {"Repository",    "Spring Data JPA",        "CRUD wrappers for all JPA entities"},
            {"Database",      "PostgreSQL (Render)",    "UserSettings, JobPost, ParsedResume, CoverLetter, PushSubscription"},
            {"File Storage",  "Cloudinary",             "Raw PDF/DOCX uploads; download via signed privateDownload()"},
            {"AI",            "Google Gemini API",      "WebClient -> gemini-2.0-flash; key supplied by user"},
        };
        drawTable(layers, new float[]{90, 130, CW - 228}, ML, true);

        subHeader("Key Services");
        bulletList(new String[]{
            "GeminiService       verifyKey(), chat(), tailorResume(), generateCoverLetter(), researchCompany()",
            "GcsStorageService   upload() / download() [signed URL via privateDownload()] / delete()",
            "ResumeBuilderService  generatePdf(TailoredResume) and generateDocx(TailoredResume) via PDFBox/POI",
            "EncryptionService   AES-256-GCM encrypt/decrypt for Gemini API keys stored in database",
            "ResumeParserService  Extract full text from uploaded PDF (PDFBox) or DOCX (Apache POI)",
        });

        subHeader("Security Model");
        para("All /api/** endpoints require an authenticated Spring Security session. The OAuth2 principal's "
           + "'sub' claim (Google user ID) is the primary user key for all DB queries. Gemini API keys are "
           + "encrypted with AES-256-GCM before storage and decrypted only at call time. Admin endpoints "
           + "additionally require the user's email to be in the app.allowed-emails allowlist.");
    }

    // ════════════════════════════════════════════════════════════════════════════
    //  Section 4 — Features & API Reference
    // ════════════════════════════════════════════════════════════════════════════

    private void buildSection4Features() throws IOException {
        newPage();
        sectionTitle("4", "Features & API Reference");

        // 4.1 Auth
        subHeader("4.1  Authentication");
        String[][] auth = {
            {"Method", "Endpoint",       "Description"},
            {"GET",    "/login",         "Redirect to Google OAuth2 consent screen"},
            {"GET",    "/logout",        "Invalidate session, redirect to /"},
            {"GET",    "/api/user/me",   "Returns {name, email, picture} of logged-in user"},
        };
        drawTable(auth, new float[]{48, 150, CW - 206}, ML, true);

        // 4.2 Resume
        ensureSpace(90);
        subHeader("4.2  Resume Management");
        String[][] resume = {
            {"Method", "Endpoint",                          "Description"},
            {"POST",   "/api/resume/upload",                "Upload PDF/DOCX; parse and store resume text"},
            {"GET",    "/api/resume/list",                  "List resumes for current user (content excluded)"},
            {"GET",    "/api/resume/download/{id}",         "Stream original file (signed Cloudinary URL)"},
            {"DELETE", "/api/resume/{id}",                  "Delete record and Cloudinary files"},
            {"POST",   "/api/resume/tailor",                "AI-tailor resume for a job; generates PDF/DOCX"},
            {"GET",    "/api/resume/download-tailored/{id}","Download tailored PDF or DOCX"},
        };
        drawTable(resume, new float[]{50, 205, CW - 263}, ML, true);

        // 4.3 Jobs
        ensureSpace(90);
        subHeader("4.3  Job Management");
        String[][] jobs = {
            {"Method", "Endpoint",           "Description"},
            {"POST",   "/api/jobs/capture",  "Save a job posting — {title, company, description}"},
            {"GET",    "/api/jobs/list",      "List all jobs for current user"},
            {"DELETE", "/api/jobs/{id}",      "Delete a job posting"},
            {"POST",   "/api/jobs/analyze",  "AI job-match score vs selected resume"},
            {"POST",   "/api/jobs/research", "AI company research and interview question generation"},
        };
        drawTable(jobs, new float[]{50, 175, CW - 233}, ML, true);

        // 4.4 Cover Letter
        newPage();
        subHeader("4.4  Cover Letter");
        para("Supports AI generation (Gemini key required) and manual writing. Every letter is rendered to "
           + "PDF and DOCX via ResumeBuilderService and stored in Cloudinary. The /list endpoint "
           + "deliberately strips the content field to save bandwidth.");
        String[][] cl = {
            {"Method", "Endpoint",                        "Description"},
            {"POST",   "/api/cover-letter/generate",      "AI-generate cover letter. Body: {resumeId, jobId}"},
            {"POST",   "/api/cover-letter/write",         "Manual cover letter. Body: {content, jobId?, resumeId?}"},
            {"GET",    "/api/cover-letter/list",          "List all cover letters (content=null)"},
            {"GET",    "/api/cover-letter/{id}",          "Get single cover letter with full content"},
            {"DELETE", "/api/cover-letter/{id}",          "Delete record + Cloudinary PDF and DOCX"},
            {"GET",    "/api/cover-letter/download/{id}", "Download file. Query: ?format=pdf|docx"},
        };
        drawTable(cl, new float[]{50, 225, CW - 283}, ML, true);

        // 4.5 Settings
        ensureSpace(90);
        subHeader("4.5  Settings — Gemini Key Management");
        para("On save, the key is live-verified against the Gemini API before being AES-256-GCM encrypted "
           + "and stored. The UI shows colour-coded status: green (verified), amber (rate-limited), red (invalid).");
        String[][] settings = {
            {"Method", "Endpoint",                        "Description"},
            {"POST",   "/api/settings/gemini-key",        "Validate and save API key. Returns {status, message}"},
            {"GET",    "/api/settings/gemini-key/status", "Returns {configured: true/false}"},
        };
        drawTable(settings, new float[]{48, 210, CW - 266}, ML, true);

        ensureSpace(80);
        text("Validation responses:", fBold, 10, ML, y, BODY);
        y -= 14;
        String[][] valResults = {
            {"status",       "HTTP", "Meaning"},
            {"VERIFIED",     "200",  "Key works. Saved. Green indicator shown in UI."},
            {"RATE_LIMITED", "200",  "Key valid but quota exhausted. Saved. Amber warning shown."},
            {"INVALID_KEY",  "400",  "Key rejected by Gemini (401/403/400). NOT saved. Red error."},
            {"SAVED",        "200",  "Network error during verify. Saved with caution warning."},
        };
        drawTable(valResults, new float[]{95, 38, CW - 141}, ML, true);

        // 4.6 Admin
        ensureSpace(90);
        subHeader("4.6  Admin Panel");
        para("Only users whose Google email is in the app.allowed-emails property can access admin "
           + "endpoints. Admin can view registered users (Google avatar, display name, email) and "
           + "see the pending allowlist.");
        String[][] admin = {
            {"Method", "Endpoint",         "Description"},
            {"GET",    "/api/admin/users", "Returns {users:[{email,displayName,picture}], allowedEmails:[...]}"},
        };
        drawTable(admin, new float[]{48, 150, CW - 206}, ML, true);

        // 4.7 Music Player
        ensureSpace(70);
        subHeader("4.7  Ambient Music Player");
        para("Runs entirely in the browser via Web Audio API — zero CDN or external files. Auto-starts "
           + "during long AI operations (tailor, generate) and stops on completion. Preferences "
           + "(track, mute state) persist in localStorage key jarus_music_prefs.");
        bulletList(new String[]{
            "Lo-Fi Pad    220 Hz sine + LFO tremolo at 0.25 Hz + 5th harmonic + lowpass filter",
            "Focus Drone  Binaural beat: 432 Hz + 436 Hz oscillators (4 Hz beat) + stereo panning",
            "Rain         White-noise buffer + 650 Hz lowpass + LFO filter sweep at 0.08 Hz",
        });
    }

    // ════════════════════════════════════════════════════════════════════════════
    //  Section 5 — Unit Test Suite
    // ════════════════════════════════════════════════════════════════════════════

    private void buildSection5UnitTests() throws IOException {
        newPage();
        sectionTitle("5", "Unit Test Suite");

        para("JARUS uses Spring Boot's test-slicing strategy. Controller tests load only the web layer "
           + "(@WebMvcTest) — no database, no Cloudinary, no real Gemini calls. Service tests use "
           + "Mockito to isolate the class under test. Every test passes without any external "
           + "infrastructure or environment variables.");

        // Summary stat boxes
        ensureSpace(65);
        float bx = ML, bw = (CW - 10) / 3f;
        float statY = y - 55;
        drawStatBox(bx,           statY, bw - 4, "26",   "Total Tests",     SUCCESS);
        drawStatBox(bx + bw,      statY, bw - 4, "0",    "Failures",        PRIMARY);
        drawStatBox(bx + bw * 2,  statY, bw - 4, "1.3s", "Execution Time",  ACCENT);
        y = statY - 12;

        // 5.1 How to Run
        subHeader("5.1  How to Run");
        codeBlock(new String[]{
            "# Run all 26 tests (no JAVA_HOME required — uses PATH JDK)",
            "./gradlew test",
            "",
            "# Run a specific test class",
            "./gradlew test --tests \"com.jarus.ai.service.GeminiServiceTest\"",
            "./gradlew test --tests \"com.jarus.ai.controller.SettingsControllerTest\"",
            "./gradlew test --tests \"com.jarus.ai.controller.CoverLetterControllerTest\"",
            "",
            "# View HTML report after run",
            "build/reports/tests/test/index.html",
        });

        // 5.2 GeminiServiceTest
        newPage();
        subHeader("5.2  GeminiServiceTest  (6 tests)");
        para("Tests GeminiService.verifyKey(String apiKey). Strategy: @ExtendWith(MockitoExtension.class) "
           + "with a fully mocked WebClient chain (post -> uri -> header -> bodyValue -> retrieve -> "
           + "bodyToMono) and a @Spy ObjectMapper (real implementation needed for JSON node building "
           + "inside callGemini).");
        String[][] geminiTests = {
            {"Test Method",                         "Stub",                   "Expected"},
            {"verifyKey_returnsVerified",            "Mono.just(OK_JSON)",     "\"VERIFIED\""},
            {"verifyKey_returnsRateLimited",         "Mono.error(429)",        "\"RATE_LIMITED\""},
            {"verifyKey_returnsInvalidKey_on401",    "Mono.error(401)",        "\"INVALID_KEY\""},
            {"verifyKey_returnsInvalidKey_on403",    "Mono.error(403)",        "\"INVALID_KEY\""},
            {"verifyKey_returns400AsInvalidKey",     "Mono.error(400)",        "\"INVALID_KEY\""},
            {"verifyKey_returnsNetworkError",        "Mono.error(RuntimeEx.)", "\"NETWORK_ERROR\""},
        };
        drawTable(geminiTests, new float[]{175, 148, CW - 331}, ML, true);

        // 5.3 SettingsControllerTest
        ensureSpace(100);
        subHeader("5.3  SettingsControllerTest  (8 tests)");
        para("@WebMvcTest(SettingsController.class). Verifies POST /api/settings/gemini-key and "
           + "GET /api/settings/gemini-key/status. Authentication simulated via oauth2Login() post-processor.");
        String[][] settingsTests = {
            {"Test Method",                                "Scenario",                          "Expected"},
            {"saveGeminiKey_rejectsBlankKey",              "apiKey = \"\"",                     "400, never saved"},
            {"saveGeminiKey_rejectsMissingKey",            "body: {}",                          "400, never saved"},
            {"saveGeminiKey_invalidKey_returns400...",     "verifyKey -> INVALID_KEY",          "400 + {status:INVALID_KEY}"},
            {"saveGeminiKey_validKey_saves_returnsVerified","verifyKey -> VERIFIED",            "200 + {status:VERIFIED}, saved x1"},
            {"saveGeminiKey_rateLimited_savesKey...",      "verifyKey -> RATE_LIMITED",         "200 + {status:RATE_LIMITED}, saved x1"},
            {"saveGeminiKey_networkError_savesKey...",     "verifyKey -> NETWORK_ERROR",        "200 + {status:SAVED}, saved x1"},
            {"geminiKeyStatus_returnsNotConfigured...",    "settings.encryptedKey = null",      "200 + {configured:false}"},
            {"geminiKeyStatus_returnsConfigured...",       "settings.encryptedKey = present",   "200 + {configured:true}"},
        };
        drawTable(settingsTests, new float[]{170, 152, CW - 330}, ML, true);

        // 5.4 CoverLetterControllerTest
        newPage();
        subHeader("5.4  CoverLetterControllerTest  (12 tests)");
        para("@WebMvcTest(CoverLetterController.class). Covers all 5 cover letter endpoints. "
           + "Note: gcsService.upload() returns String (not void), so use when(...).thenReturn(...) "
           + "not doNothing(). The @Spy ObjectMapper pattern is NOT needed here since the "
           + "controller does not build JSON internally.");
        String[][] clTests = {
            {"Test Method",                             "Endpoint",        "Expected"},
            {"write_validContent_returns200AndSaves",   "POST /write",     "200, PDF+DOCX generated, saved once"},
            {"write_blankContent_returns400",           "POST /write",     "400, nothing saved"},
            {"write_missingContent_returns400",         "POST /write",     "400, nothing saved"},
            {"getOne_existingId_returnsFullContent",    "GET /{id}",       "200, content field present"},
            {"getOne_notFound_returns404",              "GET /{id}",       "404"},
            {"delete_existingId_returns204AndCleansUp", "DELETE /{id}",    "204, gcsService.delete x2"},
            {"delete_notFound_returns404",              "DELETE /{id}",    "404, delete never called"},
            {"list_returnsLettersWithoutContent",       "GET /list",       "200, content absent in each item"},
            {"list_empty_returnsEmptyArray",            "GET /list",       "200, array length 0"},
            {"generate_noGeminiKey_returns402",         "POST /generate",  "402 Payment Required"},
            {"generate_resumeNotFound_returns404",      "POST /generate",  "404"},
            {"generate_missingIds_returns400",          "POST /generate",  "400"},
        };
        drawTable(clTests, new float[]{185, 104, CW - 297}, ML, true);

        // 5.5 Step-by-step guide
        ensureSpace(100);
        subHeader("5.5  Step-by-Step Guide for Adding New Tests");
        String[] guide = {
            "Choose test type: @WebMvcTest (controller) or @ExtendWith(MockitoExtension) (service)",
            "Declare @MockBean for every @Autowired dependency in the controller under test",
            "Always include @MockBean ClientRegistrationRepository (Spring OAuth2 requirement)",
            "Call mockOAuth2User() = oauth2Login().attributes(a -> a.put(\"sub\", USER_SUB))",
            "Stub dependencies: when(mock.method(any())).thenReturn(value)",
            "Perform request: mockMvc.perform(post(\"/path\").with(mockUser).with(csrf())...)",
            "Assert status: .andExpect(status().isOk())  or  .andExpect(status().isBadRequest())",
            "Assert JSON:   .andExpect(jsonPath(\"$.field\").value(expected))",
            "Verify side effects: verify(repo, times(1)).save(eq(USER_SUB), any())",
        };
        for (int i = 0; i < guide.length; i++) {
            ensureSpace(22);
            fillRect(ML,       y - 16, 20, 20, (i % 2 == 0) ? PRIMARY : ACCENT);
            fillRect(ML + 20,  y - 16, CW - 20, 20, (i % 2 == 0) ? new Color(232, 240, 254) : new Color(224, 247, 244));
            text(String.valueOf(i + 1), fBold, 9, ML + 6, y - 12, WHITE);
            text(guide[i], fReg, 9.5f, ML + 28, y - 12, BODY);
            y -= 20;
        }
    }

    // ════════════════════════════════════════════════════════════════════════════
    //  Section 6 — Build & Deploy
    // ════════════════════════════════════════════════════════════════════════════

    private void buildSection6BuildDeploy() throws IOException {
        newPage();
        sectionTitle("6", "Build & Deploy Guide");

        subHeader("6.1  Local Development");
        para("No specific JDK version required. build.gradle uses sourceCompatibility = VERSION_21, "
           + "so any JDK 17+ on PATH will work.");
        codeBlock(new String[]{
            "# Prerequisites: any JDK >= 17, PostgreSQL",
            "git clone https://github.com/suraj-suryn/jarvis-ai-assistant.git",
            "cd jarvis-ai-assistant/jarvis-ai",
            "",
            "# Set credentials in src/main/resources/application.properties",
            "# (or pass as environment variables)",
            "",
            "# Run unit tests (26 tests, no external services needed)",
            "./gradlew test",
            "",
            "# Start the application",
            "./gradlew bootRun",
            "",
            "# Package as JAR",
            "./gradlew build -x test",
        });

        subHeader("6.2  Required Configuration");
        String[][] config = {
            {"Property",                                              "Description"},
            {"spring.datasource.url",                                "jdbc:postgresql://host:5432/db"},
            {"spring.security.oauth2.client.registration.google.client-id", "Google OAuth2 client ID"},
            {"spring.security.oauth2.client.registration.google.client-secret","Google OAuth2 client secret"},
            {"cloudinary.url",                                        "cloudinary://api_key:secret@cloud_name"},
            {"app.encryption.key",                                    "Base64 AES-256 key for Gemini key storage"},
            {"app.vapid.public-key / app.vapid.private-key",         "VAPID keys for web push notifications"},
            {"app.allowed-emails",                                    "Comma-separated admin email allowlist"},
        };
        drawTable(config, new float[]{230, CW - 238}, ML, true);

        subHeader("6.3  Docker Build");
        codeBlock(new String[]{
            "# Multi-stage Dockerfile (in jarvis-ai/)",
            "# Stage 1: gradle:8-jdk21-alpine  — compile + package (RUN gradle build -x test)",
            "# Stage 2: eclipse-temurin:21-jre-alpine — minimal runtime",
            "",
            "docker build -t jarus-ai .",
            "docker run -p 8080:8080 \\",
            "  -e SPRING_DATASOURCE_URL=jdbc:postgresql://host:5432/jarus \\",
            "  -e SPRING_DATASOURCE_USERNAME=user \\",
            "  -e SPRING_DATASOURCE_PASSWORD=pass \\",
            "  jarus-ai",
        });

        subHeader("6.4  Render Deployment");
        bulletList(new String[]{
            "Service URL:  https://jarvis-ai-assistant-0yby.onrender.com",
            "Auto-deploy:  every push to the main branch triggers a Render build",
            "Build:        Render runs docker build using the Dockerfile in jarvis-ai/",
            "Database:     Render PostgreSQL free tier (expires after 90 days inactivity)",
            "Environment:  all secrets set in the Render dashboard (never committed to git)",
            "Free tier note: service sleeps after 15 min; first request after sleep takes ~30s",
        });
    }

    // ════════════════════════════════════════════════════════════════════════════
    //  Section 7 — Development Workflow
    // ════════════════════════════════════════════════════════════════════════════

    private void buildSection7Workflow() throws IOException {
        newPage();
        sectionTitle("7", "Development Workflow");

        subHeader("7.1  Git Flow");
        bulletList(new String[]{
            "Main branch: main — always deployable; push triggers Render auto-deploy",
            "Commit convention: fix: / feat: / build: / test:  (Conventional Commits style)",
            "Key commits: 55d123b (4 features), a066967 (Cloudinary fix + 3 test files), f1e7c71 (build portability)",
        });

        subHeader("7.2  Project Structure");
        codeBlock(new String[]{
            "jarvis-ai/",
            "  Dockerfile                  # Multi-stage: gradle:8-jdk21 build, temurin:21-jre runtime",
            "  build.gradle               # sourceCompatibility = VERSION_21, generateDocs task",
            "  src/main/java/com/jarus/ai/",
            "    JarvisAiApplication.java  # @SpringBootApplication entry point",
            "    controller/               # CoverLetterController, SettingsController, ...",
            "    service/                  # GeminiService, GcsStorageService, ResumeBuilderService, ...",
            "    repository/               # JPA repositories (CoverLetterRepository, ...)",
            "    model/                    # Entities: UserSettings, JobPost, CoverLetter, ...",
            "    security/                 # EncryptionService, OAuth2 config",
            "    util/                     # GenerateDocsPdf  (this doc generator)",
            "  src/main/resources/",
            "    application.properties    # Spring config (secrets via env vars in production)",
            "    static/                   # SPA frontend: index.html, js/, css/, sw.js",
            "  src/test/java/com/jarus/ai/",
            "    controller/               # SettingsControllerTest, CoverLetterControllerTest",
            "    service/                  # GeminiServiceTest",
            "  docs/                       # Generated PDFs (committed to git)",
        });

        subHeader("7.3  Key Implementation Notes");
        bulletList(new String[]{
            "Gemini API key: AES-256-GCM encrypted before storage; never stored in plaintext",
            "Cloudinary download: use privateDownload() always — unsigned URLs return 401",
            "Test slice: prefer @WebMvcTest over @SpringBootTest for controller tests (no DB needed)",
            "OAuth2 in tests: oauth2Login().attributes(a -> a.put(\"sub\", USER_SUB)) post-processor",
            "WebClient mocking: requires @Spy ObjectMapper alongside mocked WebClient chain",
            "Build portability: sourceCompatibility avoids toolchain JDK lookup — any JDK >= 17 works",
            "Rate limiting: Bucket4j per-user token bucket — 429 from Gemini means quota exhausted",
        });

        subHeader("7.4  Regenerating This Document");
        codeBlock(new String[]{
            "# Default password (Jarus@2024)",
            "./gradlew generateDocs",
            "",
            "# Custom password",
            "./gradlew generateDocs -Ppdf.password=YourPassword",
            "",
            "# Output: docs/JARUS_Documentation.pdf",
            "# Commit to git after regeneration:",
            "git add docs/JARUS_Documentation.pdf",
            "git commit -m \"docs: regenerate JARUS documentation PDF\"",
        });
    }

    // ════════════════════════════════════════════════════════════════════════════
    //  Rendering helpers
    // ════════════════════════════════════════════════════════════════════════════

    private void sectionTitle(String num, String title) throws IOException {
        ensureSpace(52);
        fillRect(ML, y - 40, CW, 44, PRIMARY);
        fillRect(ML, y - 40, 4, 44, ACCENT);
        text("Section " + num, fReg, 9, ML + 14, y - 13, new Color(180, 210, 255));
        text(title,             fBold, 18, ML + 14, y - 32, WHITE);
        y -= 52;
    }

    private void sectionHeader(String title) throws IOException {
        ensureSpace(52);
        fillRect(ML, y - 40, CW, 44, PRIMARY);
        fillRect(ML, y - 40, 4, 44, ACCENT);
        text(title, fBold, 18, ML + 14, y - 32, WHITE);
        y -= 52;
    }

    private void subHeader(String title) throws IOException {
        ensureSpace(38);
        y -= 12;
        fillRect(ML, y - 2, 4, 20, ACCENT);
        text(title, fBold, 12, ML + 12, y + 12, ACCENT);
        y -= 18;
        hLine(ML + 4, y, CW - 4, DIVIDER);
        y -= 8;
    }

    private void para(String txt) throws IOException {
        y -= 4;
        y = paragraph(txt, fReg, 10, ML, y, CW, BODY);
        y -= 6;
    }

    private void bulletList(String[] items) throws IOException {
        y -= 4;
        for (String item : items) {
            ensureSpace(20);
            text("\u2022", fBold, 11, ML + 6, y, PRIMARY);
            y = paragraph(item, fReg, 10, ML + 20, y, CW - 20, BODY);
            y -= 3;
        }
        y -= 4;
    }

    private void codeBlock(String[] lines) throws IOException {
        float blockH = lines.length * 14f + 18f;
        ensureSpace(blockH + 8);
        float bx = ML, bw = CW;
        float by = y - blockH;
        fillRect(bx, by, bw, blockH, new Color(28, 28, 28));
        fillRect(bx, by + blockH - 4, bw, 4, ACCENT);
        float cy = y - 8;
        for (String line : lines) {
            if (line.startsWith("#")) {
                text(sanitize(line), fItalic, 8.5f, bx + 10, cy, new Color(144, 200, 144));
            } else if (!line.isEmpty()) {
                text(sanitize(line), fCourier, 8.5f, bx + 10, cy, new Color(218, 218, 218));
            }
            cy -= 14;
        }
        y = by - 8;
    }

    private void drawStatBox(float x, float boxy, float w, String value, String label, Color color) throws IOException {
        fillRect(x, boxy, w, 52, color);
        float vw = textWidth(value, fBold, 22);
        text(value, fBold, 22, x + (w - vw) / 2f, boxy + 34, WHITE);
        float lw = textWidth(label, fReg, 9);
        text(label, fReg, 9, x + (w - lw) / 2f, boxy + 14, WHITE);
    }

    private void drawTable(String[][] rows, float[] colWidths, float x, boolean hasHeader) throws IOException {
        float rowH = 20f;
        float totalW = 0;
        for (float w : colWidths) totalW += w;
        ensureSpace(rows.length * rowH + 12);
        y -= 6;

        for (int r = 0; r < rows.length; r++) {
            float rowTop = y;
            float rowY   = rowTop - rowH;

            if (r == 0 && hasHeader) {
                fillRect(x, rowY, totalW, rowH, PRIMARY);
            } else {
                fillRect(x, rowY, totalW, rowH, r % 2 == 0 ? LIGHT_BG : WHITE);
            }
            cs.setStrokingColor(DIVIDER);
            cs.setLineWidth(0.4f);
            cs.addRect(x, rowY, totalW, rowH);
            cs.stroke();

            float cx = x;
            for (int c = 0; c < rows[r].length && c < colWidths.length; c++) {
                Color  tc   = (r == 0 && hasHeader) ? WHITE : BODY;
                PDType1Font tf = (r == 0 && hasHeader) ? fBold : fReg;
                float  ts   = (r == 0 && hasHeader) ? 9f : 9f;
                String cell = truncateToFit(rows[r][c], tf, ts, colWidths[c] - 7);
                text(cell, tf, ts, cx + 5, rowY + 6, tc);
                cx += colWidths[c];
            }
            y = rowY;
        }
        y -= 8;
    }

    private void hLine(float x, float lineY, float w, Color c) throws IOException {
        cs.setStrokingColor(c);
        cs.setLineWidth(0.5f);
        cs.moveTo(x, lineY);
        cs.lineTo(x + w, lineY);
        cs.stroke();
    }

    private void text(String s, PDType1Font font, float size, float x, float ty, Color color)
            throws IOException {
        cs.beginText();
        cs.setFont(font, size);
        cs.setNonStrokingColor(color);
        cs.newLineAtOffset(x, ty);
        cs.showText(sanitize(s));
        cs.endText();
    }

    private void textCentred(String s, PDType1Font font, float size, float ty, Color color)
            throws IOException {
        float w = textWidth(s, font, size);
        text(s, font, size, (PW - w) / 2f, ty, color);
    }

    private float paragraph(String txt, PDType1Font font, float size, float x, float startY,
                             float maxW, Color color) throws IOException {
        float lh   = size * 1.5f;
        float curY = startY;
        for (String line : wrap(txt, font, size, maxW)) {
            text(line, font, size, x, curY, color);
            curY -= lh;
            if (curY < MB + 20) {
                closeStream();
                newPage();
                curY = y;
            }
        }
        return curY;
    }

    private List<String> wrap(String txt, PDType1Font font, float size, float maxW) throws IOException {
        List<String> lines = new ArrayList<>();
        if (txt == null || txt.isEmpty()) return lines;
        String[] words = txt.split(" ");
        StringBuilder cur = new StringBuilder();
        for (String word : words) {
            String candidate = cur.length() == 0 ? word : cur + " " + word;
            float  w;
            try { w = font.getStringWidth(sanitize(candidate)) / 1000f * size; }
            catch (Exception e) { w = candidate.length() * size * 0.55f; }
            if (w > maxW && cur.length() > 0) {
                lines.add(cur.toString());
                cur = new StringBuilder(word);
            } else {
                cur = new StringBuilder(candidate);
            }
        }
        if (cur.length() > 0) lines.add(cur.toString());
        return lines;
    }

    private float textWidth(String s, PDType1Font font, float size) {
        try { return font.getStringWidth(sanitize(s)) / 1000f * size; }
        catch (Exception e) { return s.length() * size * 0.55f; }
    }

    private String truncateToFit(String s, PDType1Font font, float size, float maxW) {
        if (s == null) return "";
        String t = s;
        try {
            while (!t.isEmpty() && font.getStringWidth(sanitize(t)) / 1000f * size > maxW) {
                t = t.substring(0, t.length() - 1);
            }
        } catch (IOException ignored) {}
        return t.equals(s) ? s : t + "..";
    }

    private String sanitize(String s) {
        if (s == null) return "";
        StringBuilder sb = new StringBuilder(s.length());
        for (char c : s.toCharArray()) {
            if      (c < 32)         sb.append(' ');
            else if (c > 126 && c < 160) sb.append(' ');
            else if (c > 255)        sb.append('?');
            else                     sb.append(c);
        }
        return sb.toString();
    }

    private void fillRect(float x, float ry, float w, float h, Color color) throws IOException {
        cs.setNonStrokingColor(color);
        cs.addRect(x, ry, w, h);
        cs.fill();
    }

    private void ensureSpace(float needed) throws IOException {
        if (y - needed < MB + 20) {
            closeStream();
            newPage();
        }
    }

    private void newPage() throws IOException {
        if (cs != null) closeStream();
        page = new PDPage(PDRectangle.A4);
        doc.addPage(page);
        cs = new PDPageContentStream(doc, page);
        pageNum++;
        y = PH - MT;
    }

    private void closeStream() throws IOException {
        if (cs != null) { cs.close(); cs = null; }
    }

    // ── Page numbers (skip cover) ────────────────────────────────────────────

    private void addPageNumbers() throws IOException {
        int total = doc.getNumberOfPages() - 1; // exclude cover
        for (int i = 1; i < doc.getNumberOfPages(); i++) {
            PDPage p = doc.getPage(i);
            try (PDPageContentStream s = new PDPageContentStream(
                    doc, p, PDPageContentStream.AppendMode.APPEND, true)) {
                // footer divider
                s.setStrokingColor(DIVIDER);
                s.setLineWidth(0.4f);
                s.moveTo(ML, MB - 8);
                s.lineTo(PW - MR, MB - 8);
                s.stroke();
                // page label centred
                String label = "JARUS Technical Documentation  |  Page " + i + " of " + total;
                float lw = textWidth(label, fReg, 7.5f);
                s.beginText();
                s.setFont(fReg, 7.5f);
                s.setNonStrokingColor(MUTED);
                s.newLineAtOffset((PW - lw) / 2f, MB - 20);
                s.showText(label);
                s.endText();
            }
        }
    }

    // ── Encryption ───────────────────────────────────────────────────────────

    private void protect(String password) throws IOException {
        AccessPermission ap = new AccessPermission();
        ap.setCanPrint(true);
        ap.setCanExtractContent(false);
        ap.setCanModify(false);
        ap.setCanFillInForm(false);
        ap.setCanModifyAnnotations(false);
        StandardProtectionPolicy policy =
                new StandardProtectionPolicy(password, password, ap);
        policy.setEncryptionKeyLength(128);
        doc.protect(policy);
    }
}
