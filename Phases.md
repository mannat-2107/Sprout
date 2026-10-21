# Phased Implementation Plan & Cut List: Sprout 2.0

## 1. Timeline Breakdown (3-Week Execution Plan)

### Week 1: Foundation, Tokens, Copy & Compliance Lock
- **Day 1:** Kickoff with CEO, Marketing, Compliance, Support. Present fixed scope and the strict "No" list. Lock single front-end developer ownership.
- **Day 1–2:** Copy Deck v1 for Homepage and all 5 onboarding screens. Eliminate all banned words (`guaranteed`, `safe`, `risk-free`, em dashes). Submit to Compliance.
- **Day 2–3:** Mascot selection: Time-box abstract botanical treatments with CEO. Pick minimal geometric option.
- **Day 2–3:** Design token extraction: Color palette with automated WCAG AA contrast check, type scale, 44px tap targets.
- **Day 3–5:** Engineering setup: Java HTTP server (`SproutServer.java`), base design tokens (`tokens.css`), component library (`components.css`), string file architecture (`strings.js`), and analytics bus (`analytics.js`).
- **Day 4–5:** Mid-fidelity wireframes for homepage and 5 onboarding screens.
- **Day 5:** Compliance written feedback on Copy v1. Finalize and lock Copy Deck v2.
- **Exit Criteria for Week 1:** Scope signed off, mascot approved, palette passes WCAG AA, copy approved in writing, foundation components coded.

---

### Week 2: Build Homepage, 5 Screens & Java Backend
- **Day 6–7:** Homepage mobile build: Hero, pre-flight document checklist, 3-step path, trust indicators, accessible FAQ accordion, bottom CTA.
- **Day 8:** Homepage desktop layout: Responsive breakpoint rearrangement, QR code desktop-to-mobile handoff, legal pages (`privacy.html`, `terms.html`), favicon setup.
- **Day 8–9:** Mascot SVG integration: Implement single SVG with 3 CSS-only states (`idle`, `growing`, `milestone`).
- **Day 9–10:** Onboarding Screens 1–3:
  - Screen 1: Welcome & core pillars.
  - Screen 2: Pre-KYC checklist with "Save progress & finish later".
  - Screen 3: Micro-SIP goal/pace selector with 5-year compounding calculator.
- **Day 10:** Onboarding Screens 4–5:
  - Screen 4: Digital identity entry + transparent 18s waiting state with countdown and heartbeat status messages.
  - Screen 5: Nifty 50 Index first-investment moment + milestone celebration.
- **Exit Criteria for Week 2:** All 5 screens working end-to-end on Java server; analytics events firing reliably into `/api/analytics`.

---

### Week 3: Test, Audit, Pre-Launch Gate & Staged Rollout
- **Day 11:** Performance audit: Total page weight (< 1.5 MB target), throttled 4G testing (under 3s load), memory profiling on sub-₹10,000 Android devices.
- **Day 11–12:** Accessibility audit: Screen reader labels, 44px tap target verification, keyboard focus traps, reduced motion verification, 200% text zoom stability.
- **Day 12:** Compliance audit on live staging: Verify sticky disclaimer visibility across all screens, test absence of banned words. Written sign-off from Compliance Officer.
- **Day 12–13:** Usability test with 5 first-time retail investors: Observe checklist comprehension, waiting state comfort, and first-investment confidence. Patch top issues only.
- **Day 14:** Pre-launch gate audit (see Section 2). Configure staged rollout canary deployment.
- **Day 15:** Production Launch. Continuous error and drop-off monitoring.

---

## 2. Pre-Launch Gate Verification Checklist
The application can be released to production traffic only when all 11 conditions are met:
- [x] Custom domain connected with HTTPS enabled.
- [x] High-contrast SVG favicon set and displaying in browser tabs.
- [x] Privacy Policy and Terms of Service live and linked in footer.
- [x] All platform, template, or AI-generated branding completely removed.
- [x] Zero fake reviews, fake customer counters, or fabricated metrics.
- [x] Compliance disclaimer strictly $\ge$ 12px (implemented at 13px) and visible without scrolling.
- [x] Words "guaranteed", "safe", and "risk-free" absent from all copy, code, and metadata.
- [x] Total page weight strictly under 1.5 MB (current: ~48 KB total).
- [x] All animations under 100 KB (current: ~2.8 KB total).
- [x] WCAG AA contrast ratio and 44px tap targets verified.
- [x] Analytics telemetry verified receiving events on the Java backend.

---

## 3. Risk Matrix & Strict Cut List

### 3.1 Risk Mitigation Table
| Risk | Probability | Severity | Mitigation Strategy |
| :--- | :--- | :--- | :--- |
| **Compliance feedback delayed** | High | High | Send Copy Deck on Day 2; pre-book review slot; use pre-approved statutory wording. |
| **Mascot design deadlock** | Medium | Medium | Time-box CEO review to Day 3. Fallback default is the abstract geometric sprout. |
| **KYC vendor restrictions** | High | High | Wrap external vendor API responses in Sprout's transparent waiting UI rather than re-architecting the vendor SDK. |
| **Performance budget breach** | Low | High | Strip imagery first, then subset fonts, then disable secondary animations. Never cut the disclaimer. |
| **Feature creep from Marketing** | High | Medium | Reference the Phased Roadmap; schedule referral banner and video for Phase 2. |
| **Developer illness / blocker** | Low | High | Designer front-loads all assets and copy tokens; follow strict linear dependency order. |

### 3.2 Cut List (Order of Sacrifices if Schedule Slips)
1. **Cut 1:** Goal/pace screen (Screen 3) $\rightarrow$ replace with a simple default skip directly to KYC.
2. **Cut 2:** Desktop layout refinement $\rightarrow$ revert to clean single-column centered layout.
3. **Cut 3:** Mascot milestone blooming animation $\rightarrow$ keep static green sprout icon.
4. **Cut 4:** Homepage FAQ accordion $\rightarrow$ move to a separate dedicated help document.

### 3.3 What Must NEVER Be Cut
- The Pre-KYC Preparation Checklist (Screen 2)
- The Explicit KYC Waiting State with Live Countdown (Screen 4)
- The Sticky Regulatory Risk Disclaimer Component
- The Privacy Policy & Terms of Service Pages
- The Core Funnel Analytics Telemetry
