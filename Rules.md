# Engineering Rules & Regulatory Boundaries: Sprout 2.0

## 1. Technical Stack Boundaries
1. **Backend Language:** **Java exclusively.** Use pure Java standard library (`com.sun.net.httpserver.HttpServer`). Under no circumstances introduce Python, Node/Express, or external frameworks unless explicitly mandated.
2. **Frontend Stack:** Vanilla HTML5, Vanilla CSS3, and Modern Vanilla JavaScript (ES6+). Do not inject React, Vue, Svelte, or Next.js runtimes. Keep the client completely static-renderable from any CDN.
3. **Styling Engine:** Pure CSS variables with BEM-inspired architectural class naming. Do not install TailwindCSS or heavy CSS-in-JS libraries.
4. **Animation Constraints:** CSS transforms and opacity exclusively (`scale`, `translateY`, `rotate`, `opacity`). Total animation bundle must not exceed 100 KB. Every animation must support `@media (prefers-reduced-motion: reduce)`.

---

## 2. Regulatory Compliance Constraints (SEBI & AMFI)
1. **Forbidden Terminology (Zero Tolerance):**
   - **NEVER** use the words:
     - `"guaranteed"`
     - `"safe"`
     - `"risk-free"`
     - `"assured returns"`
   - This ban applies across all user-facing copy, alt text, meta descriptions, error messages, placeholders, code comments, and test scripts.
2. **Statutory Risk Disclaimer:**
   - Every view containing investment content must display the compliance disclaimer in at least **12px font size** (Sprout standard: 13px / 0.8125rem).
   - On mobile, it must be visible without scrolling via a sticky/fixed bottom bar or clean strip.
   - All disclaimer text must reference the single centralized string in `strings.js` (`complianceDisclaimer`) so the Compliance team can audit and modify it in one file.
3. **Registration Disclosures:**
   - Always state Sprout's regulatory status accurately: *"Sprout Technologies Pvt. Ltd. is a SEBI-registered mutual fund distributor (ARN-281942)."*

---

## 3. Anti-Vibe-Coding Guidelines (Visual & Content Guardrails)
To prevent generic, untrustworthy, or AI-generated design patterns:
1. **No Purple Gradients:** Do not use violet, purple, or neon gradients. Rely exclusively on the botanical trust palette: deep pine `#0E281C`, forest green `#155A37`, sea green `#2E8B57`, and warm linen paper `#FBFBF9`.
2. **No Pill-Shaped Buttons:** Buttons must use clean, architectural rectangular geometry with subtle border radii (`border-radius: 6px` or `8px`). Never use `border-radius: 9999px`.
3. **No Fake Reviews or Counters:** Never display invented social proof (e.g., "Rated 4.9 stars by 1M users", "Rohan from Pune just invested ₹500"). Use only verifiable facts and product mechanics.
4. **No Vague Hero Text:** Avoid empty marketing poetry like *"Unlock your financial destiny"*. Use concrete, honest, literal descriptions: *"Invest in index funds with as little as ₹100."*
5. **No Emoji Icons:** Do not use system emojis (🚀, 📈, 💰, 🌱) as interface icons. Use precision geometric SVG line icons with uniform 2px stroke width.
6. **No Em Dashes:** Never use the em dash (`—`). Use standard hyphens with spaces or clean sentence breaks.
7. **No Crazy Scroll or Cursor Animations:** Disable custom cursor trails, 3D tilt effects, parallax lag, and scroll-hijacking.
8. **No "Made with AI" Tags:** The product must present as a tier-1 institutional financial product built by human engineers.
9. **No AI-Slop Stock Photos:** Do not use plastic, airbrushed AI renders of smiling business people. Favor crisp vector diagrams, typography, and the structured SVG mascot.

---

## 4. Accessibility & Device Standards (Sub-₹10,000 Target)
1. **Touch Targets:** Minimum 44px $\times$ 44px for every clickable element, button, tab, and form input.
2. **Color Contrast:** All body text must meet or exceed WCAG AA standards (minimum 4.5:1 for body copy, 3:1 for large headers).
3. **Input Labels:** Every input must have an explicit, associated `<label>` tag.
4. **Memory Footprint:** Prevent memory leaks in timers. Always `clearInterval` on unmount or transition.
5. **Keyboard Accessibility:** All interactive elements must have visible focus rings (`:focus-visible`).
