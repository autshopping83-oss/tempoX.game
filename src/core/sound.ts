/**
 * @license
 * SPDX-License-Identifier: Apache-2.0
 */

class SoundEngine {
  private ctx: AudioContext | null = null;
  private masterGain: GainNode | null = null;
  private soundEnabled: boolean = true;
  private vibrationEnabled: boolean = true;
  private volume: number = SoundEngine.loadVolume();

  /** Reads the persisted master volume, clamped to [0..1]. */
  private static loadVolume(): number {
    if (typeof window === "undefined") return 0.8;
    const raw = parseFloat(localStorage.getItem("60s_volume") ?? "");
    return Number.isFinite(raw) ? Math.min(1, Math.max(0, raw)) : 0.8;
  }

  constructor() {
    // Load preferences from localStorage if available
    if (typeof window !== "undefined") {
      const savedSound = localStorage.getItem("60s_sound");
      this.soundEnabled = savedSound !== "false";
      const savedVibe = localStorage.getItem("60s_vibe");
      this.vibrationEnabled = savedVibe !== "false";
    }
  }

  private initContext() {
    if (!this.ctx && typeof window !== "undefined") {
      try {
        const AudioCtx = window.AudioContext || (window as any).webkitAudioContext;
        if (AudioCtx) {
          this.ctx = new AudioCtx();
          this.masterGain = this.ctx.createGain();
          this.masterGain.gain.value = this.volume;
          this.masterGain.connect(this.ctx.destination);
        }
      } catch (e) {
        console.warn("Web Audio API is not supported:", e);
      }
    }
    if (this.ctx && this.ctx.state === "suspended") {
      this.ctx.resume().catch(() => {});
    }
  }

  private out(): AudioNode | null {
    this.initContext();
    return this.masterGain;
  }

  setSoundEnabled(enabled: boolean) {
    this.soundEnabled = enabled;
    localStorage.setItem("60s_sound", enabled ? "true" : "false");
  }

  setVibrationEnabled(enabled: boolean) {
    this.vibrationEnabled = enabled;
    localStorage.setItem("60s_vibe", enabled ? "true" : "false");
  }

  /** Master SFX volume [0..1] — persisted, mirrors native SoundManager. */
  setVolume(value: number) {
    this.volume = Math.min(1, Math.max(0, value));
    localStorage.setItem("60s_volume", String(this.volume));
    if (this.masterGain && this.ctx) {
      this.masterGain.gain.setTargetAtTime(this.volume, this.ctx.currentTime, 0.01);
    }
  }

  getVolume(): number {
    return this.volume;
  }

  getSoundEnabled(): boolean {
    return this.soundEnabled;
  }

  getVibrationEnabled(): boolean {
    return this.vibrationEnabled;
  }

  playCorrect() {
    if (!this.soundEnabled) return;
    try {
      const dest = this.out();
      if (!this.ctx || !dest) return;
      const now = this.ctx.currentTime;

      // Play a quick ascending major chord (C5 -> E5 -> G5)
      const playTone = (freq: number, start: number, duration: number) => {
        if (!this.ctx) return;
        const osc = this.ctx.createOscillator();
        const gain = this.ctx.createGain();

        osc.type = "triangle";
        osc.frequency.setValueAtTime(freq, start);

        gain.gain.setValueAtTime(0.08, start);
        gain.gain.exponentialRampToValueAtTime(0.001, start + duration);

        osc.connect(gain);
        gain.connect(dest);

        osc.start(start);
        osc.stop(start + duration);
      };

      playTone(523.25, now, 0.08); // C5
      playTone(659.25, now + 0.04, 0.08); // E5
      playTone(783.99, now + 0.08, 0.15); // G5
    } catch (e) {
      console.warn("Audio failed to play:", e);
    }
  }

  playIncorrect() {
    if (!this.soundEnabled) return;
    try {
      const dest = this.out();
      if (!this.ctx || !dest) return;
      const now = this.ctx.currentTime;

      const osc = this.ctx.createOscillator();
      const gain = this.ctx.createGain();

      osc.type = "sawtooth";
      osc.frequency.setValueAtTime(140, now);
      osc.frequency.linearRampToValueAtTime(70, now + 0.25);

      gain.gain.setValueAtTime(0.1, now);
      gain.gain.exponentialRampToValueAtTime(0.001, now + 0.25);

      osc.connect(gain);
      gain.connect(dest);

      osc.start(now);
      osc.stop(now + 0.25);
    } catch (e) {
      console.warn("Audio failed to play:", e);
    }
  }

  playCombo(level: number) {
    if (!this.soundEnabled) return;
    try {
      const dest = this.out();
      if (!this.ctx || !dest) return;
      const now = this.ctx.currentTime;

      // Higher combo -> higher pitch blips
      const freq = Math.min(440 + level * 50, 1200);
      const osc = this.ctx.createOscillator();
      const gain = this.ctx.createGain();

      osc.type = "sine";
      osc.frequency.setValueAtTime(freq, now);
      osc.frequency.setValueAtTime(freq * 1.25, now + 0.05);

      gain.gain.setValueAtTime(0.06, now);
      gain.gain.exponentialRampToValueAtTime(0.001, now + 0.12);

      osc.connect(gain);
      gain.connect(dest);

      osc.start(now);
      osc.stop(now + 0.12);
    } catch (e) {
      console.warn("Audio failed to play:", e);
    }
  }

  playRecord() {
    if (!this.soundEnabled) return;
    try {
      const dest = this.out();
      if (!this.ctx || !dest) return;
      const now = this.ctx.currentTime;

      // Triumph arpeggio melody
      const notes = [440.00, 554.37, 659.25, 880.00, 1108.73, 1318.51, 1760.00];
      const step = 0.06;
      notes.forEach((freq, idx) => {
        if (!this.ctx || !dest) return;
        const osc = this.ctx.createOscillator();
        const gain = this.ctx.createGain();

        osc.type = "triangle";
        osc.frequency.setValueAtTime(freq, now + idx * step);

        gain.gain.setValueAtTime(0.06, now + idx * step);
        gain.gain.exponentialRampToValueAtTime(0.001, now + idx * step + 0.15);

        osc.connect(gain);
        gain.connect(dest);

        osc.start(now + idx * step);
        osc.stop(now + idx * step + 0.15);
      });
    } catch (e) {
      console.warn("Audio failed to play:", e);
    }
  }

  playTick(high: boolean = false) {
    if (!this.soundEnabled) return;
    try {
      const dest = this.out();
      if (!this.ctx || !dest) return;
      const now = this.ctx.currentTime;

      const osc = this.ctx.createOscillator();
      const gain = this.ctx.createGain();

      osc.type = "sine";
      osc.frequency.setValueAtTime(high ? 900 : 700, now);

      gain.gain.setValueAtTime(0.04, now);
      gain.gain.exponentialRampToValueAtTime(0.001, now + 0.04);

      osc.connect(gain);
      gain.connect(dest);

      osc.start(now);
      osc.stop(now + 0.04);
    } catch (e) {
      console.warn("Audio failed to play:", e);
    }
  }

  vibrate(duration: number | number[]) {
    if (!this.vibrationEnabled) return;
    if (typeof navigator !== "undefined" && navigator.vibrate) {
      try {
        navigator.vibrate(duration);
      } catch (e) {
        // Soft fail
      }
    }
  }
}

export const sound = new SoundEngine();
