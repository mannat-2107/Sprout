/**
 * Sprout 2.0 High-Performing Onboarding State Machine
 * Incorporates the best components from user-provided fintech screens:
 * 1. Segmented step indicator with time estimates
 * 2. Pre-KYC protocol checklist with auto-save
 * 3. Daily/Weekly/Monthly micro-SIP pace selector with deployment calculator
 * 4. Multi-substep verification & Government Repositories waiting card with CERSAI progress
 * 5. NPCI mandate confirmation & Botanical Ledger tier progression
 */

class OnboardingFlow {
  constructor() {
    this.currentScreen = 1;
    this.frequency = "daily"; // "daily", "weekly", "monthly"
    this.depositAmount = 50;
    this.elapsedSeconds = 0;
    this.elapsedTimer = null;
    this.isKycComplete = false;
    this.isInvestmentComplete = false;
  }

  init() {
    this.bindEvents();
    this.goToScreen(1);
    this.updateDeploymentCalculator();
  }

  goToScreen(screenNum) {
    if (screenNum < 1 || screenNum > 5) return;
    this.currentScreen = screenNum;

    // Toggle screen DOM elements
    document.querySelectorAll(".ob-screen").forEach(el => el.classList.remove("is-active"));
    const target = document.getElementById(`ob-screen-${screenNum}`);
    if (target) target.classList.add("is-active");

    // Update segmented progress bar tracks
    const stepLabels = [
      { step: "PREVIEW", time: "~1 min" },
      { step: "PREPARATION", time: "~3 mins" },
      { step: "INVESTMENT PACE", time: "~2 mins" },
      { step: "KYC VERIFICATION", time: "~2 mins" },
      { step: "COMPLETE", time: "Success" }
    ];
    
    const metaStep = document.getElementById("step-meta-label");
    const metaTime = document.getElementById("step-meta-time");
    if (metaStep && stepLabels[screenNum - 1]) {
      metaStep.textContent = `STEP ${screenNum} OF 5 • ${stepLabels[screenNum - 1].step}`;
    }
    if (metaTime && stepLabels[screenNum - 1]) {
      metaTime.textContent = `⏱ ${stepLabels[screenNum - 1].time}`;
    }

    document.querySelectorAll(".step-track-segment").forEach((seg, idx) => {
      seg.classList.toggle("is-completed", idx < screenNum - 1);
      seg.classList.toggle("is-active", idx === screenNum - 1);
    });

    // Update top device nav dots
    document.querySelectorAll(".device-nav-dot").forEach((dot, idx) => {
      dot.classList.toggle("is-active", idx + 1 === screenNum);
    });

    // Track analytics
    window.sproutAnalytics.track("onboarding_screen_view", { screenNumber: screenNum });

    // Update Mascot Stage
    const mascotTarget = "#onboarding-mascot-mount";
    if (screenNum <= 2) {
      window.SproutMascot.setState(mascotTarget, "idle");
    } else if (screenNum <= 4) {
      window.SproutMascot.setState(mascotTarget, "growing");
    } else if (screenNum === 5) {
      window.SproutMascot.setState(mascotTarget, "milestone");
    }

    // Scroll to top
    const contentArea = document.querySelector(".phone-screen-content");
    if (contentArea) contentArea.scrollTop = 0;
  }

  bindEvents() {
    // Screen 1: Welcome CTA
    const btnWelcome = document.getElementById("ob-btn-welcome-start");
    if (btnWelcome) {
      btnWelcome.addEventListener("click", () => {
        window.sproutAnalytics.track("cta_tap", { buttonId: "welcome_get_started" });
        this.goToScreen(2);
      });
    }

    // Screen 2: Checklist CTA
    const btnChecklist = document.getElementById("ob-btn-checklist-ready");
    if (btnChecklist) {
      btnChecklist.addEventListener("click", () => {
        window.sproutAnalytics.track("cta_tap", { buttonId: "checklist_continue" });
        this.goToScreen(3);
      });
    }

    const btnSaveLater = document.getElementById("ob-btn-save-later");
    if (btnSaveLater) {
      btnSaveLater.addEventListener("click", () => {
        window.sproutAnalytics.track("cta_tap", { buttonId: "checklist_save_exit" });
        alert("Progress saved securely. You can return anytime to resume where you left off.");
      });
    }

    // Screen 3: Frequency Selector Tabs
    document.querySelectorAll(".freq-tab-btn").forEach(btn => {
      btn.addEventListener("click", () => {
        document.querySelectorAll(".freq-tab-btn").forEach(b => b.classList.remove("is-active"));
        btn.classList.add("is-active");
        this.frequency = btn.getAttribute("data-freq");
        this.updateDeploymentCalculator();
        window.sproutAnalytics.track("pace_frequency_selected", { frequency: this.frequency });
      });
    });

    // Screen 3: Amount Cards
    document.querySelectorAll(".amount-card").forEach(card => {
      card.addEventListener("click", () => {
        document.querySelectorAll(".amount-card").forEach(c => c.classList.remove("is-selected"));
        card.classList.add("is-selected");
        this.depositAmount = parseInt(card.getAttribute("data-amount"), 10) || 50;
        this.updateDeploymentCalculator();
        window.sproutAnalytics.track("deposit_amount_selected", { amount: this.depositAmount });
      });
    });

    const btnPaceConfirm = document.getElementById("ob-btn-pace-confirm");
    if (btnPaceConfirm) {
      btnPaceConfirm.addEventListener("click", () => {
        window.sproutAnalytics.track("cta_tap", { buttonId: "confirm_pace_verify_kyc" });
        this.goToScreen(4);
        this.startKycExchange();
      });
    }

    // Screen 4: Background notification action
    const btnContinueBg = document.getElementById("ob-btn-kyc-background");
    if (btnContinueBg) {
      btnContinueBg.addEventListener("click", () => {
        window.sproutAnalytics.track("cta_tap", { buttonId: "kyc_continue_in_background" });
        alert("Verification scheduled in background. You will receive an SMS confirmation once CERSAI clears your record.");
      });
    }

    // Screen 4: Proceed when verified
    const btnKycProceed = document.getElementById("ob-btn-kyc-proceed");
    if (btnKycProceed) {
      btnKycProceed.addEventListener("click", () => {
        window.sproutAnalytics.track("cta_tap", { buttonId: "kyc_proceed_to_mandate" });
        this.goToScreen(5);
      });
    }

    // Screen 5: Portfolio Action
    const btnPortfolio = document.getElementById("ob-btn-go-portfolio");
    if (btnPortfolio) {
      btnPortfolio.addEventListener("click", () => {
        window.sproutAnalytics.track("cta_tap", { buttonId: "go_to_portfolio" });
        alert("Welcome to your Sprout Portfolio! Your discipline has begun compounding.");
      });
    }

    // PDF Download Mock
    const btnPdf = document.getElementById("ob-btn-download-pdf");
    if (btnPdf) {
      btnPdf.addEventListener("click", () => {
        window.sproutAnalytics.track("cta_tap", { buttonId: "download_mandate_pdf" });
        // COMPLIANCE: Replace [ARN-PLACEHOLDER] with real AMFI ARN before going live
        alert("Downloading Mandate Confirmation PDF (Ref: CKYC-9042-M / ARN: [ARN-PLACEHOLDER])...");
      });
    }

    // Top dot screen jumper
    document.querySelectorAll(".device-nav-dot").forEach(dot => {
      dot.addEventListener("click", () => {
        const targetScreen = parseInt(dot.getAttribute("data-screen"), 10);
        this.goToScreen(targetScreen);
      });
    });

    // Back button
    const backBtn = document.getElementById("phone-back-btn");
    if (backBtn) {
      backBtn.addEventListener("click", () => {
        if (this.currentScreen > 1) {
          this.goToScreen(this.currentScreen - 1);
        }
      });
    }
  }

  updateDeploymentCalculator() {
    let monthlyTotal = 0;
    if (this.frequency === "daily") {
      monthlyTotal = this.depositAmount * 30;
    } else if (this.frequency === "weekly") {
      monthlyTotal = this.depositAmount * 4;
    } else {
      monthlyTotal = this.depositAmount;
    }

    const heading = document.getElementById("calc-deployment-heading");
    if (heading) {
      heading.textContent = `At ₹${this.depositAmount} / ${this.frequency}, you will invest ₹${monthlyTotal.toLocaleString("en-IN")} every month.`;
    }

    // Update screen 5 summary card to match
    const summaryCommitment = document.getElementById("summary-daily-commitment");
    if (summaryCommitment) {
      summaryCommitment.textContent = `₹${this.depositAmount} / ${this.frequency}`;
    }
  }

  startKycExchange() {
    this.elapsedSeconds = 0;
    clearInterval(this.elapsedTimer);

    const timerDisplay = document.getElementById("kyc-elapsed-timer");
    const progressFill = document.getElementById("kyc-exchange-fill");
    const cersaiAck = document.getElementById("milestone-cersai");
    const settlementAck = document.getElementById("milestone-settlement");
    const successBox = document.getElementById("kyc-verified-box");
    const inProgressBtn = document.getElementById("ob-btn-checking-status");

    if (successBox) successBox.style.display = "none";
    if (inProgressBtn) inProgressBtn.style.display = "flex";

    window.sproutAnalytics.track("kyc_wait_shown", { initialDelay: 45 });

    this.elapsedTimer = setInterval(() => {
      this.elapsedSeconds++;
      
      const mins = String(Math.floor(this.elapsedSeconds / 60)).padStart(2, "0");
      const secs = String(this.elapsedSeconds % 60).padStart(2, "0");
      if (timerDisplay) timerDisplay.textContent = `${mins}:${secs} elapsed`;

      // Progress animation across 12 simulated seconds
      const pct = Math.min(100, Math.round((this.elapsedSeconds / 12) * 100));
      if (progressFill) progressFill.style.width = `${pct}%`;

      if (this.elapsedSeconds >= 4 && cersaiAck) {
        cersaiAck.classList.add("is-done");
      }
      if (this.elapsedSeconds >= 8 && settlementAck) {
        settlementAck.classList.add("is-done");
      }

      if (this.elapsedSeconds >= 12) {
        clearInterval(this.elapsedTimer);
        this.isKycComplete = true;

        // Update CKYC sub-step badge to verified
        const ckycBadge = document.getElementById("substep-ckyc-badge");
        const ckycIcon = document.getElementById("substep-ckyc-icon");
        if (ckycBadge) {
          ckycBadge.className = "substep-badge badge-green";
          ckycBadge.textContent = "VERIFIED";
        }
        if (ckycIcon) {
          ckycIcon.className = "substep-icon-status substep-status-verified";
          ckycIcon.innerHTML = "✓";
        }

        if (inProgressBtn) inProgressBtn.style.display = "none";
        if (successBox) successBox.style.display = "block";

        window.sproutAnalytics.track("kyc_substep_complete", { step: "central_registry_verified" });
      }
    }, 1000);
  }
}

window.OnboardingFlow = OnboardingFlow;
