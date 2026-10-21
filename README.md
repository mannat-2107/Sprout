# 🌱 Sprout — Paperless Micro-SIP Investing

**Sprout** is a mobile-first web application that lets first-time investors in India start a systematic investment plan (SIP) in direct mutual funds — paperlessly, from ₹10/day.

---

## ✨ Features

| Feature | Details |
|---|---|
| **Phone + OTP Auth** | Real OTP generated for any number; register & login flows |
| **Paperless KYC** | PAN validation (NSDL), Aadhaar (12-digit number **or** document upload), Bank penny-drop |
| **DigiLocker Integration** | Aadhaar XML fetch via UIDAI / DigiLocker consent |
| **Investment Pace Selector** | Daily / Weekly / Monthly SIP with live deployment calculator |
| **Mandate PDF Download** | Generates a real NPCI e-NACH mandate confirmation with user data |
| **CAS Statement** | Consolidated Account Statement download |
| **Investor Profile** | View KYC status, linked PAN, bank, SIP details, and documents |
| **i18n** | English + Hindi (हिंदी) language toggle |
| **SEBI Compliance** | Statutory disclaimers, AMFI ARN placeholder, no false claims |

---

## 🏗️ Architecture

```
sprout/
├── SproutServer.java       # Pure Java HTTP server (no framework)
├── app.html                # Mobile onboarding app (5-screen SPA)
├── index.html              # Marketing landing page
├── css/
│   ├── tokens.css          # Design system tokens
│   ├── components.css      # Shared component styles
│   └── onboarding.css      # App-specific styles
├── js/
│   ├── onboarding.js       # Onboarding state machine & all UI logic
│   ├── analytics.js        # Funnel telemetry
│   ├── mascot.js           # Sprout mascot SVG
│   └── strings.js          # i18n strings (EN / HI)
├── backend/                # Java check utilities
├── database/               # PostgreSQL schema + migrations
│   ├── schema.sql
│   ├── test_seed.sql
│   └── 002_auth.sql
├── docker-compose.yml      # PostgreSQL dev database
└── lib/                    # JDBC driver
```

---

## 🚀 Quick Start

### Prerequisites
- Java 17+ (JDK)
- Docker Desktop (for the database)

### 1 — Start the database
```bash
docker compose up -d
```

### 2 — Compile & run the server
```bash
javac -d out SproutServer.java
java -cp out SproutServer
```

### 3 — Open the app
```
http://localhost:8080/app.html
```

---

## 🔑 KYC Flow

1. **PAN** — Enter 10-digit PAN (e.g. `ABCDE1234F`) or upload a card photo (AI OCR simulated)
2. **Aadhaar** — Choose:
   - **Enter 12-digit number** → DigiLocker OTP consent
   - **Upload document** → AI pre-check
3. **Bank** — Account number + IFSC → ₹1 IMPS penny-drop verification
4. Run → 6-second animated verification → KYC complete

---

## 🔐 Auth Flow

- Enter any 10-digit Indian mobile number
- OTP is generated dynamically (shown in dev toast)
- Demo override: OTP `849201`
- New users: "Create Account" shows a name field
- Returning users: "Sign In" (OTP-only)

---

## 📋 API Endpoints

| Method | Route | Description |
|---|---|---|
| `POST` | `/api/auth/otp/send` | Generate & dispatch OTP |
| `POST` | `/api/auth/otp/verify` | Verify OTP → issue session token |
| `POST` | `/api/auth/logout` | Revoke session |
| `POST` | `/api/kyc/verify` | Verify PAN, Aadhaar, bank account |
| `GET/POST` | `/api/onboarding` | Save / restore onboarding progress |
| `POST` | `/api/investment/order` | Place SIP order |
| `GET` | `/api/portfolio` | Fetch portfolio holdings |
| `GET` | `/api/user/profile` | Investor profile |
| `GET` | `/api/health` | Health check |

---

## ⚖️ Compliance Notes

- All ARN references are `[ARN-PLACEHOLDER]` — **replace before go-live**
- Statutory disclaimers are present on every screen
- No guaranteed return claims; only illustrative historical figures
- "0% fee" removed — direct plan expense ratios disclosed

---

## 📄 License

Private repository — © Sprout Technologies Pvt. Ltd.
