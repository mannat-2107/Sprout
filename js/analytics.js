/**
 * Sprout 2.0 Funnel Analytics Engine
 * Tracks exact events specified in Implementation Plan:
 * - homepage view, scroll depth, cta tap, store click
 * - onboarding screen views 1-5, kyc substeps, kyc wait shown, kyc abandoned
 * - first investment started and completed
 * - organic vs paid traffic tagging
 */

class SproutAnalytics {
  constructor() {
    this.sessionId = "sess_" + Math.random().toString(36).substring(2, 9);
    this.source = this.detectTrafficSource();
    this.events = [];
    this.listeners = [];
    this.scrollMilestones = new Set();
    
    this.initScrollTracking();
  }

  detectTrafficSource() {
    const params = new URLSearchParams(window.location.search);
    if (params.get("utm_source") || params.get("gclid") || params.get("fbclid")) {
      return "paid";
    }
    return document.referrer && !document.referrer.includes(window.location.host) ? "referral" : "organic";
  }

  track(eventName, properties = {}) {
    const payload = {
      event: eventName,
      timestamp: new Date().toISOString(),
      sessionId: this.sessionId,
      source: this.source,
      url: window.location.pathname,
      properties: properties
    };

    this.events.push(payload);
    console.log(`[Sprout Analytics] [${payload.source.toUpperCase()}] ${eventName}`, properties);

    // Notify UI event inspector listeners
    this.listeners.forEach(fn => fn(payload));

    // Send to Java backend /api/analytics asynchronously
    try {
      fetch("/api/analytics", {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify(payload)
      }).catch(() => {
        // Silent catch for static standalone viewing
      });
    } catch (e) {
      // Standalone mode
    }
  }

  onEvent(callback) {
    this.listeners.push(callback);
  }

  initScrollTracking() {
    let ticking = false;
    window.addEventListener("scroll", () => {
      if (!ticking) {
        window.requestAnimationFrame(() => {
          const scrollH = document.documentElement.scrollHeight - window.innerHeight;
          if (scrollH <= 0) return;
          const pct = Math.round((window.scrollY / scrollH) * 100);

          [25, 50, 75, 100].forEach(milestone => {
            if (pct >= milestone && !this.scrollMilestones.has(milestone)) {
              this.scrollMilestones.add(milestone);
              this.track("scroll_depth", { depthPercent: milestone });
            }
          });
          ticking = false;
        });
        ticking = true;
      }
    }, { passive: true });
  }
}

window.sproutAnalytics = new SproutAnalytics();
