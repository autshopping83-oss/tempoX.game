/**
 * @license
 * SPDX-License-Identifier: Apache-2.0
 */

import React, { useState } from "react";
import { GameStats, GAME_ACHIEVEMENTS, getXPForLevel } from "../core/gameEngine";
import { Play, Volume2, VolumeX, Smartphone, Trophy, BarChart2, Sparkles, Sliders } from "lucide-react";
import { motion, AnimatePresence } from "motion/react";
import FloatingBackgroundShapes from "./FloatingBackgroundShapes";
import { ShapeGradients } from "./GeometricShapes";
import { GameTheme, GameColors } from "../core/GameTheme";

interface Props {
  stats: GameStats;
  soundOn: boolean;
  vibeOn: boolean;
  onToggleSound: () => void;
  onToggleVibration: () => void;
  onStartGame: (seed?: number) => void;
}

export default function HomeScreen({
  stats,
  soundOn,
  vibeOn,
  onToggleSound,
  onToggleVibration,
  onStartGame,
}: Props) {
  const [activeTab, setActiveTab] = useState<"MAIN" | "STATS" | "ACHIEVEMENTS">("MAIN");
  const [customSeedInput, setCustomSeedInput] = useState<string>("");

  // XP calculation
  const xpCurrentLevel = getXPForLevel(stats.level);
  const xpNextLevel = getXPForLevel(stats.level + 1);
  const levelProgressXp = stats.totalXP - xpCurrentLevel;
  const xpRequiredForLevelUp = xpNextLevel - xpCurrentLevel;
  const progressPercent = Math.min(
    100,
    Math.max(0, Math.round((levelProgressXp / xpRequiredForLevelUp) * 100))
  );

  const handlePlayClick = () => {
    const seedNum = customSeedInput ? parseInt(customSeedInput, 10) : undefined;
    onStartGame(seedNum);
  };

  return (
    <div className={`relative flex flex-col flex-grow justify-between ${GameTheme.spacing.outerPadding} max-w-md mx-auto w-full h-full text-slate-800 ${GameTheme.colors.background} overflow-y-auto select-none`}>
      {/* Visual gradients & shapes in the background */}
      <FloatingBackgroundShapes />
      <ShapeGradients />

      {/* Main Core HUD Wrapper */}
      <div className={`relative z-10 flex flex-col flex-grow justify-between h-full ${GameTheme.spacing.containerGap}`}>
        
        {/* Top Header Row (Minimal settings toggles) */}
        <div className="flex justify-between items-center pt-2">
          {/* Decorative left item */}
          <div className="flex gap-2.5">
            <button
              onClick={onToggleSound}
              className={`p-2.5 rounded-full transition-all bg-white border ${GameTheme.colors.borders.light} ${GameTheme.shadows.soft} cursor-pointer text-slate-500 hover:${GameTheme.colors.primary.text} active:scale-95`}
              title={soundOn ? "Desativar Som" : "Ativar Som"}
            >
              {soundOn ? <Volume2 className="w-4 h-4" /> : <VolumeX className="w-4 h-4" />}
            </button>
            <button
              onClick={onToggleVibration}
              className={`p-2.5 rounded-full transition-all bg-white border ${GameTheme.colors.borders.light} ${GameTheme.shadows.soft} cursor-pointer ${
                vibeOn ? "text-[#22C55E]" : "text-slate-400"
              } hover:text-[#22C55E] active:scale-95`}
              title={vibeOn ? "Desativar Vibração" : "Ativar Vibração"}
            >
              <Smartphone className="w-4 h-4" />
            </button>
          </div>

          <div className={`text-right flex items-center gap-1 ${GameTheme.colors.primary.lightBg} px-3 py-1.5 rounded-full border ${GameTheme.colors.primary.borderLight}`}>
            <Sparkles className={`w-3.5 h-3.5 ${GameTheme.colors.primary.text}`} />
            <span className={`text-[10px] font-black tracking-wider ${GameTheme.colors.primary.text} uppercase`}>
              PREMIUM ARCADE
            </span>
          </div>
        </div>

        {/* Official TEMPOX Logo Wordmark */}
        <div className="text-center my-4">
          <motion.div
            initial={{ scale: 0.9, opacity: 0 }}
            animate={{ scale: 1, opacity: 1 }}
            transition={{ type: "spring", stiffness: 100, damping: 15 }}
            className="inline-block relative"
          >
            {/* Subtle floaty decorative elements nearby */}
            <span className="absolute -top-4 -left-6 text-xl animate-float-1 text-[#FACC15]">▲</span>
            <span className="absolute -bottom-2 -right-6 text-lg animate-float-2 text-[#EC4899]">⬦</span>
            <span className="absolute top-2 -right-8 text-xs animate-float-3 text-[#3B82F6]">●</span>

            <img
              src="/logo.png"
              alt="TEMPOX"
              draggable={false}
              className="h-28 sm:h-32 w-auto max-w-[240px] object-contain mx-auto select-none drop-shadow-[0_10px_25px_rgba(109,61,245,0.30)]"
            />
            <p className="text-xs text-slate-400 font-black tracking-[0.3em] uppercase mt-3">
              THINK FAST. REACT FASTER.
            </p>
          </motion.div>
        </div>

        {/* Main Tab Area Content */}
        <div className="flex-grow flex flex-col justify-center my-2 min-h-[220px]">
          <AnimatePresence mode="wait">
            {activeTab === "MAIN" && (
              <motion.div
                key="main-tab"
                initial={{ opacity: 0, scale: 0.96 }}
                animate={{ opacity: 1, scale: 1 }}
                exit={{ opacity: 0, scale: 0.96 }}
                transition={{ duration: 0.2 }}
                className={`flex flex-col ${GameTheme.spacing.outerPadding} ${GameTheme.spacing.containerGap}`}
              >
                {/* Clean, high-contrast premium card showing Level & HighScore */}
                <div className={`bg-white border ${GameTheme.colors.borders.light} ${GameTheme.shapes.card} p-5 ${GameTheme.shadows.premium} relative overflow-hidden`}>
                  <div className="absolute top-0 right-0 w-32 h-32 bg-gradient-to-br from-[#6D3DF5]/5 to-transparent rounded-full -mr-10 -mt-10" />
                  
                  <div className="flex justify-between items-center mb-4 relative z-10">
                    <div>
                      <span className="text-[10px] uppercase tracking-wider text-slate-400 font-extrabold">
                        Nível de Perfil
                      </span>
                      <h3 className={`text-2xl font-black ${GameTheme.colors.primary.text}`}>
                        Nível {stats.level}
                      </h3>
                    </div>
                    <div className="text-right">
                      <span className="text-[10px] uppercase tracking-wider text-amber-500 font-extrabold flex items-center gap-1 justify-end">
                        ★ MELHOR PONTUAÇÃO
                      </span>
                      <h3 className="text-3xl font-extrabold text-slate-950 tracking-tight font-mono">
                        {stats.highScore.toLocaleString()}
                      </h3>
                    </div>
                  </div>

                  {/* Progressive XP Bar */}
                  <div className="w-full bg-slate-50 h-3 rounded-full overflow-hidden mb-2 border border-slate-100/50 p-0.5">
                    <motion.div
                      initial={{ width: 0 }}
                      animate={{ width: `${progressPercent}%` }}
                      transition={{ duration: 0.8, ease: "easeOut" }}
                      className="bg-gradient-to-r from-[#6D3DF5] to-[#EC4899] h-full rounded-full"
                    />
                  </div>
                  <div className="flex justify-between text-[10px] text-slate-400 font-semibold">
                    <span>{stats.totalXP} XP acumulados</span>
                    <span>{progressPercent}% para Nível {stats.level + 1}</span>
                  </div>
                </div>

                {/* Big Visual JOGAR Button */}
                <motion.button
                  whileHover={{ scale: 1.02 }}
                  whileTap={{ scale: 0.95 }}
                  onClick={handlePlayClick}
                  className={`w-full py-4 px-6 ${GameTheme.colors.primary.bgGradient} text-white ${GameTheme.shapes.button} font-black flex items-center justify-between ${GameTheme.shadows.btnPrimary} cursor-pointer text-lg tracking-wider transition-all duration-150`}
                >
                  <div className="flex items-center gap-3">
                    <div className="p-1 bg-white/20 rounded-lg">
                      <Play className="fill-white stroke-none w-4 h-4" />
                    </div>
                    <span>JOGAR AGORA</span>
                  </div>
                  <span className="text-white/60 font-mono text-sm">▶</span>
                </motion.button>

                {/* Compact, elegant Seed Input */}
                <div className={`bg-white border ${GameTheme.colors.borders.light} ${GameTheme.shapes.button} p-3 ${GameTheme.shadows.soft} flex items-center justify-between gap-4`}>
                  <div className="flex items-center gap-2">
                    <Sliders className="w-4 h-4 text-slate-400" />
                    <span className="text-xs font-bold text-slate-500 uppercase tracking-wide">Seed da Partida</span>
                  </div>
                  <input
                    type="text"
                    maxLength={8}
                    placeholder="Opcional"
                    value={customSeedInput}
                    onChange={(e) => setCustomSeedInput(e.target.value.replace(/\D/g, ""))}
                    className={`w-32 bg-slate-50 border ${GameTheme.colors.borders.light} rounded-xl py-1 px-3 text-xs font-bold font-mono tracking-widest ${GameTheme.colors.primary.text} text-right placeholder-slate-300 focus:outline-none focus:border-[#6D3DF5]/30 focus:bg-white`}
                  />
                </div>
              </motion.div>
            )}

            {activeTab === "STATS" && (
              <motion.div
                key="stats-tab"
                initial={{ opacity: 0, scale: 0.96 }}
                animate={{ opacity: 1, scale: 1 }}
                exit={{ opacity: 0, scale: 0.96 }}
                transition={{ duration: 0.2 }}
                className={`flex flex-col gap-4 bg-white border ${GameTheme.colors.borders.light} ${GameTheme.shapes.card} p-5 ${GameTheme.shadows.premium}`}
              >
                <div className="flex items-center gap-2 pb-2 border-b border-slate-100">
                  <BarChart2 className={`w-5 h-5 ${GameTheme.colors.primary.text}`} />
                  <h3 className={GameTheme.typography.sectionTitle}>
                    Estatísticas Históricas
                  </h3>
                </div>

                <div className="grid grid-cols-2 gap-3.5">
                  <div className={`bg-slate-50/50 p-3 rounded-2xl border ${GameTheme.colors.borders.light}`}>
                    <span className={GameTheme.typography.pillLabel}>Partidas</span>
                    <p className="text-xl font-black text-slate-900 font-mono mt-0.5">{stats.gamesPlayed}</p>
                  </div>
                  <div className={`bg-slate-50/50 p-3 rounded-2xl border ${GameTheme.colors.borders.light}`}>
                    <span className={GameTheme.typography.pillLabel}>Recorde</span>
                    <p className="text-xl font-black text-amber-500 font-mono mt-0.5">{stats.highScore}</p>
                  </div>
                  <div className={`bg-slate-50/50 p-3 rounded-2xl border ${GameTheme.colors.borders.light}`}>
                    <span className={GameTheme.typography.pillLabel}>Total XP</span>
                    <p className="text-xl font-black text-[#6D3DF5] font-mono mt-0.5">{stats.totalXP}</p>
                  </div>
                  <div className={`bg-slate-50/50 p-3 rounded-2xl border ${GameTheme.colors.borders.light}`}>
                    <span className={GameTheme.typography.pillLabel}>Combo Máximo</span>
                    <p className="text-xl font-black text-[#EF4444] font-mono mt-0.5">🔥 x{stats.maxCombo}</p>
                  </div>
                </div>

                <div className={`bg-slate-50/40 p-3 rounded-2xl border ${GameTheme.colors.borders.light} flex justify-between items-center text-sm`}>
                  <div>
                    <span className={GameTheme.typography.pillLabel}>Respostas</span>
                    <div className="flex gap-3 mt-1 text-xs">
                      <span className="text-[#22C55E] font-black">✓ {stats.totalCorrect}</span>
                      <span className="text-[#EF4444] font-black">✗ {stats.totalIncorrect}</span>
                    </div>
                  </div>
                  <div className="text-right">
                    <span className={GameTheme.typography.pillLabel}>Precisão Média</span>
                    <p className="text-lg font-black text-slate-800 font-mono mt-0.5">
                      {stats.totalCorrect + stats.totalIncorrect > 0
                        ? `${Math.round(
                            (stats.totalCorrect / (stats.totalCorrect + stats.totalIncorrect)) * 100
                          )}%`
                        : "0%"}
                    </p>
                  </div>
                </div>
              </motion.div>
            )}

            {activeTab === "ACHIEVEMENTS" && (
              <motion.div
                key="ach-tab"
                initial={{ opacity: 0, scale: 0.96 }}
                animate={{ opacity: 1, scale: 1 }}
                exit={{ opacity: 0, scale: 0.96 }}
                transition={{ duration: 0.2 }}
                className="flex flex-col gap-3 max-h-[260px] overflow-y-auto pr-1"
              >
                <div className="flex items-center gap-2 pb-1">
                  <Trophy className="w-5 h-5 text-amber-500" />
                  <h3 className={GameTheme.typography.sectionTitle}>
                    Troféus e Conquistas ({stats.achievements.length}/5)
                  </h3>
                </div>

                <div className="flex flex-col gap-2.5">
                  {GAME_ACHIEVEMENTS.map((ach) => {
                    const unlocked = stats.achievements.includes(ach.id);
                    return (
                      <div
                        key={ach.id}
                        className={`flex items-center gap-3 p-3.5 rounded-2xl border transition-all ${
                          unlocked
                            ? `bg-white border-purple-200 ${GameTheme.shadows.soft}`
                            : `bg-white/40 ${GameTheme.colors.borders.light} opacity-60`
                        }`}
                      >
                        <span className="text-2xl select-none">{ach.icon}</span>
                        <div className="flex-1 min-w-0">
                          <h4 className={`${GameTheme.typography.cardTitle} truncate`}>{ach.title}</h4>
                          <p className="text-[10px] text-slate-400 mt-0.5 leading-tight">{ach.description}</p>
                        </div>
                        <div>
                          {unlocked ? (
                            <span className="text-[9px] bg-emerald-50 text-emerald-600 font-extrabold uppercase px-2 py-0.5 rounded-full border border-emerald-100">
                              ✓ LIBERADO
                            </span>
                          ) : (
                            <span className={`text-[9px] bg-slate-50 text-slate-400 font-bold uppercase px-2 py-0.5 rounded-full border ${GameTheme.colors.borders.light}`}>
                              BLOQUEADO
                            </span>
                          )}
                        </div>
                      </div>
                    );
                  })}
                </div>
              </motion.div>
            )}
          </AnimatePresence>
        </div>

        {/* Elegant Bottom Segmented Bar */}
        <div className={`mt-4 pt-3 border-t ${GameTheme.colors.borders.light}`}>
          <div className="flex bg-slate-100/80 p-1 rounded-2xl border border-slate-200/40">
            <button
              onClick={() => setActiveTab("MAIN")}
              className={`flex-grow py-2.5 text-xs font-black uppercase tracking-wider rounded-xl transition-all cursor-pointer ${
                activeTab === "MAIN"
                  ? `bg-white ${GameTheme.colors.primary.text} shadow-sm font-extrabold`
                  : "text-slate-400 hover:text-slate-600"
              }`}
            >
              Jogar
            </button>
            <button
              onClick={() => setActiveTab("STATS")}
              className={`flex-grow py-2.5 text-xs font-black uppercase tracking-wider rounded-xl transition-all cursor-pointer ${
                activeTab === "STATS"
                  ? `bg-white ${GameTheme.colors.primary.text} shadow-sm font-extrabold`
                  : "text-slate-400 hover:text-slate-600"
              }`}
            >
              Estatísticas
            </button>
            <button
              onClick={() => setActiveTab("ACHIEVEMENTS")}
              className={`flex-grow py-2.5 text-xs font-black uppercase tracking-wider rounded-xl transition-all cursor-pointer ${
                activeTab === "ACHIEVEMENTS"
                  ? `bg-white ${GameTheme.colors.primary.text} shadow-sm font-extrabold`
                  : "text-slate-400 hover:text-slate-600"
              }`}
            >
              Troféus
            </button>
          </div>
        </div>

      </div>
    </div>
  );
}
