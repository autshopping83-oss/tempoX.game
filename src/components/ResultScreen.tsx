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
    <div className={`relative flex flex-col justify-between ${GameTheme.spacing.outerPadding} max-w-md mx-auto w-full h-full text-slate-800 ${GameTheme.colors.background} overflow-y-auto select-none`}>
      <FloatingBackgroundShapes />

      <div className={`relative z-10 flex flex-col flex-grow justify-between h-full ${GameTheme.spacing.containerGap}`}>
        
        {/* Upper Header: Time Out Banner */}
        <div className="text-center mt-3">
          <motion.img
            src="/logo.png"
            alt="TEMPOX"
            draggable={false}
            initial={{ opacity: 0, y: -10 }}
            animate={{ opacity: 1, y: 0 }}
            transition={{ delay: 0.25, duration: 0.5, ease: "easeOut" }}
            className="h-8 w-auto object-contain mx-auto select-none mb-2.5 drop-shadow-[0_0_12px_rgba(109,61,245,0.30)]"
          />
          <span className={`text-[10px] uppercase tracking-[0.25em] text-[#EF4444] ${GameTheme.colors.danger.lightBg} border ${GameTheme.colors.danger.border} px-3 py-1 rounded-full font-black`}>
            TEMPO ESGOTADO!
          </span>
          
          {/* Giant score display - center of attention */}
          <motion.div
            initial={{ scale: 0.8, opacity: 0 }}
            animate={{ scale: 1, opacity: 1 }}
            transition={{ type: "spring", stiffness: 120, damping: 15 }}
            className="my-5"
          >
            <span className="text-[11px] font-extrabold uppercase text-slate-400 tracking-wider">
              Pontuação Final
            </span>
            <h1 className={`${GameTheme.typography.scoreMain} mt-1`}>
              {score.toLocaleString()}
            </h1>

            {isHighScore && (
              <motion.div
                initial={{ rotate: -3, scale: 0.9 }}
                animate={{ rotate: 0, scale: 1 }}
                transition={{ delay: 0.2 }}
                className={`inline-flex items-center gap-1.5 ${GameTheme.colors.warning.bg} text-slate-900 px-4 py-1.5 rounded-full text-xs font-black uppercase mt-3 ${GameTheme.shadows.premium}`}
              >
                <Trophy className="w-3.5 h-3.5 fill-current" />
                🏆 NOVO RECORDE REGISTRADO!
              </motion.div>
            )}
          </motion.div>
        </div>

        {/* Celebratory Graphic Cup or Badge if record */}
        <div className="flex justify-center my-1.5">
          <motion.div
            initial={{ y: 20, opacity: 0 }}
            animate={{ y: 0, opacity: 1 }}
            transition={{ type: "spring", delay: 0.1 }}
            className={`relative bg-white border ${GameTheme.colors.borders.light} rounded-full p-4 ${GameTheme.shadows.soft}`}
          >
            {isHighScore ? (
              <div className="w-16 h-16 rounded-full bg-amber-50 flex items-center justify-center relative">
                <Trophy className="w-9 h-9 text-amber-500 fill-amber-500/20" />
                <Sparkles className="absolute -top-1 -right-1 w-4 h-4 text-amber-500 animate-pulse" />
              </div>
            ) : (
              <div className="w-16 h-16 rounded-full bg-indigo-50 flex items-center justify-center">
                <Award className={`w-9 h-9 ${GameTheme.colors.primary.text}`} />
              </div>
            )}
          </motion.div>
        </div>

        {/* 3 Grid Stats */}
        <div className="grid grid-cols-3 gap-3.5 my-4">
          <div className={`bg-white border ${GameTheme.colors.borders.light}/80 rounded-2xl p-3 text-center ${GameTheme.shadows.soft}`}>
            <div className="mx-auto w-7 h-7 rounded-full bg-amber-50 text-amber-500 flex items-center justify-center mb-1.5">
              <Zap className="w-4 h-4 fill-amber-500/10" />
            </div>
            <span className="text-[9px] text-slate-400 font-bold uppercase tracking-wider block">Desafios</span>
            <p className="text-sm font-black text-slate-800 font-mono mt-0.5">{challengesCompletedSession}</p>
          </div>

          <div className={`bg-white border ${GameTheme.colors.borders.light}/80 rounded-2xl p-3 text-center ${GameTheme.shadows.soft}`}>
            <div className={`mx-auto w-7 h-7 rounded-full ${GameTheme.colors.success.lightBg} ${GameTheme.colors.success.text} flex items-center justify-center mb-1.5`}>
              <Crosshair className="w-4 h-4" />
            </div>
            <span className="text-[9px] text-slate-400 font-bold uppercase tracking-wider block">Precisão</span>
            <p className={`text-sm font-black ${GameTheme.colors.success.text} font-mono mt-0.5`}>{accuracy}%</p>
          </div>

          <div className={`bg-white border ${GameTheme.colors.borders.light}/80 rounded-2xl p-3 text-center ${GameTheme.shadows.soft}`}>
            <div className="mx-auto w-7 h-7 rounded-full bg-rose-50 text-rose-500 flex items-center justify-center mb-1.5">
              <span className="text-xs font-bold">🔥</span>
            </div>
            <span className="text-[9px] text-slate-400 font-bold uppercase tracking-wider block">Combo Máx</span>
            <p className="text-sm font-black text-rose-500 font-mono mt-0.5">x{maxComboSession}</p>
          </div>
        </div>

        {/* Rewarded Ad Card */}
        <div className={`relative bg-white border ${GameTheme.colors.borders.light} ${GameTheme.shapes.card} p-5 mb-5 text-center ${GameTheme.shadows.premium} overflow-hidden`}>
          {/* Graphic gold coins vector-ish indicator */}
          <div className="flex justify-center gap-1.5 mb-2.5">
            <div className="w-5 h-5 rounded-full bg-amber-400 border border-amber-300 shadow-sm flex items-center justify-center text-[10px] font-black text-amber-800">XP</div>
            <div className="w-5 h-5 rounded-full bg-amber-400 border border-amber-300 shadow-sm flex items-center justify-center text-[10px] font-black text-amber-800 -ml-2">XP</div>
            <div className="w-5 h-5 rounded-full bg-amber-400 border border-amber-300 shadow-sm flex items-center justify-center text-[10px] font-black text-amber-800 -ml-2">XP</div>
          </div>

          <span className="text-slate-400 text-[10px] uppercase tracking-wider font-extrabold">
            RECOMPENSA ACUMULADA
          </span>
          <h2 className={`text-2xl font-black ${GameTheme.colors.success.text} font-mono mt-0.5`}>
            +{adState === "COMPLETED" ? xpGainedSession * 2 : xpGainedSession} XP
          </h2>

          {adState !== "COMPLETED" ? (
            <div className="mt-4 flex flex-col items-center gap-2">
              <span className="text-[11px] text-slate-500">Dobre seus ganhos assistindo a um vídeo curto!</span>
              <motion.button
                whileHover={{ scale: 1.02 }}
                whileTap={{ scale: 0.96 }}
                onClick={handleDoubleRewardClick}
                disabled={adState !== "IDLE"}
                className={`w-full max-w-[240px] py-2.5 bg-gradient-to-r from-amber-400 to-amber-500 text-[#111827] rounded-xl font-black text-xs uppercase tracking-wider flex items-center justify-center gap-2 cursor-pointer ${GameTheme.shadows.btnWarning} hover:from-amber-500 hover:to-amber-400 transition-all duration-150`}
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
        </div>

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
        <div className="flex flex-col gap-3 mt-auto">
          <motion.button
            whileHover={{ scale: 1.02 }}
            whileTap={{ scale: 0.95 }}
            onClick={() => onPlayAgain(seed)}
            className={`w-full py-4 bg-gradient-to-r from-emerald-500 to-emerald-600 hover:from-emerald-600 hover:to-emerald-500 text-white ${GameTheme.shapes.button} font-black text-base flex items-center justify-center gap-2 ${GameTheme.shadows.btnSuccess} cursor-pointer transition-all duration-150`}
          >
            <RotateCcw className="w-5 h-5" />
            <span>JOGAR NOVAMENTE</span>
          </motion.button>

          <div className="grid grid-cols-2 gap-3.5">
            <button
              onClick={handleShareClick}
              className={`py-3 bg-white hover:bg-slate-50 border ${GameTheme.colors.borders.medium} text-slate-600 rounded-xl font-extrabold text-xs flex items-center justify-center gap-1.5 cursor-pointer transition-all`}
            >
              <Share2 className="w-3.5 h-3.5" />
              <span>COMPARTILHAR</span>
            </button>

            <button
              onClick={onGoHome}
              className={`py-3 bg-slate-100 hover:bg-slate-200 text-slate-700 rounded-xl font-extrabold text-xs flex items-center justify-center gap-1.5 cursor-pointer transition-all`}
            >
              <Home className="w-3.5 h-3.5" />
              <span>MENU PRINCIPAL</span>
            </button>
          </div>
        </div>

      </div>
    </div>
  );
}
