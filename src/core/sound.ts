/**
 * @license
 * SPDX-License-Identifier: Apache-2.0
 */

interface ToneOpts {
  freq: number;
  type?: OscillatorType;
  start?: number;
  dur?: number;
  gain?: number;
  attack?: number;
  glideTo?: number;
  glideType?: "exp" | "lin";
  filterFreq?: number;
}

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

  /** Synthesized tone with envelope, optional glide and lowpass. */
  private tone(dest: AudioNode, o: ToneOpts) {
    if (!this.ctx) return;
    const now = this.ctx.currentTime;
    const t0 = now + (o.start ?? 0);
    const dur = o.dur ?? 0.15;
    const osc = this.ctx.createOscillator();
    const gain = this.ctx.createGain();

    osc.type = o.type ?? "triangle";
    osc.frequency.setValueAtTime(o.freq, t0);
    if (o.glideTo !== undefined) {
      if ((o.glideType ?? "exp") === "exp" && o.glideTo > 0) {
        osc.frequency.exponentialRampToValueAtTime(o.glideTo, t0 + dur);
      } else {
        osc.frequency.linearRampToValueAtTime(o.glideTo, t0 + dur);
      }
    }

    const peak = o.gain ?? 0.08;
    const atk = o.attack ?? 0.004;
    gain.gain.setValueAtTime(0.0001, t0);
    gain.gain.linearRampToValueAtTime(peak, t0 + atk);
    gain.gain.exponentialRampToValueAtTime(0.001, t0 + dur);

    let node: AudioNode = gain;
    if (o.filterFreq) {
      const lp = this.ctx.createBiquadFilter();
      lp.type = "lowpass";
      lp.frequency.value = o.filterFreq;
      gain.connect(lp);
      node = lp;
    }

    osc.connect(gain);
    node.connect(dest);

    osc.start(t0);
    osc.stop(t0 + dur + 0.02);
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

  /** Soft percussive impact — sine drop + dark noise tail. */
  private impact(dest: AudioNode, opts: { start?: number; freq?: number; dur?: number; gain?: number }) {
    const f = opts.freq ?? 120;
    const dur = opts.dur ?? 0.22;
    this.tone(dest, { freq: f * 1.6, glideTo: f * 0.55, start: opts.start, dur, gain: opts.gain ?? 0.14, type: "sine" });
    this.noise(dest, { start: opts.start, dur: dur * 0.6, gain: (opts.gain ?? 0.14) * 0.35, filterType: "lowpass", freqFrom: 900, freqTo: 200 });
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

  /** Clean electronic UI click — tight blip + micro transient. */
  playClick() {
    if (!this.soundEnabled) return;
    try {
      const dest = this.out();
      if (!this.ctx || !dest) return;
      this.tone(dest, { freq: 1900, glideTo: 950, dur: 0.045, gain: 0.05, type: "sine" });
      this.noise(dest, { dur: 0.02, gain: 0.03, filterType: "highpass", freqFrom: 4000 });
    } catch (e) {
      console.warn("Audio failed to play:", e);
    }
  }

  /** Crystalline two-note confirmation for primary actions. */
  playConfirm() {
    if (!this.soundEnabled) return;
    try {
      const dest = this.out();
      if (!this.ctx || !dest) return;
      this.tone(dest, { freq: 1318.51, dur: 0.12, gain: 0.07, type: "triangle" }); // E6
      this.tone(dest, { freq: 1567.98, start: 0.07, dur: 0.2, gain: 0.07, type: "triangle" }); // G6
      this.tone(dest, { freq: 3135.96, start: 0.07, dur: 0.18, gain: 0.02, type: "sine" }); // shimmer
    } catch (e) {
      console.warn("Audio failed to play:", e);
    }
  }

  /** Bright satisfying ding for correct answers. */
  playCorrect() {
    if (!this.soundEnabled) return;
    try {
      const dest = this.out();
      if (!this.ctx || !dest) return;
      this.tone(dest, { freq: 880, dur: 0.28, gain: 0.09, type: "triangle" }); // A5
      this.tone(dest, { freq: 1760, dur: 0.2, gain: 0.04, type: "sine" }); // octave partial
      this.noise(dest, { dur: 0.09, gain: 0.025, filterType: "highpass", freqFrom: 5200 }); // sparkle
    } catch (e) {
      console.warn("Audio failed to play:", e);
    }
  }

  /** Short elegant buzz — filtered saw fall + sub thump. */
  playIncorrect() {
    if (!this.soundEnabled) return;
    try {
      const dest = this.out();
      if (!this.ctx || !dest) return;
      this.tone(dest, { freq: 170, glideTo: 95, dur: 0.19, gain: 0.07, type: "sawtooth", filterFreq: 850 });
      this.impact(dest, { freq: 90, dur: 0.16, gain: 0.08 });
    } catch (e) {
      console.warn("Audio failed to play:", e);
    }
  }

  /** Rising whoosh with light impact — scales with combo level. */
  playCombo(level: number) {
    if (!this.soundEnabled) return;
    try {
      const dest = this.out();
      if (!this.ctx || !dest) return;
      const lift = Math.min(level - 1, 5); // cap the pitch climb
      this.noise(dest, { dur: 0.24, gain: 0.06, filterType: "bandpass", freqFrom: 350, freqTo: 2400 + lift * 250, q: 1.4 });
      this.tone(dest, { freq: 330 * Math.pow(2, lift / 12), glideTo: 660 * Math.pow(2, lift / 12), dur: 0.22, gain: 0.05, type: "triangle" });
      this.impact(dest, { start: 0.2, freq: 150, dur: 0.16, gain: 0.09 });
    } catch (e) {
      console.warn("Audio failed to play:", e);
    }
  }

  /** Clean digital tick for the final countdown. */
  playTick(high: boolean = false) {
    if (!this.soundEnabled) return;
    try {
      const dest = this.out();
      if (!this.ctx || !dest) return;
      this.tone(dest, { freq: high ? 1500 : 1150, dur: 0.035, gain: high ? 0.06 : 0.045, type: "square", filterFreq: 3200 });
    } catch (e) {
      console.warn("Audio failed to play:", e);
    }
  }

  /** Short premium fanfare for a brand-new record. */
  playRecord() {
    if (!this.soundEnabled) return;
    try {
      const dest = this.out();
      if (!this.ctx || !dest) return;
      const notes = [523.25, 659.25, 783.99, 1046.5]; // C5 E5 G5 C6
      notes.forEach((freq, idx) => {
        this.tone(dest, { freq, start: idx * 0.07, dur: 0.16, gain: 0.075, type: "triangle" });
        this.tone(dest, { freq: freq * 2, start: idx * 0.07, dur: 0.12, gain: 0.02, type: "sine" });
      });
      // Final bright chord + shimmer
      [1046.5, 1318.51, 1567.98].forEach((freq) => {
        this.tone(dest, { freq, start: 0.3, dur: 0.35, gain: 0.05, type: "triangle" });
      });
      this.noise(dest, { start: 0.3, dur: 0.25, gain: 0.02, filterType: "highpass", freqFrom: 6000 });
    } catch (e) {
      console.warn("Audio failed to play:", e);
    }
  }

  /** Metallic shine for trophy unlocks — inharmonic partials. */
  playTrophy() {
    if (!this.soundEnabled) return;
    try {
      const dest = this.out();
      if (!this.ctx || !dest) return;
      this.tone(dest, { freq: 2093, dur: 0.4, gain: 0.06, type: "sine" });
      this.tone(dest, { freq: 2637 * 1.007, dur: 0.34, gain: 0.035, type: "sine" });
      this.tone(dest, { freq: 3520 * 0.996, dur: 0.28, gain: 0.02, type: "sine" });
      this.noise(dest, { dur: 0.12, gain: 0.03, filterType: "highpass", freqFrom: 7000 });
    } catch (e) {
      console.warn("Audio failed to play:", e);
    }
  }

  /** Smooth fall + elegant impact — composed, not sad or exaggerated. */
  playGameOver() {
    if (!this.soundEnabled) return;
    try {
      const dest = this.out();
      if (!this.ctx || !dest) return;
      this.tone(dest, { freq: 540, glideTo: 262, dur: 0.38, gain: 0.08, type: "triangle", filterFreq: 1600 });
      this.tone(dest, { freq: 270, glideTo: 135, dur: 0.38, gain: 0.04, type: "sine" });
      this.impact(dest, { start: 0.32, freq: 110, dur: 0.26, gain: 0.11 });
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
