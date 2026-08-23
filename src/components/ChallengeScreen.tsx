/**
 * @license
 * SPDX-License-Identifier: Apache-2.0
 */

import React, { useEffect, useRef, useState } from "react";
import { ChallengeInstance } from "../challenge/types";
import MemoryChallenge from "../challenge/challenges/MemoryChallenge";
import ReflexChallenge from "../challenge/challenges/ReflexChallenge";
import MathChallenge from "../challenge/challenges/MathChallenge";
import AttentionChallenge from "../challenge/challenges/AttentionChallenge";
import { Pause, Play, Volume2, VolumeX, Smartphone, RefreshCw, X } from "lucide-react";
import { motion, AnimatePresence } from "motion/react";
import FloatingBackgroundShapes from "./FloatingBackgroundShapes";
import FloatingCard from "./ui/FloatingCard";
import { ShapeGradients } from "./GeometricShapes";
import { GameTheme, GameColors } from "../core/GameTheme";
import { gameFeel } from "../core/gameFeel";
import { useI18n } from "../core/i18n";
import { sound } from "../core/sound";
import GameTimer from "./GameTimer";
import AnimatedScore from "./AnimatedScore";
import ProgressBar from "./ProgressBar";

interface Props {
  challenge: ChallengeInstance | null;
  score: number;
  combo: number;
  gameTimeLeft: number;
  challengeTimeLeft: number;
  difficultyLevel: number;
  soundOn: boolean;
  vibeOn: boolean;
  gameState: "PLAYING" | "PAUSED";
  onPause: () => void;
  onResume: () => void;
  onQuit: () => void;
  onSolveChallenge: (success: boolean, detailValue?: any) => void;
  onToggleSound: () => void;
  onToggleVibration: () => void;
  onRestart: () => void;
  /** Freeze/unfreeze the per-challenge countdown (Memory watch phase). */
  onWatchPhaseChange?: (paused: boolean) => void;
}

export default function ChallengeScreen({
  challenge,
  score,
  combo,
  gameTimeLeft,
  challengeTimeLeft,
  difficultyLevel,
  soundOn,
  vibeOn,
  gameState,
  onPause,
  onResume,
  onQuit,
  onSolveChallenge,
  onToggleSound,
  onToggleVibration,
  onRestart,
  onWatchPhaseChange,
}: Props) {

  // Game feel FX tracking
  const fxLayerRef = useRef<HTMLDivElement | null>(null);
  const cardRef = useRef<HTMLDivElement | null>(null);
  const comboZoneRef = useRef<HTMLDivElement | null>(null);
  const lastPointerRef = useRef({ x: window.innerWidth / 2, y: window.innerHeight * 0.6 });
  const prevScoreRef = useRef(score);
  const prevComboRef = useRef(combo);
  const { t } = useI18n();
  const [fxVolume, setFxVolume] = useState(() => sound.getVolume());

  const handleVolumeChange = (value: number) => {
    setFxVolume(value);
    sound.setVolume(value);
  };

  useEffect(() => {
    const onDown = (e: PointerEvent) => {
      lastPointerRef.current = { x: e.clientX, y: e.clientY };
    };
    window.addEventListener("pointerdown", onDown, { passive: true });
    return () => window.removeEventListener("pointerdown", onDown);
  }, []);

  // Hit celebration: particles + "+N" rising near the tap position
  useEffect(() => {
    const diff = score - prevScoreRef.current;
    if (diff > 0) {
      gameFeel.successFX(fxLayerRef.current, lastPointerRef.current, diff);
      prevScoreRef.current = score;
    } else if (score < prevScoreRef.current) {
      prevScoreRef.current = score;
    }
  }, [score]);

  // Combo reward stars + miss detection (combo broken)
  useEffect(() => {
    if (combo > prevComboRef.current && combo >= 3 && combo % 3 === 0) {
      const zone = comboZoneRef.current;
      if (zone) {
        const r = zone.getBoundingClientRect();
        gameFeel.comboFX(fxLayerRef.current, { x: r.left + r.width / 2, y: r.top + r.height / 2 });
      }
    }
    if (combo === 0 && prevComboRef.current >= 2) {
      gameFeel.failureFX(cardRef.current);
    }
    prevComboRef.current = combo;
  }, [combo]);

  if (!challenge) {
    return (
      <div className={`flex items-center justify-center h-full text-slate-400 ${GameTheme.colors.background}`}>
        <div className={`animate-spin rounded-full h-8 w-8 border-t-2 ${GameTheme.colors.primary.border}`} />
      </div>
    );
  }

  // Active challenge identity: badge color + BIG instantly-readable instruction
  const getChallengeMetadata = (): { color: string; name: string; instruction: string } => {
    switch (challenge.type) {
      case "MEMORY":
        return { color: GameColors.pink, name: t("challenge_memory_name"), instruction: t("challenge_memory_instruction") };
      case "REFLEX":
        return { color: GameColors.orange, name: t("challenge_reflex_name"), instruction: t("challenge_reflex_instruction") };
      case "MATH":
        return { color: GameColors.blue, name: t("challenge_math_name"), instruction: t("challenge_math_instruction") };
      case "ATTENTION":
        return {
          color: GameColors.green,
          name: t("challenge_attention_name"),
          instruction: t("challenge_attention_instruction"),
        };
      default:
        return { color: GameColors.primary, name: t("nav_rules"), instruction: t("challenge_attention_instruction") };
    }
  };

  const metadata = getChallengeMetadata();

  // Individual challenge bar timer percentage
  const challengeTimePct = Math.min(
    100,
    Math.max(0, (challengeTimeLeft / challenge.duration) * 100)
  );

  return (
    <div className={`relative flex flex-col flex-grow justify-between ${GameTheme.spacing.outerPadding} max-w-md mx-auto w-full h-full text-slate-800 ${GameTheme.colors.background} overflow-hidden select-none pt-safe pb-safe`}>
      {/* Living decorative background behind everything */}
      <FloatingBackgroundShapes />
      <ShapeGradients />

      {/* FX layer: particles and floating points */}
      <div ref={fxLayerRef} className="absolute inset-0 z-40 pointer-events-none overflow-hidden" />

      <div className="relative z-10 flex flex-col flex-grow min-h-0">

        {/* ===== HUD SUPERIOR: [PAUSE] [TIMER] [SCORE] ===== */}
        <div className="grid grid-cols-3 items-start gap-2 shrink-0">
          <div className="flex justify-start">
            <button
              onClick={onPause}
              title="Pausar Partida"
              className="hud-chip w-12 h-12 rounded-2xl flex items-center justify-center cursor-pointer active:scale-90 transition-transform"
              style={{ boxShadow: "0 10px 22px -8px rgba(109,61,245,0.35)" }}
            >
              <Pause className="w-5 h-5 text-slate-600 fill-slate-500" />
            </button>
          </div>

          <div className="flex justify-center">
            <GameTimer secondsLeft={gameTimeLeft} />
          </div>

          <div className="flex justify-end">
            <div
              data-score-chip
              className="hud-chip rounded-2xl px-3 py-1.5 flex flex-col items-end min-w-[76px]"
              style={{ boxShadow: "0 10px 22px -8px rgba(109,61,245,0.25)" }}
            >
              <span className="text-[8px] font-black tracking-[0.2em] text-slate-400 uppercase">{t("hud_points")}</span>
              <AnimatedScore value={score} className="font-mono font-black text-lg text-slate-900 leading-none mt-0.5" />
            </div>
          </div>
        </div>

        {/* ===== COMBO ZONE ===== */}
        <div ref={comboZoneRef} className="h-9 shrink-0 flex items-center justify-center relative mt-1.5">
          <AnimatePresence mode="wait">
            {combo >= 2 && (
              <motion.div
                key={`combo-${combo}`}
                initial={{ scale: 0.4, opacity: 0 }}
                animate={{ scale: [1.25, 1], opacity: 1 }}
                exit={{ scale: 0.5, opacity: 0 }}
                transition={{ duration: 0.25, ease: "easeOut" }}
                style={{ boxShadow: "0 6px 18px -4px rgba(236,72,153,0.55)" }}
                className="flex items-center gap-1.5 bg-gradient-to-r from-[#EC4899] to-[#EF4444] text-white px-3 py-1 rounded-full text-xs font-black select-none pointer-events-none"
              >
                <span>🔥</span>
                <span>{t("hud_combo_chip", [combo])}</span>
              </motion.div>
            )}
          </AnimatePresence>
        </div>

        {/* ===== MAIN CHALLENGE FLOATING CARD ===== */}
        <div ref={cardRef} data-challenge-card className="relative flex-grow min-h-0 mt-1">
          <div
            className="glass-card challenge-float w-full h-full rounded-[28px] p-4 pt-3 pb-3 flex flex-col"
            style={{
              boxShadow: `0 24px 48px -18px ${metadata.color}45, 0 8px 24px -12px rgba(15,23,42,0.10), inset 0 1px 0 rgba(255,255,255,0.9)`,
            }}
          >
            {/* Header: colored badge + BIG instruction */}
            <div className="shrink-0 flex flex-col items-center gap-2 mb-2">
              <span
                style={{ backgroundColor: metadata.color, boxShadow: `0 4px 16px -2px ${metadata.color}66` }}
                className="inline-flex items-center justify-center text-white text-xs font-black uppercase tracking-[0.22em] px-4 py-1.5 rounded-full"
              >
                {metadata.name}
              </span>
              <h2
                data-instruction
                style={{ fontSize: "clamp(17px, 4.6vw, 22px)" }}
                className="font-black uppercase leading-tight tracking-tight text-slate-800 text-center px-1"
              >
                {metadata.instruction}
              </h2>
            </div>

            {/* Challenge playground */}
            <div className="flex-grow min-h-0 relative">
              <AnimatePresence mode="wait">
                <motion.div
                  key={challenge.id}
                  initial={{ opacity: 0, scale: 0.97, y: 6 }}
                  animate={{ opacity: 1, scale: 1, y: 0 }}
                  exit={{ opacity: 0, scale: 0.98 }}
                  transition={{ duration: 0.15 }}
                  className="w-full h-full flex flex-col justify-center"
                >
                  {challenge.type === "MEMORY" && (
                    <MemoryChallenge
                      challenge={challenge}
                      onSolve={(success, seqLen) => onSolveChallenge(success, seqLen)}
                      onWatchPhaseChange={onWatchPhaseChange}
                    />
                  )}
                  {challenge.type === "REFLEX" && (
                    <ReflexChallenge
                      challenge={challenge}
                      onSolve={(success) => onSolveChallenge(success)}
                    />
                  )}
                  {challenge.type === "MATH" && (
                    <MathChallenge
                      challenge={challenge}
                      onSolve={(success) => onSolveChallenge(success)}
                    />
                  )}
                  {challenge.type === "ATTENTION" && (
                    <AttentionChallenge
                      challenge={challenge}
                      onSolve={(success) => onSolveChallenge(success)}
                    />
                  )}
                </motion.div>
              </AnimatePresence>
            </div>

            {/* Thick arcade progress bar */}
            <ProgressBar pct={challengeTimePct} className="mt-3 shrink-0" />
          </div>
        </div>

      </div>

      {/* 5. MINIMALIST FULLSCREEN PAUSE MENU OVERLAY */}
      <AnimatePresence>
        {gameState === "PAUSED" && (
          <motion.div
            initial={{ opacity: 0 }}
            animate={{ opacity: 1 }}
            exit={{ opacity: 0 }}
            className={`absolute inset-0 bg-[#F8FAFC]/98 backdrop-blur-md z-50 flex flex-col justify-between pt-safe pb-safe pause-overlay text-slate-800`}
          >
            {/* Header pause status */}
            <div className="text-center shrink-0 pause-header">
              <span className={`text-xs uppercase tracking-[0.25em] ${GameTheme.colors.primary.text} ${GameTheme.colors.primary.lightBg} px-3.5 py-1 rounded-full font-black border ${GameTheme.colors.primary.borderLight}`}>
                {t("pause_badge")}
              </span>
              <h1 className="pause-title font-black tracking-tight text-slate-900 leading-none">
                {t("pause_heading")}
              </h1>
              <p className="text-xs text-slate-400 pause-subtitle font-bold uppercase tracking-wider">
                {t("pause_subtitle")}
              </p>
            </div>

            {/* Middle pause panel */}
            <div className="max-w-xs w-full mx-auto flex flex-col shrink-0 pause-panel">

              {/* Settings Card */}
              <div className={`flex flex-col pause-card bg-white/85 backdrop-blur-md border border-white/70 rounded-3xl shadow-premium overflow-hidden`}>
                <div className="flex justify-between items-center">
                  <span className="text-xs text-slate-500 font-extrabold uppercase tracking-wide">{t("settings_sound")}</span>
                  <button
                    onClick={onToggleSound}
                    className={`p-2.5 rounded-full transition-all cursor-pointer ${
                      soundOn
                        ? `${GameTheme.colors.primary.lightBg} ${GameTheme.colors.primary.text} border ${GameTheme.colors.primary.borderLight}`
                        : "bg-slate-50 text-slate-400 border border-slate-100"
                    }`}
                  >
                    {soundOn ? <Volume2 className="w-4 h-4" /> : <VolumeX className="w-4 h-4" />}
                  </button>
                </div>

                <div className="flex justify-between items-center border-t border-slate-100 pt-4">
                  <span className="text-xs text-slate-500 font-extrabold uppercase tracking-wide">{t("settings_vibration")}</span>
                  <button
                    onClick={onToggleVibration}
                    className={`p-2.5 rounded-full transition-all cursor-pointer ${
                      vibeOn
                        ? `${GameTheme.colors.success.lightBg} ${GameTheme.colors.success.text} border ${GameTheme.colors.success.border}`
                        : "bg-slate-50 text-slate-400 border border-slate-100"
                    }`}
                  >
                    <Smartphone className="w-4 h-4" />
                  </button>
                </div>

                {/* Master SFX volume — persisted, mirrors the native pause menu */}
                <div className="border-t border-slate-100 pt-3">
                  <div className="flex justify-between items-center mb-1">
                    <span className="text-xs text-slate-500 font-extrabold uppercase tracking-wide">{t("settings_volume")}</span>
                    <span className="text-[10px] font-mono font-black text-slate-400">{Math.round(fxVolume * 100)}%</span>
                  </div>
                  <input
                    type="range"
                    min={0}
                    max={1}
                    step={0.05}
                    value={fxVolume}
                    onChange={(e) => handleVolumeChange(parseFloat(e.target.value))}
                    className="w-full accent-[#6D3DF5] cursor-pointer"
                    aria-label={t("settings_volume")}
                  />
                </div>
              </div>

              {/* Quick Options */}
              <button
                onClick={() => { sound.playConfirm(); onResume(); }}
                className={`w-full shrink-0 pause-btn-primary ${GameTheme.colors.primary.bgGradient} text-white ${GameTheme.shapes.button} font-black flex items-center justify-center gap-2 cursor-pointer ${GameTheme.shadows.btnPrimary} transition-all duration-150`}
              >
                <Play className="w-4 h-4 fill-white" />
                <span>{t("pause_resume")}</span>
              </button>

              <button
                onClick={() => {
                  onResume();
                  onRestart();
                }}
                className={`w-full shrink-0 pause-btn-secondary bg-slate-100 hover:bg-slate-200 text-slate-700 ${GameTheme.shapes.button} font-extrabold text-xs flex items-center justify-center gap-2 cursor-pointer border ${GameTheme.colors.borders.medium} transition-colors`}
              >
                <RefreshCw className="w-4 h-4" />
                <span>{t("pause_restart")}</span>
              </button>
            </div>

            {/* Exit/Abandon button */}
            <button
              onClick={onQuit}
              className={`shrink-0 mt-auto mx-auto px-5 py-2.5 text-xs font-black uppercase text-rose-500 ${GameTheme.colors.danger.lightBg} border ${GameTheme.colors.danger.border} ${GameTheme.shapes.button} cursor-pointer transition-all flex items-center gap-1.5`}
            >
              <X className="w-3.5 h-3.5" />
              {t("pause_quit")}
            </button>
          </motion.div>
        )}
      </AnimatePresence>

    </div>
  );
}
