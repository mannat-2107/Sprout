# Project Memory & Context Persistence: Sprout 2.0

> **Notice for Future AI Sessions & Developers:**
> This file persists critical project state, architectural decisions, completed work, and verification logs. Read this file first before making changes or proposing new features to prevent hallucination, redundant discovery, or architectural drift.

---

## 1. Active Project State & Runtime Status
- **Repository Location:** `c:\Users\ST\Desktop\sprout\`
- **Backend Service:** Pure Java HTTP/REST server (`SproutServer.java`)
  - **Runtime:** Java SE 26 (HotSpot 64-bit Server VM)
  - **Local Port:** `http://localhost:8080/`
  - **Health Endpoint:** `http://localhost:8080/api/health` $\rightarrow$ Returns `{"status":"UP","runtime":"Java 26","app":"Sprout 2.0"}`
  - **Background Daemon:** Running and monitored via agent task manager (`task-60`).
- **Frontend Architecture:** Static-first, zero-framework (HTML5 + CSS3 + Vanilla ES6 JS). Total bundle footprint is **~49 KB** (budget cap: 1,500 KB).
- **Localization:** Key-based architecture supporting English (`en`) and Hindi (`hi`) toggled on the fly via `data-i18n` attributes.

---

## 2. Integrated High-Performing Components (From Reference Screens)

Based on institutional fintech reference screens, the onboarding flow now features the following upgraded components:
1. **Segmented Step Progress Indicator:** Horizontal multi-segment track (`STEP X OF 5 • [NAME]`) with dynamic time estimates (`⏱ ~1 min`, `⏱ ~3 mins`, etc.).
2. **Micro-SIP Value Proposition System (Screen 1):** Value cards featuring *Start Tiny (₹10/day)*, *Direct Mutual Funds (0% Fee)*, and *Zero Lock-in (Instant exit)*.
3. **Official KYC Protocol Banner & Preparation Checklist (Screen 2):** Document readiness cards (*PAN [Required]*, *Aadhaar [DigiLocker]*, *Bank & IFSC [SIP Setup]*) with an explicit auto-save reassurance notice.
4. **Botanical Tier & Frequency Switcher (Screen 3):**
   - *BOTANICAL TIER II • First Sprout* badge.
   - Segmented frequency toggle (*Daily*, *Weekly*, *Monthly*).
   - Deposit amount cards with contextual analogies (*₹20 / day: A cup of tea*, *₹50 / day: Most chosen*, *₹100 / day: Steady compounding*).
   - Dynamic *Monthly Deployment* breakdown box displaying total monthly deployment based on frequency and amount.
5. **Multi-Substep Verification & Government Repositories Card (Screen 4):**
   - 3-tier sub-step verification checklist (*PAN Validation [Verified]*, *Aadhaar DigiLocker [Verified]*, *CKYC & Bank Verification [In Progress]*).
   - Government Repositories waiting card featuring a live elapsed timer (`00:00 elapsed`), progress bar, and multi-stage milestone track (*Request Dispatched* $\rightarrow$ *Awaiting CERSAI Ack* $\rightarrow$ *Settlement*).
   - *Continue in Background (Notify via SMS)* fallback action for high-friction sessions.
   - *GROWTH MILESTONE 1 OF 4: Seed in Soil* badge.
6. **NPCI Mandate & Order Summary Card (Screen 5):**
   - *[NPCI Active]* badge, daily commitment display, fund selection, auto-debit bank route via e-NACH, and T+1 business day allotment timeline.
   - *1-Tap Control* and *Zero Hidden Cost* trust pillars.
   - *BOTANICAL LEDGER: Stage 2 of 4 Active* badge.
   - Action to *Download Mandate Confirmation (PDF)* and *Go to Your Portfolio*.

---

## 3. Inventory of Project Files

| Deliverable | Path | Status | Notes |
| :--- | :--- | :--- | :--- |
| **Java Server** | [`SproutServer.java`](file:///c:/Users/ST/Desktop/sprout/SproutServer.java) | **Live & Compiled** | Implements static serving, `/api/analytics`, `/api/kyc/verify`, `/api/investment/order`. |
| **PostgreSQL Schema**| [`database/schema.sql`](file:///c:/Users/ST/Desktop/sprout/database/schema.sql) | **Shipped** | 19 core tables (Phases 1-3), paise integer math, append-only trigger, KYC/ledger constraints. |
| **OpenAPI Spec** | [`backend/openapi.yaml`](file:///c:/Users/ST/Desktop/sprout/backend/openapi.yaml) | **Shipped** | OpenAPI 3.0 spec for Auth, Onboarding, Consents, KYC Webhooks, and Orders. |
| **KYC State Machine**| [`backend/KycStateMachine.java`](file:///c:/Users/ST/Desktop/sprout/backend/KycStateMachine.java) | **Tested** | Cryptographic HMAC-SHA256 signature verification, deduplication, state guardrails. |
| **Order Service** | [`backend/OrderService.java`](file:///c:/Users/ST/Desktop/sprout/backend/OrderService.java) | **Tested** | Idempotency key registry, pre-flight KYC check, append-only double-entry ledger. |
| **Backend Unit Tests**| [`backend/OrderServiceTest.java`](file:///c:/Users/ST/Desktop/sprout/backend/OrderServiceTest.java) | **Passed (4/4)** | Verified ₹100 order, 10x duplicate idempotency replay, KYC rejection, and state jumps. |
| **Master Web App** | [`index.html`](file:///c:/Users/ST/Desktop/sprout/index.html) | **Shipped & Tested** | Standalone marketing website with clean navigation to dedicated app. |
| **Standalone App** | [`app.html`](file:///c:/Users/ST/Desktop/sprout/app.html) | **Shipped & Tested** | Dedicated mobile web application with upgraded 5-screen onboarding machine. |
| **Design Tokens** | [`css/tokens.css`](file:///c:/Users/ST/Desktop/sprout/css/tokens.css) | **Shipped** | WCAG AA audited botanical colors, 6px border radii, 44px tap targets, typography scale. |
| **Shared Components**| [`css/components.css`](file:///c:/Users/ST/Desktop/sprout/css/components.css) | **Shipped** | Segmented step bar, botanical tier badges, gov repo card, amount cards, mandate card. |
| **Homepage Layout** | [`css/homepage.css`](file:///c:/Users/ST/Desktop/sprout/css/homepage.css) | **Shipped** | Mobile-first CSS with desktop breakpoint at 768px, QR handoff, FAQ accordion. |
| **Onboarding CSS** | [`css/onboarding.css`](file:///c:/Users/ST/Desktop/sprout/css/onboarding.css) | **Shipped** | Phone mockup frame, pace selector, KYC waiting state styles. |
| **Legal Styles** | [`css/legal.css`](file:///c:/Users/ST/Desktop/sprout/css/legal.css) | **Shipped** | Accessible typography for statutory documentation. |
| **String Dictionaries**| [`js/strings.js`](file:///c:/Users/ST/Desktop/sprout/js/strings.js) | **Shipped** | English and Hindi dictionaries. Centralized compliance disclaimer. Zero banned words. |
| **Mascot Engine** | [`js/mascot.js`](file:///c:/Users/ST/Desktop/sprout/js/mascot.js) | **Shipped** | Inline SVG with 3 stages (`idle`, `growing`, `milestone`). < 3 KB CSS transform animation. |
| **Funnel Analytics** | [`js/analytics.js`](file:///c:/Users/ST/Desktop/sprout/js/analytics.js) | **Shipped** | Event dispatcher tracking all required funnel events; logs to Java backend & UI drawer. |
| **Onboarding Logic** | [`js/onboarding.js`](file:///c:/Users/ST/Desktop/sprout/js/onboarding.js) | **Shipped** | Interactive 5-screen flow with live elapsed timer, dynamic deployment calc, and CERSAI progress. |
| **App Controller** | [`js/app.js`](file:///c:/Users/ST/Desktop/sprout/js/app.js) | **Shipped** | Locale switcher, view switcher (Homepage vs App Prototype), FAQ toggles. |
| **Privacy Policy** | [`privacy.html`](file:///c:/Users/ST/Desktop/sprout/privacy.html) | **Shipped** | SEBI & DPDP compliant privacy policy with Grievance Officer contacts. |
| **Terms of Service** | [`terms.html`](file:///c:/Users/ST/Desktop/sprout/terms.html) | **Shipped** | AMFI ARN-281942 registered mutual fund distributor statutory terms. |
| **Favicon** | [`favicon.svg`](file:///c:/Users/ST/Desktop/sprout/favicon.svg) | **Shipped** | Minimalist botanical brand icon. |
| **PRD.md** | [`PRD.md`](file:///c:/Users/ST/Desktop/sprout/PRD.md) | **Shipped** | Product requirements document. |
| **Architecture.md** | [`Architecture.md`](file:///c:/Users/ST/Desktop/sprout/Architecture.md) | **Shipped** | System architecture & technical specs. |
| **Rules.md** | [`Rules.md`](file:///c:/Users/ST/Desktop/sprout/Rules.md) | **Shipped** | Engineering rules & regulatory boundaries. |
| **Phases.md** | [`Phases.md`](file:///c:/Users/ST/Desktop/sprout/Phases.md) | **Shipped** | Phased roadmap and cut list. |
| **Design.md** | [`Design.md`](file:///c:/Users/ST/Desktop/sprout/Design.md) | **Shipped** | Visual design system specification. |
| **Memory.md** | [`Memory.md`](file:///c:/Users/ST/Desktop/sprout/Memory.md) | **Shipped** | Persistent session context & decisions log. |

---

## 4. End-to-End Verification History

### Browser Test Runs
1. **Initial Flow Test (`sprout_e2e_demo`):** Verified basic responsive homepage and onboarding machine.
2. **Upgraded Component Test (`sprout_best_components_demo`):**
   - **Video Recording:** `sprout_best_components_demo_1792556597699.webp`
   - Verified segmented progress bar across all 5 screens.
   - Tested Daily vs. Weekly frequency toggles and pace amount cards (₹20, ₹50, ₹100).
   - Observed live government repository elapsed timer and CERSAI acknowledgment progression.
   - Verified NPCI Active Mandate Summary and interactive PDF download.
