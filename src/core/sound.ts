/**
 * @license
 * SPDX-License-Identifier: Apache-2.0
 */

interface NoiseOpts {
  start?: number;
  dur?: number;
  gain?: number;
  attack?: number;
  filterType?: BiquadFilterType;
  freqFrom?: number;
  freqTo?: number;
  q?: number;
}

class SoundEngine {
  private ctx: AudioContext | null = null;
  private masterGain: GainNode | null = null;
  private noiseBuffer: AudioBuffer | null = null;
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

          // Cached 1s white-noise buffer shared by all noise layers
          const len = this.ctx.sampleRate;
          this.noiseBuffer = this.ctx.createBuffer(1, len, this.ctx.sampleRate);
          const data = this.noiseBuffer.getChannelData(0);
          for (let i = 0; i < len; i++) data[i] = Math.random() * 2 - 1;
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

  /** Filtered noise burst — whooshes, sparkles, thud tails. */
  private noise(dest: AudioNode, o: NoiseOpts) {
    if (!this.ctx || !this.noiseBuffer) return;
    const now = this.ctx.currentTime;
    const t0 = now + (o.start ?? 0);
    const dur = o.dur ?? 0.1;

    const src = this.ctx.createBufferSource();
    src.buffer = this.noiseBuffer;
    src.loop = true;

    const filter = this.ctx.createBiquadFilter();
    filter.type = o.filterType ?? "bandpass";
    filter.Q.value = o.q ?? 1;
    const f0 = o.freqFrom ?? 1000;
    filter.frequency.setValueAtTime(f0, t0);
    if (o.freqTo !== undefined && o.freqTo > 0) {
      filter.frequency.exponentialRampToValueAtTime(o.freqTo, t0 + dur);
    }

    const gain = this.ctx.createGain();
    const peak = o.gain ?? 0.05;
    const atk = o.attack ?? 0.01;
    gain.gain.setValueAtTime(0.0001, t0);
    gain.gain.linearRampToValueAtTime(peak, t0 + atk);
    gain.gain.exponentialRampToValueAtTime(0.001, t0 + dur);

    src.connect(filter);
    filter.connect(gain);
    gain.connect(dest);

    src.start(t0);
    src.stop(t0 + dur + 0.02);
  }

  /** Synthesized marimba bar: fundamental + 4.05x signature partial + woody knock. */
  private marimba(dest: AudioNode, freq: number, dur: number, gain: number, start = 0) {
    if (!this.ctx) return;
    const t0 = this.ctx.currentTime + start;
    const detune = 1 + (Math.random() - 0.5) * 0.004; // organic micro-drift
    this.partial(dest, freq * detune, dur, gain, t0);
    this.partial(dest, freq * 4.05, dur * 0.32, gain * 0.34, t0);
    this.partial(dest, freq * 9.2, dur * 0.1, gain * 0.1, t0);
    this.noise(dest, { start, dur: 0.006, gain: gain * 0.22, filterType: "lowpass", freqFrom: 1500 });
  }

  /** Single decaying sine with fast attack. */
  private partial(dest: AudioNode, freq: number, dur: number, gain: number, t0: number) {
    if (!this.ctx) return;
    const osc = this.ctx.createOscillator();
    const g = this.ctx.createGain();
    osc.type = "sine";
    osc.frequency.setValueAtTime(freq, t0);
    g.gain.setValueAtTime(0.0001, t0);
    g.gain.linearRampToValueAtTime(gain, t0 + 0.0015);
    g.gain.exponentialRampToValueAtTime(0.001, t0 + dur);
    osc.connect(g);
    g.connect(dest);
    osc.start(t0);
    osc.stop(t0 + dur + 0.02);
  }

  /** Warm soft mallet for gentle negative feedback. */
  private softMallet(dest: AudioNode, freq: number, dur: number, gain: number, start = 0) {
    this.partial(dest, freq, dur, gain, this.ctx!.currentTime + start);
    this.partial(dest, freq * 2.01, dur * 0.4, gain * 0.14, this.ctx!.currentTime + start);
    this.noise(dest, { start, dur: 0.008, gain: gain * 0.15, filterType: "lowpass", freqFrom: 700 });
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

  /** Velvety wood-block pop <50ms, ±6% pitch drift vs fatigue. */
  playClick() {
    if (!this.soundEnabled) return;
    try {
      const dest = this.out();
      if (!this.ctx || !dest) return;
      const drift = 1 + (Math.random() - 0.5) * 0.12;
      this.noise(dest, { dur: 0.02, gain: 0.13, filterType: "bandpass", freqFrom: 1750 * drift, q: 2.2 });
      this.partial(dest, 880 * drift, 0.032, 0.06, this.ctx.currentTime);
    } catch (e) {
      console.warn("Audio failed to play:", e);
    }
  }

  /** Two-note marimba affirmation for primary actions. */
  playConfirm() {
    if (!this.soundEnabled) return;
    try {
      const dest = this.out();
      if (!this.ctx || !dest) return;
      this.marimba(dest, 659.25, 0.16, 0.09); // E5
      this.marimba(dest, 880, 0.24, 0.1, 0.07); // A5
    } catch (e) {
      console.warn("Audio failed to play:", e);
    }
  }

  /** Joyful ascending marimba arpeggio — instant dopamine. */
  playCorrect() {
    if (!this.soundEnabled) return;
    try {
      const dest = this.out();
      if (!this.ctx || !dest) return;
      const seq: [number, number, number][] = [
        [523.25, 0, 0.09], [659.25, 0.07, 0.09], [783.99, 0.14, 0.1], [1046.5, 0.21, 0.12],
      ];
      seq.forEach(([f, t, g]) => this.marimba(dest, f, t === 0 ? 0.24 : t === 0.21 ? 0.34 : 0.26, g, t));
    } catch (e) {
      console.warn("Audio failed to play:", e);
    }
  }

  /** Gentle muffled descend — kind, zero punishment. */
  playIncorrect() {
    if (!this.soundEnabled) return;
    try {
      const dest = this.out();
      if (!this.ctx || !dest) return;
      this.softMallet(dest, 392, 0.13, 0.09); // G4
      this.softMallet(dest, 329.63, 0.16, 0.08, 0.085); // E4
    } catch (e) {
      console.warn("Audio failed to play:", e);
    }
  }

  /** Rising pentatonic wooden run + light knock. */
  playCombo(level: number) {
    if (!this.soundEnabled) return;
    try {
      const dest = this.out();
      if (!this.ctx || !dest) return;
      const lift = Math.min(level - 1, 5);
      const k = Math.pow(2, lift / 12);
      [[392, 0], [493.88, 0.055], [587.33, 0.11], [783.99, 0.165]].forEach(([f, t]) =>
        this.marimba(dest, f * k, 0.18, 0.09, t));
      this.noise(dest, { start: 0.235, dur: 0.014, gain: 0.09, filterType: "bandpass", freqFrom: 1500, q: 2.4 });
    } catch (e) {
      console.warn("Audio failed to play:", e);
    }
  }

  /** Rhythmic wood pulse for the final countdown. */
  playTick(high: boolean = false) {
    if (!this.soundEnabled) return;
    try {
      const dest = this.out();
      if (!this.ctx || !dest) return;
      const drift = 1 + (Math.random() - 0.5) * 0.08;
      this.noise(dest, { dur: 0.016, gain: high ? 0.13 : 0.11, filterType: "bandpass", freqFrom: 1250 * drift, q: 2.6 });
      this.partial(dest, 1050 * drift, 0.03, high ? 0.06 : 0.05, this.ctx.currentTime);
    } catch (e) {
      console.warn("Audio failed to play:", e);
    }
  }

  /** Bright marimba flourish for a brand-new record. */
  playRecord() {
    if (!this.soundEnabled) return;
    try {
      const dest = this.out();
      if (!this.ctx || !dest) return;
      [523.25, 659.25, 783.99, 1046.5].forEach((f, i) => this.marimba(dest, f, i === 3 ? 0.34 : 0.2, 0.09, i * 0.07));
      this.marimba(dest, 1318.51, 0.5, 0.1, 0.28); // E6 landing
      [523.25, 659.25, 783.99].forEach(f => this.partial(dest, f, 0.42, 0.025, this.ctx!.currentTime + 0.36));
    } catch (e) {
      console.warn("Audio failed to play:", e);
    }
  }

  /** Warm chime sparkle for trophies. */
  playTrophy() {
    if (!this.soundEnabled) return;
    try {
      const dest = this.out();
      if (!this.ctx || !dest) return;
      [[1046.5, 0, 0.4], [1318.51, 0.06, 0.36], [783.99, 0.12, 0.3]].forEach(([f, t, d]) =>
        this.marimba(dest, f, d, 0.08, t));
      this.noise(dest, { start: 0.02, dur: 0.05, gain: 0.02, filterType: "highpass", freqFrom: 6500 });
    } catch (e) {
      console.warn("Audio failed to play:", e);
    }
  }

  /** Smooth downward resolution — invites a quick retry. */
  playGameOver() {
    if (!this.soundEnabled) return;
    try {
      const dest = this.out();
      if (!this.ctx || !dest) return;
      [[523.25, 0, 0.22, 0.09], [440, 0.14, 0.24, 0.085], [349.23, 0.28, 0.3, 0.08], [261.63, 0.46, 0.55, 0.09]].forEach(
        ([f, t, d, g]) => this.marimba(dest, f as number, d as number, g as number, t as number));
      this.noise(dest, { start: 0.46, dur: 0.01, gain: 0.05, filterType: "lowpass", freqFrom: 500 });
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
