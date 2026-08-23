/**
 * @license
 * SPDX-License-Identifier: Apache-2.0
 */

import React, { useState } from "react";
import { Trophy, RotateCcw, Home, Sparkles, Share2, Award, Zap, Crosshair } from "lucide-react";
import { motion, AnimatePresence } from "motion/react";
import { sound } from "../core/sound";
import { GameStats } from "../core/gameEngine";
import FloatingBackgroundShapes from "./FloatingBackgroundShapes";
import { GameTheme, GameColors } from "../core/GameTheme";
import FloatingCard from "./ui/FloatingCard";
import PrimaryButton from "./ui/PrimaryButton";
import SecondaryButton from "./ui/SecondaryButton";
import StatCard from "./ui/StatCard";

interface Props {
  score: number;
  maxComboSession: number;
  xpGainedSession: number;
  challengesCompletedSession: number;
  correctAnswersSession: number;
  newAchievementsUnlocked: string[];
  seed: number;
  stats: GameStats;
  onPlayAgain: (seed?: number) => void;
  onGoHome: () => void;
  onDoubleXPReward: (extraXP: number) => void;
}

export default function ResultScreen({
  score,
  maxComboSession,
  xpGainedSession,
  challengesCompletedSession,
  correctAnswersSession,
  newAchievementsUnlocked,
  seed,
  stats,
  onPlayAgain,
  onGoHome,
  onDoubleXPReward,
}: Props) {
  const [adState, setAdState] = useState<"IDLE" | "LOADING" | "PLAYING" | "COMPLETED">("IDLE");
  const [adCountdown, setAdCountdown] = useState(5);

  const isHighScore = score >= stats.highScore && score > 0;
  const accuracy =
    challengesCompletedSession > 0
      ? Math.round((correctAnswersSession / challengesCompletedSession) * 100)
      : 0;

  const handleDoubleRewardClick = () => {
    setAdState("LOADING");
    sound.playTick(false);

    setTimeout(() => {
      setAdState("PLAYING");
      let count = 4;
      setAdCountdown(5);

      const interval = setInterval(() => {
        if (count > 0) {
          setAdCountdown(count);
          sound.playTick(false);
          count--;
        } else {
          clearInterval(interval);
          setAdState("COMPLETED");
          sound.playRecord();
          onDoubleXPReward(xpGainedSession);
        }
      }, 1000);
    }, 1200);
  };

  const handleShareClick = () => {
    if (navigator.share) {
      navigator.share({
        title: "TEMPOX",
        text: `Fiz ${score} pontos com combo x${maxComboSession} no TEMPOX! Consegue superar?`,
        url: window.location.href,
      }).catch(() => {});
    } else {
      alert(`Pontuação compartilhada: ${score} pontos!`);
    }
  };

  return (
    <div className={`relative flex flex-col flex-grow min-h-0 justify-between ${GameTheme.spacing.outerPadding} w-full text-slate-800 ${GameTheme.colors.background} overflow-x-hidden`}>
      <FloatingBackgroundShapes />

      <div className={`relative z-10 flex flex-col flex-grow justify-between h-full ${GameTheme.spacing.containerGap}`}>
        
        {/* Upper Header: Time Out Banner */}
        <div className="text-center mt-[var(--sp-xs)]">
          <span className={`text-[10px] uppercase tracking-[0.25em] text-[#EF4444] ${GameTheme.colors.danger.lightBg} border ${GameTheme.colors.danger.border} px-3 py-1 rounded-full font-black`}>
            TEMPO ESGOTADO!
          </span>
          
          {/* Giant score display - center of attention */}
          <motion.div
            initial={{ scale: 0.92, opacity: 0 }}
            animate={{ scale: 1, opacity: 1 }}
            transition={{ type: "spring", stiffness: 120, damping: 15 }}
            className="my-[var(--sp-xs)] flex items-center justify-center gap-[var(--sp-sm)]"
          >
            {/* Compact medal/trophy badge — always visible */}
            <motion.div
              initial={{ y: 14, opacity: 0 }}
              animate={{ y: 0, opacity: 1 }}
              transition={{ type: "spring", delay: 0.1 }}
              className={`shrink-0 w-12 h-12 rounded-full flex items-center justify-center border shadow-soft ${
                isHighScore ? "bg-amber-50 border-amber-200" : "bg-indigo-50 border-slate-100"
              }`}
            >
              {isHighScore ? (
                <Trophy className="w-6 h-6 text-amber-500 fill-amber-500/20" />
              ) : (
                <Award className={`w-6 h-6 ${GameTheme.colors.primary.text}`} />
              )}
            </motion.div>

            <div>
              <span className="text-[10px] font-extrabold uppercase text-slate-400 tracking-wider block leading-none">
                Pontuação Final
              </span>
              <h1
                className={`${GameTheme.typography.scoreMain.replace("text-7xl", "")} mt-0.5 leading-none`}
                style={{ fontSize: "var(--fs-score)" }}
              >
                {score.toLocaleString()}
              </h1>
            </div>
          </motion.div>

          {isHighScore && (
            <motion.div
              initial={{ rotate: -3, scale: 0.9 }}
              animate={{ rotate: 0, scale: 1 }}
              transition={{ delay: 0.2 }}
              className={`inline-flex items-center gap-1.5 ${GameTheme.colors.warning.bg} text-slate-900 px-3 py-1 rounded-full text-[10px] font-black uppercase ${GameTheme.shadows.premium}`}
            >
              <Sparkles className="w-3.5 h-3.5" />
              🏆 NOVO RECORDE REGISTRADO!
            </motion.div>
          )}
        </div>

        {/* 3 Grid Stats */}
        <div className="grid grid-cols-3 gap-[var(--sp-xs)] my-[var(--sp-xs)]">
          <StatCard
            label="Desafios"
            value={challengesCompletedSession}
            icon={<Zap className="w-4 h-4 fill-amber-500/10" />}
            valueClassName="text-amber-500"
          />
          <StatCard
            label="Precisão"
            value={`${accuracy}%`}
            icon={<Crosshair className="w-4 h-4" />}
            valueClassName={GameTheme.colors.success.text}
          />
          <StatCard
            label="Combo Máx"
            value={`x${maxComboSession}`}
            icon={<span className="text-xs font-bold">🔥</span>}
            valueClassName="text-rose-500"
          />
        </div>

        {/* Rewarded Ad Card */}
        <FloatingCard className="p-[var(--sp-sm)] mb-[var(--sp-xs)] text-center">
          {/* Graphic gold coins vector-ish indicator */}
          <div className="flex justify-center gap-1 mb-[var(--sp-xs)]">
            <div className="w-4 h-4 rounded-full bg-amber-400 border border-amber-300 shadow-sm flex items-center justify-center text-[10px] font-black text-amber-800">XP</div>
            <div className="w-4 h-4 rounded-full bg-amber-400 border border-amber-300 shadow-sm flex items-center justify-center text-[10px] font-black text-amber-800 -ml-2">XP</div>
            <div className="w-4 h-4 rounded-full bg-amber-400 border border-amber-300 shadow-sm flex items-center justify-center text-[10px] font-black text-amber-800 -ml-2">XP</div>
          </div>

          <span className="text-slate-400 text-[10px] uppercase tracking-wider font-extrabold">
            RECOMPENSA ACUMULADA
          </span>
          <h2 className={`text-xl font-black ${GameTheme.colors.success.text} font-mono leading-none`}>
            +{adState === "COMPLETED" ? xpGainedSession * 2 : xpGainedSession} XP
          </h2>

          {adState !== "COMPLETED" ? (
            <div className="mt-[var(--sp-xs)] flex flex-col items-center gap-1.5">
              <span className="text-[11px] text-slate-500">Dobre seus ganhos assistindo a um vídeo curto!</span>
              <motion.button
                whileHover={{ scale: 1.02 }}
                whileTap={{ scale: 0.96 }}
                onClick={handleDoubleRewardClick}
                disabled={adState !== "IDLE"}
                className={`w-full min-h-[56px] bg-gradient-to-r from-amber-400 to-amber-500 text-[#111827] rounded-xl font-black text-xs uppercase tracking-wider flex items-center justify-center gap-2 cursor-pointer ${GameTheme.shadows.btnWarning} hover:from-amber-500 hover:to-amber-400 transition-all duration-150`}
              >
                <span>🎬 DOBRAR RECOMPENSA</span>
              </motion.button>
            </div>
          ) : (
            <div className="mt-3 text-xs font-black text-emerald-600 flex items-center justify-center gap-1 bg-emerald-50 py-1.5 px-3 rounded-full border border-emerald-100 max-w-[180px] mx-auto">
              <span>✓ DOBRADO COM SUCESSO!</span>
            </div>
          )}

          {/* Ad Play Screen Simulation overlay */}
          <AnimatePresence>
            {(adState === "LOADING" || adState === "PLAYING") && (
              <motion.div
                initial={{ opacity: 0 }}
                animate={{ opacity: 1 }}
                exit={{ opacity: 0 }}
                className={`absolute inset-0 ${GameTheme.colors.background} z-40 flex flex-col items-center justify-center p-4 text-center`}
              >
                {adState === "LOADING" ? (
                  <div className="flex flex-col items-center gap-2">
                    <div className={`w-8 h-8 border-4 ${GameTheme.colors.primary.border} border-t-transparent rounded-full animate-spin`} />
                    <span className="text-xs uppercase tracking-wider text-slate-500 font-extrabold">
                      Carregando Anúncio...
                    </span>
                  </div>
                ) : (
                  <div className="flex flex-col items-center gap-2">
                    <div className={`text-5xl font-mono font-black ${GameTheme.colors.primary.text} animate-soft-pulse`}>
                      {adCountdown}
                    </div>
                    <span className="text-xs uppercase tracking-wider text-slate-700 font-extrabold">
                      Vídeo Premiado
                    </span>
                    <p className="text-[10px] text-slate-400 max-w-[200px]">
                      Ganhe o dobro de XP ao fim do anúncio promocional.
                    </p>
                  </div>
                )}
              </motion.div>
            )}
          </AnimatePresence>
        </FloatingCard>

        {/* New Unlocked Achievements Announcement */}
        {newAchievementsUnlocked.length > 0 && (
          <div className={`${GameTheme.colors.primary.lightBg} border ${GameTheme.colors.primary.borderLight} rounded-2xl p-4 mb-4`}>
            <h4 className={`text-[10px] font-black ${GameTheme.colors.primary.text} uppercase tracking-widest text-center mb-2 flex items-center justify-center gap-1.5`}>
              <Trophy className="w-3.5 h-3.5" />
              NOVOS TROFÉUS CONQUISTADOS!
            </h4>
            <div className="flex flex-col gap-1.5">
              {newAchievementsUnlocked.map((id) => (
                <div key={id} className="text-center text-xs font-black text-slate-700">
                  {id === "elefante" && "🧠 Memória de Elefante"}
                  {id === "reflexo" && "⚡ Reflexo Perfeito"}
                  {id === "imparavel" && "🔥 Imparável"}
                  {id === "sobrevivente" && "⏱️ Sobrevivente"}
                  {id === "recordista" && "🏆 Recordista"}
                </div>
              ))}
            </div>
          </div>
        )}

        {/* Footer Navigation Actions */}
        <div className="flex flex-col gap-[var(--sp-xs)] mt-auto pb-safe">
          <PrimaryButton onClick={() => onPlayAgain(seed)} icon={<RotateCcw className="w-5 h-5" />}>
            JOGAR NOVAMENTE
          </PrimaryButton>

          <div className="grid grid-cols-2 gap-3.5">
            <SecondaryButton onClick={handleShareClick}>
              <Share2 className="w-4 h-4" />
              <span>COMPARTILHAR</span>
            </SecondaryButton>

            <SecondaryButton onClick={onGoHome}>
              <Home className="w-4 h-4" />
              <span>MENU PRINCIPAL</span>
            </SecondaryButton>
          </div>
        </div>

      </div>
    </div>
  );
}
