/**
 * Sprout Mascot Component (SVG with 3 Growth States)
 * Compliance & Design:
 * - One SVG with grouped parts
 * - Pure CSS transforms & opacity only (lightweight on sub-₹10,000 devices)
 * - 3 states: idle, growing, milestone
 * - Never carries text inside the SVG
 * - Under 3 KB total footprint
 */

const SproutMascot = {
  render(state = "idle") {
    return `
      <div class="mascot-container mascot-state-${state}" data-mascot-state="${state}" role="img" aria-label="Sprout growth mascot">
        <svg class="mascot-svg" viewBox="0 0 120 120" fill="none" xmlns="http://www.w3.org/2000/svg">
          <!-- Background Milestone Halo (Stage 3 only) -->
          <circle class="mascot-aura" cx="60" cy="58" r="42" fill="#DCFCE7" opacity="0" />
          <circle class="mascot-aura" cx="60" cy="58" r="32" fill="#BBF7D0" opacity="0" />
          
          <!-- Base Pod / Earth Foundation -->
          <ellipse cx="60" cy="98" rx="28" ry="7" fill="#E2E8F0" />
          <ellipse cx="60" cy="97" rx="22" ry="5" fill="#CBD5E1" />

          <!-- Sprout Seed Base -->
          <path d="M52 94C52 89 55 86 60 86C65 86 68 89 68 94C68 97.5 64.5 99 60 99C55.5 99 52 97.5 52 94Z" fill="#155A37" />

          <!-- Main Stem -->
          <g class="mascot-stem">
            <path d="M60 90V46" stroke="#155A37" stroke-width="4.5" stroke-linecap="round" />
            
            <!-- Left Primary Leaf -->
            <path class="mascot-leaf-left" d="M60 62C46 62 40 50 42 42C50 42 60 52 60 62Z" fill="#2E8B57" />
            
            <!-- Right Secondary Leaf -->
            <path class="mascot-leaf-right" d="M60 52C74 52 80 40 78 32C70 32 60 42 60 52Z" fill="#38A169" />
          </g>

          <!-- Golden Bloom (Unlocked in Milestone Stage 3) -->
          <g class="mascot-golden-flower" opacity="0" style="transform-origin: 60px 42px;">
            <circle cx="60" cy="40" r="5" fill="#EAB308" />
            <circle cx="60" cy="32" r="2.5" fill="#FACC15" />
            <circle cx="68" cy="40" r="2.5" fill="#FACC15" />
            <circle cx="60" cy="48" r="2.5" fill="#FACC15" />
            <circle cx="52" cy="40" r="2.5" fill="#FACC15" />
            <circle cx="60" cy="40" r="2" fill="#CA8A04" />
          </g>
        </svg>
      </div>
    `;
  },

  setState(containerSelector, state) {
    const containers = document.querySelectorAll(containerSelector);
    containers.forEach(container => {
      container.classList.remove("mascot-state-idle", "mascot-state-growing", "mascot-state-milestone");
      container.classList.add(`mascot-state-${state}`);
      container.setAttribute("data-mascot-state", state);
    });
  }
};

window.SproutMascot = SproutMascot;
