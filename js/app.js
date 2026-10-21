/**
 * Sprout 2.0 Master Application Controller
 * Handles I18N, View Routing (Homepage vs Onboarding Simulator),
 * Accessibility checks, and Event Inspector.
 */

document.addEventListener("DOMContentLoaded", () => {
  let currentLang = "en";
  let currentView = "homepage"; // "homepage" or "onboarding"

  // Initialize Mascot mounts
  const heroMascotMount = document.getElementById("hero-mascot-mount");
  if (heroMascotMount) {
    heroMascotMount.innerHTML = window.SproutMascot.render("idle");
  }

  const obMascotMount = document.getElementById("onboarding-mascot-mount");
  if (obMascotMount) {
    obMascotMount.innerHTML = window.SproutMascot.render("idle");
  }

  // Initialize Onboarding Simulator
  const onboarding = new window.OnboardingFlow();
  onboarding.init();

  // Initial Language setup
  function updateLanguage(lang) {
    currentLang = lang;
    const strings = window.SPROUT_STRINGS[lang] || window.SPROUT_STRINGS.en;

    document.querySelectorAll("[data-i18n]").forEach(elem => {
      const key = elem.getAttribute("data-i18n");
      if (strings[key]) {
        if (elem.tagName === "INPUT" && elem.hasAttribute("placeholder")) {
          elem.placeholder = strings[key];
        } else {
          elem.textContent = strings[key];
        }
      }
    });

    // Toggle button active states
    document.querySelectorAll(".lang-btn").forEach(btn => {
      btn.classList.toggle("is-active", btn.getAttribute("data-lang") === lang);
    });

    document.documentElement.lang = lang === "hi" ? "hi" : "en";
  }

  // Bind language buttons
  document.querySelectorAll(".lang-btn").forEach(btn => {
    btn.addEventListener("click", () => {
      const selected = btn.getAttribute("data-lang");
      updateLanguage(selected);
      window.sproutAnalytics.track("language_switched", { language: selected });
    });
  });

  // Switch between Homepage and Onboarding Flow
  function setViewMode(mode) {
    currentView = mode;
    const homeView = document.getElementById("view-homepage");
    const obView = document.getElementById("view-onboarding");

    if (mode === "homepage") {
      if (homeView) homeView.style.display = "block";
      if (obView) obView.style.display = "none";
      window.scrollTo(0, 0);
      window.sproutAnalytics.track("homepage_view");
    } else {
      if (homeView) homeView.style.display = "none";
      if (obView) obView.style.display = "flex";
      window.scrollTo(0, 0);
      onboarding.goToScreen(onboarding.currentScreen);
    }

    document.querySelectorAll(".view-mode-btn").forEach(btn => {
      btn.classList.toggle("is-active", btn.getAttribute("data-view") === mode);
    });
  }

  document.querySelectorAll(".view-mode-btn").forEach(btn => {
    btn.addEventListener("click", () => {
      setViewMode(btn.getAttribute("data-view"));
    });
  });

  // Launch CTA links from homepage
  document.querySelectorAll(".trigger-onboarding-flow").forEach(cta => {
    cta.addEventListener("click", (e) => {
      e.preventDefault();
      window.sproutAnalytics.track("cta_tap", { buttonId: cta.id || "homepage_launch_cta" });
      setViewMode("onboarding");
    });
  });

  const backToWebBtn = document.getElementById("btn-back-to-website");
  if (backToWebBtn) {
    backToWebBtn.addEventListener("click", () => {
      setViewMode("homepage");
    });
  }

  // FAQ Accordion
  document.querySelectorAll(".faq-item").forEach(item => {
    const qBtn = item.querySelector(".faq-question");
    if (qBtn) {
      qBtn.addEventListener("click", () => {
        const isOpen = item.classList.contains("is-open");
        document.querySelectorAll(".faq-item").forEach(i => i.classList.remove("is-open"));
        if (!isOpen) item.classList.add("is-open");
        window.sproutAnalytics.track("faq_toggled", { question: qBtn.textContent.trim() });
      });
    }
  });

  // Telemetry Console Drawer
  const telemetryDrawer = document.getElementById("telemetry-log-content");
  if (telemetryDrawer) {
    window.sproutAnalytics.onEvent((eventData) => {
      const entry = document.createElement("div");
      entry.className = "telemetry-item";
      entry.innerHTML = `
        <span class="telemetry-time">${eventData.timestamp.split("T")[1].replace("Z", "")}</span>
        <strong>${eventData.event}</strong>
        <code>${JSON.stringify(eventData.properties)}</code>
      `;
      telemetryDrawer.prepend(entry);
    });
  }

  const toggleTelBtn = document.getElementById("toggle-telemetry-btn");
  if (toggleTelBtn) {
    toggleTelBtn.addEventListener("click", () => {
      const drawer = document.getElementById("telemetry-inspector");
      if (drawer) drawer.classList.toggle("is-collapsed");
    });
  }

  // Initial runs
  updateLanguage("en");
  window.sproutAnalytics.track("homepage_view");
});
