/**
 * @license
 * SPDX-License-Identifier: Apache-2.0
 */

/**
 * GameFeelManager
 *
 * Centralized arcade game-feel system: particles, floating points,
 * punch/shake/flash feedback and timer urgency tiers.
 *
 * All effects are pure UI (transform/opacity only) and never touch
 * game logic, scoring or timing.
 */

export type UrgencyTier = "calm" | "warn" | "urgent" | "critical" | "final";

interface Point {
  x: number;
  y: number;
}

const MAX_LIVE_PARTICLES = 36;
let liveParticles = 0;

const STAR_PATH =
  '<svg viewBox="0 0 24 24" width="100%" height="100%" fill="currentColor"><path d="M12 1.8l2.6 7.2 7.6.3-6 4.8 2.1 7.3L12 17l-6.3 4.4 2.1-7.3-6-4.8 7.6-.3z"/></svg>';

function spawnParticle(
  layer: HTMLElement | null,
  at: Point,
  color: string,
  kind: "dot" | "square" | "star"
) {
  if (!layer || liveParticles >= MAX_LIVE_PARTICLES) return;
  const rect = layer.getBoundingClientRect();
  const p = document.createElement("span");
  p.className = "gf-particle";
  const size = kind === "star" ? 10 + Math.random() * 8 : 6 + Math.random() * 7;
  const angle = Math.random() * Math.PI * 2;
  const dist = 30 + Math.random() * 48;

  p.style.left = `${at.x - rect.left}px`;
  p.style.top = `${at.y - rect.top}px`;
  p.style.width = `${size}px`;
  p.style.height = `${size}px`;
  p.style.setProperty("--dx", `${Math.cos(angle) * dist}px`);
  p.style.setProperty("--dy", `${Math.sin(angle) * dist - 26}px`);
  p.style.setProperty("--rot", `${Math.round(Math.random() * 220 - 110)}deg`);

  if (kind === "star") {
    p.style.color = color;
    p.innerHTML = STAR_PATH;
  } else {
    p.style.background = color;
    p.style.borderRadius = kind === "dot" ? "999px" : "3px";
    if (kind === "square") p.style.rotate = `${Math.round(Math.random() * 90)}deg`;
  }

  layer.appendChild(p);
  liveParticles++;
  window.setTimeout(() => {
    p.remove();
    liveParticles--;
  }, 760);
}

class GameFeelManager {
  /** Short physical press feedback: 1 -> .96 -> 1.03 -> 1 */
  buttonPress(el?: HTMLElement | null) {
    if (!el) return;
    el.classList.remove("gf-punch");
    void el.offsetWidth;
    el.classList.add("gf-punch");
    window.setTimeout(() => el.classList.remove("gf-punch"), 300);
  }

  shake(el?: HTMLElement | null) {
    if (!el) return;
    el.classList.remove("gf-shake");
    void el.offsetWidth;
    el.classList.add("gf-shake");
    window.setTimeout(() => el.classList.remove("gf-shake"), 440);
  }

  /** Release punch alias of buttonPress */
  punch(el?: HTMLElement | null) {
    this.buttonPress(el);
  }

  flash(el?: HTMLElement | null, tone: "red" | "green" = "red") {
    if (!el) return;
    const cls = tone === "red" ? "gf-flash-red" : "gf-flash-green";
    el.classList.remove("gf-flash-red", "gf-flash-green");
    void el.offsetWidth;
    el.classList.add(cls);
    window.setTimeout(() => el.classList.remove(cls), 480);
  }

  burst(layer: HTMLElement | null, at: Point, colors: string[], count = 10) {
    if (!layer) return;
    for (let i = 0; i < count; i++) {
      const roll = Math.random();
      const kind = roll < 0.55 ? "dot" : roll < 0.85 ? "square" : "star";
      spawnParticle(layer, at, colors[i % colors.length], kind);
    }
  }

  floatPoints(layer: HTMLElement | null, at: Point, text: string, color = "#22C55E") {
    if (!layer) return;
    const rect = layer.getBoundingClientRect();
    const el = document.createElement("span");
    el.className = "gf-float-points";
    el.textContent = text;
    el.style.left = `${at.x - rect.left}px`;
    el.style.top = `${at.y - rect.top}px`;
    el.style.color = color;
    el.style.textShadow = `0 2px 10px ${color}55, 0 0 2px #ffffff`;
    layer.appendChild(el);
    window.setTimeout(() => el.remove(), 850);
  }

  /** Hit celebration: particles + rising "+N" near the tap position */
  successFX(layer: HTMLElement | null, at: Point, pointsEarned?: number) {
    this.burst(
      layer,
      at,
      ["#22C55E", "#6D3DF5", "#EC4899", "#FACC15"],
      12
    );
    if (typeof pointsEarned === "number" && pointsEarned > 0) {
      this.floatPoints(layer, { x: at.x, y: at.y - 14 }, `+${pointsEarned}`, "#16A34A");
    }
  }

  /** Miss feedback: horizontal shake + short red flash on the challenge card */
  failureFX(cardEl?: HTMLElement | null, layer?: HTMLElement | null, at?: Point) {
    this.shake(cardEl);
    this.flash(cardEl, "red");
    if (layer && at) {
      this.burst(layer, at, ["#EF4444", "#F87171"], 8);
    }
  }

  /** Combo reward: small star burst (used on combo milestones) */
  comboFX(layer: HTMLElement | null, at: Point) {
    this.burst(layer, at, ["#EC4899", "#FACC15", "#F59E0B", "#6D3DF5"], 9);
  }

  urgency(secondsLeft: number): UrgencyTier {
    if (secondsLeft <= 3) return "final";
    if (secondsLeft <= 5) return "critical";
    if (secondsLeft <= 10) return "urgent";
    if (secondsLeft <= 20) return "warn";
    return "calm";
  }
}

export const gameFeel = new GameFeelManager();
