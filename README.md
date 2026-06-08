# JARUS — AI Job Hunt Assistant

> **JARUS** (an anagram of *Suraj*) is a full-stack, AI-powered job hunt assistant built on Spring Boot and Google Gemini. It helps you capture jobs from any site, tailor your resume with AI, track your pipeline, research companies, and read job emails — all from a single dark HUD interface.

---

## Features

| Feature | Description |
|---|---|
| **AI Resume Tailoring** | Paste a job — Gemini rewrites only the relevant resume sections and shows a side-by-side diff |
| **Job Capture Bookmarklet** | One-click capture from LinkedIn, Indeed, Naukri, Glassdoor, or any job site |
| **Match Score** | AI scores how well your resume matches each job (0–100%) |
| **Cover Letter Generator** | Generates a tailored cover letter; downloadable as PDF or DOCX |
| **Kanban Pipeline** | Drag-and-drop board: New → Saved → Applied → Interview → Offer |
| **Company Research** | Wikipedia + Google CSE + Gemini interview prep (rounds, questions, tips) |
| **Gmail Integration** | Reads job emails, auto-tags as Recruiter / Applied / Interview / Rejection |
| **Morning Job Scan** | Daily 8 AM cron scans Indeed RSS, RemoteOK, Arbeitnow and sends push notifications |
| **Push Notifications** | PWA Web Push so you get alerts on your phone |
| **Multi-user + Whitelisted** | Only approved emails can log in; managed via admin panel |
| **Encrypted Gemini Key** | Your API key is AES-256/GCM encrypted per user in Firestore |
| **Admin Panel** | Add/remove allowed users without redeploying |
| **Data Export / Delete** | Full account deletion wipes all Firestore + GCS data |

---

## Tech Stack

| Layer | Technology |
|---|---|
| **Backend** | Java 17, Spring Boot 3.5, Gradle |
| **AI** | Google Gemini 1.5 Flash (via WebClient) |
| **Auth** | Google OAuth2 (Spring Security) with Gmail scopes |
| **Database** | Google Cloud Firestore |
| **File Storage** | Google Cloud Storage (GCS) |
| **Email** | Gmail API (read, send digest) |
| **Resume Parsing** | Apache PDFBox 3.0 (PDF), Apache POI 5.2 (DOCX) |
| **Job Feeds** | Rome RSS (Indeed), RemoteOK API, Arbeitnow API |
| **Push Notifications** | VAPID Web Push + BouncyCastle |
| **Rate Limiting** | Bucket4j (per-user in-memory) |
| **Hosting** | Google Cloud Run (serverless, min-instances=0) |
| **CI/CD** | GitHub Actions → Cloud Run on push to `main` |
| **Frontend** | Vanilla JS, dark HUD theme, PWA (installable) |
| **Logging** | Logstash JSON encoder → GCP Cloud Logging |

---

## Project Structure

```
jarvis-ai-assistant/
├── jarvis-ai/                        # Spring Boot app
│   ├── src/main/java/com/jarus/ai/
│   │   ├── model/                    # 11 POJOs (JobPost, ParsedResume, etc.)
│   │   ├── security/                 # SecurityConfig, OAuth2 handler, EncryptionService
│   │   ├── config/                   # WebClient, CORS, WebMVC / rate limit
│   │   ├── repository/               # 7 Firestore repositories
│   │   ├── service/                  # 9 services (Gemini, Gmail, GCS, Push, etc.)
│   │   ├── controller/               # 10 REST controllers
│   │   ├── filter/                   # LoggingFilter (MDC), RateLimitInterceptor
│   │   ├── scheduler/                # JobScanScheduler (8 AM cron)
│   │   └── JarusAiApplication.java
│   ├── src/main/resources/
│   │   ├── static/                   # index.html, CSS, JS, manifest.json, sw.js
│   │   ├── application.properties
│   │   └── logback-spring.xml
│   ├── Dockerfile                    # Multi-stage build (Gradle → JRE alpine)
│   └── build.gradle
├── .github/workflows/deploy.yml      # Auto-deploy to Cloud Run on push to main
├── .env.example                      # Template for all required env vars
└── .gitignore
```

---

## Local Setup

### Prerequisites
- Java 17+ ([Temurin 17](https://adoptium.net/temurin/releases/?version=17&os=windows&arch=x64))
- A Google Cloud project with **Firestore** and **Cloud Storage** enabled
- Google OAuth2 credentials (Cloud Console → APIs & Services → Credentials)
- A free [Gemini API key](https://ai.google.dev/)

### 1. Clone & configure

```bash
git clone https://github.com/suraj-suryn/jarvis-ai-assistant.git
cd jarvis-ai-assistant
cp .env.example .env   # fill in your values
```

### 2. Set environment variables and run

```bash
cd jarvis-ai
# Windows PowerShell
$env:GOOGLE_OAUTH_CLIENT_ID="your-client-id"
$env:GOOGLE_OAUTH_CLIENT_SECRET="your-secret"
$env:APP_ENCRYPTION_SECRET="any-random-32-char-string!!"
$env:ADMIN_EMAIL="your@gmail.com"
$env:GCP_PROJECT_ID="your-gcp-project"
$env:GOOGLE_APPLICATION_CREDENTIALS="path/to/service-account.json"
.\gradlew.bat bootRun
```

Open **http://localhost:8080** and log in with your Google account.

---

## Deploy to Google Cloud Run

Push to `main` — GitHub Actions handles the rest automatically.

### Required GitHub Secrets

Go to **Repo → Settings → Secrets and variables → Actions** and add:

| Secret | Value |
|---|---|
| `GCP_SA_KEY` | Service account JSON (base64 or raw) |
| `GCP_PROJECT_ID` | Your GCP project ID |
| `CLOUD_RUN_SERVICE_NAME` | e.g. `jarus-ai` |
| `GOOGLE_OAUTH_CLIENT_ID` | OAuth2 client ID |
| `GOOGLE_OAUTH_CLIENT_SECRET` | OAuth2 client secret |
| `APP_ENCRYPTION_SECRET` | 32-char random string |
| `ADMIN_EMAIL` | Your Gmail address |
| `GCS_BUCKET_NAME` | e.g. `jarus-files` |
| `GOOGLE_CSE_API_KEY` | Google Custom Search key (optional) |
| `GOOGLE_CSE_ID` | Custom Search engine ID (optional) |
| `VAPID_PUBLIC_KEY` | VAPID public key for Web Push |
| `VAPID_PRIVATE_KEY` | VAPID private key for Web Push |

Generate VAPID keys with:
```bash
npx web-push generate-vapid-keys
```

---

## API Endpoints

| Method | Path | Description |
|---|---|---|
| `POST` | `/api/resume/upload` | Upload PDF or DOCX resume |
| `POST` | `/api/resume/tailor` | AI-tailor resume to a job |
| `GET` | `/api/resume/download/{id}?format=pdf\|docx` | Download tailored resume |
| `POST` | `/api/jobs/capture` | Capture job (bookmarklet) |
| `GET` | `/api/jobs` | List all captured jobs |
| `PATCH` | `/api/jobs/{id}/status` | Move job in pipeline |
| `POST` | `/api/jobs/scan` | Trigger manual job scan |
| `GET` | `/api/company/research` | Company + interview research |
| `POST` | `/api/cover-letter/generate` | Generate AI cover letter |
| `GET` | `/api/email/jobs` | Fetch job-related Gmail messages |
| `POST` | `/api/settings/gemini-key` | Save encrypted Gemini API key |
| `GET` | `/api/settings` | Get user settings + profile |
| `POST` | `/api/push/subscribe` | Subscribe to push notifications |
| `GET` | `/api/admin/users` | List allowed users (admin only) |
| `DELETE` | `/api/account` | Delete all user data |

---

## Security

- **Authentication**: Google OAuth2 — only whitelisted emails can log in
- **Gemini API key**: AES-256/GCM encrypted before storing in Firestore; never logged
- **Rate limiting**: Bucket4j — 10 req/hr for tailor/cover-letter, 20/hr for research
- **CSRF**: Disabled only for `/api/jobs/capture` (bookmarklet) and `/api/push/**`
- **HTTPS**: Enforced by Cloud Run; local dev uses HTTP only

---

Developed by **Suraj Prasad**
