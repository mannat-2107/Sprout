# Project Requirements Document (PRD): Sprout 2.0

## 1. Executive Summary & Problem Statement
Sprout is a mobile investing platform engineered for first-time retail investors across India, enabling participation in index mutual funds with contributions starting from ₹100.

### Current Baseline Metrics
- **Total Installs / Downloads:** 40,000
- **Completed KYC:** 9,000 (77.5% drop-off from install to KYC completion)
- **First-Time Investors:** 3,000 (66.7% drop-off from KYC to activation)
- **Homepage Bounce Rate:** 78% (measured over 2 weeks, including paid campaigns)
- **KYC Specific Drop-off Rate:** 62%
- **User Demographics:** 91% mobile users, 40% using devices priced under ₹10,000
- **Network Profile:** Average 4G load time of 6.2 seconds
- **User Perception:** 65% of surveyed users reported that "investing feels scary"
- **Language Preferences:** Significant demand for Hindi and regional language support

---

## 2. Customer Support Root-Cause Diagnostics
Direct customer ticket analysis reveals two primary UX failure points responsible for the majority of abandonment:
1. *"I didn't know I needed my PAN and Aadhaar ready"*
   - **Root Cause:** Users begin onboarding blind without document expectations, get interrupted during verification, and drop off permanently.
   - **Solution:** Mandatory Pre-KYC Preparation Checklist on the homepage and Screen 2 of onboarding with a prominent "Save progress & finish later" mechanism.
2. *"The app asked me to wait and I thought it crashed"*
   - **Root Cause:** KYC registry calls to CVL-KRA and bank penny drops take 15 to 25 seconds with opaque spinners. Users assume an app freeze and terminate the process.
   - **Solution:** Transparent, animated waiting state featuring an 18-second countdown timer, explicit progress heartbeat messages, and bold reassurance: *"Your app has not crashed. We are awaiting a secure confirmation from the central registry."*

---

## 3. Stakeholder Alignment & Conflict Resolution

| Stakeholder | Initial Stance / Demand | Resolution in Sprout 2.0 |
| :--- | :--- | :--- |
| **CEO** | "Apple-like: minimal, premium. No cartoon characters. Investors won't trust a cartoon." | Abstract, architectural SVG botanical mascot. Pure geometric lines and organic curves; no cartoon eyes or playful tropes. |
| **Head of Marketing** | "Competitor mascot got 2M views. We need a mascot, video, live ticker, 3 pop-ups, referral banner." | Mascot retained in 3 elegant SVG growth stages. Banners, tickers, pop-ups, and auto-play videos deferred to Post-Launch Phase to protect core trust and speed. |
| **Compliance Officer** | "Every investment page must show risk disclaimer in >= 12px, visible without scrolling. Banned: 'guaranteed', 'safe', 'risk-free'." | Global fixed/sticky disclaimer component (13px, high-contrast) visible on all pages. Strict ban on forbidden vocabulary enforced in code and copy. |
| **Lead Developer** | "Page weight < 1.5 MB (target < 600 KB). Animations < 100 KB. 1 front-end developer for 3 weeks." | Zero-dependency static front-end + pure Java backend (`SproutServer.java`). Total bundle weight under 50 KB; animations under 3 KB via CSS transforms. |
| **Customer Support Lead** | "Fix surprise document requests and silent KYC freezes." | Pre-flight checklist with save links + transparent heartbeat waiting states with countdowns. |

---

## 4. Scope & Feature Specifications

### 4.1 In Scope (Launch Requirement)
1. **Responsive Homepage:** Single responsive page serving mobile and desktop with clean breakpoint rearrangement, desktop QR store handoff, pre-flight document checklist, 3-step path, and FAQ.
2. **5-Screen App Onboarding Flow:**
   - **Screen 1 (Welcome):** Value pillars (SEBI-regulated, 0 lock-in, zero hidden fees) and initial idle mascot.
   - **Screen 2 (Preparation Checklist):** Explicit PAN, Aadhaar OTP, and Bank details checklist + "Save and finish later" link.
   - **Screen 3 (Goal & Pace):** Micro-SIP pace selector (₹100, ₹500, ₹1,000/mo) with 5-year compounding projection and compliant disclaimers.
   - **Screen 4 (KYC & Explicit Waiting State):** Clean identity entry, live 18s countdown timer, status heartbeat messages, and verification confirmation.
   - **Screen 5 (First-Investment Moment):** Nifty 50 Index Basket order confirmation, transparent fee ledger (₹0 platform fee), transaction ID generation, and milestone mascot state.
3. **Mascot System:** 3-stage SVG (Idle $\rightarrow$ Growing $\rightarrow$ Milestone) animated with CSS transforms only.
4. **Compliance System:** Single-source centralized disclaimer string component, visible without scrolling.
5. **Localization Infrastructure:** Centralized key-based string dictionaries for English (`en`) and Hindi (`hi`), auto-adjusting flexible layouts.
6. **Telemetry & Funnel Analytics:** Client-side tracking bus with real-time inspector and Java backend event persistence.
7. **Legal Compliance Documents:** Dedicated Privacy Policy (`privacy.html`) and Terms of Service (`terms.html`).

### 4.2 Out of Scope for Initial Launch
- Video player embeds and auto-playing media
- Live stock/index ticker feeds
- Promotional modal pop-ups
- Homepage referral banners
- Multi-language support beyond English and Hindi (Tamil, Telugu, Bengali queued for Phase 2)

---

## 5. Success Metrics & Post-Launch Targets
- **Homepage Bounce Rate:** Reduce from 78% to $\le$ 45%
- **KYC Completion Rate:** Increase from 38% to $\ge$ 65%
- **Time to First Investment:** Under 3.5 minutes for ready users
- **Customer Support Tickets:** $\ge$ 60% reduction in tickets mentioning "app crashed during KYC" or "didn't know about PAN"
