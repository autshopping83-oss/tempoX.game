/**
 * @license
 * SPDX-License-Identifier: Apache-2.0
 */

import React, { useState } from "react";
import { GameStats, GAME_ACHIEVEMENTS, getXPForLevel } from "../core/gameEngine";
import { Volume2, VolumeX, Smartphone, Trophy, BarChart2, Sparkles, Sliders } from "lucide-react";
import { motion, AnimatePresence } from "motion/react";
import FloatingBackgroundShapes from "./FloatingBackgroundShapes";
import { ShapeGradients } from "./GeometricShapes";
import { GameTheme, GameColors } from "../core/GameTheme";
import FloatingCard from "./ui/FloatingCard";
import PrimaryButton from "./ui/PrimaryButton";
import StatCard from "./ui/StatCard";
import TrophyCard from "./ui/TrophyCard";
import BottomNavigation from "./ui/BottomNavigation";
import { useI18n, DictKey } from "../core/i18n";

/** Web achievement ids -> i18n key stems (native ids differ). */
const ACH_KEY_STEM: Record<string, string> = {
  elefante: "elephant",
  reflexo: "reflex",
  imparavel: "unstoppable",
  sobrevivente: "survivor",
  recordista: "recordist",
};

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
  const { t } = useI18n();

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
    <div className={`relative flex flex-col flex-grow min-h-0 justify-between ${GameTheme.spacing.outerPadding} w-full text-slate-800 ${GameTheme.colors.background} overflow-x-hidden`}>
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
              {t("home_premium_badge")}
            </span>
          </div>
        </div>

        {/* Official TEMPOX Logo Wordmark */}
        <div className="text-center my-[var(--sp-xs)]">
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
              style={{ height: "var(--logo-h)" }} className="w-auto max-w-[240px] object-contain mx-auto select-none drop-shadow-[0_10px_25px_rgba(109,61,245,0.30)]"
            />
            <p className="text-xs text-slate-400 font-black tracking-[0.3em] uppercase mt-[var(--sp-xs)]">
              {t("home_tagline")}
            </p>
          </motion.div>
        </div>

        {/* Main Tab Area Content */}
        <div className="flex-grow min-h-0 flex flex-col justify-center my-[var(--sp-xs)]">
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
                <FloatingCard className="p-[var(--sp-md)]">
                  <div className="absolute top-0 right-0 w-32 h-32 bg-gradient-to-br from-[#6D3DF5]/5 to-transparent rounded-full -mr-10 -mt-10" />
                  
                  <div className="flex justify-between items-center mb-[var(--sp-xs)] relative z-10">
                    <div>
                      <span className="text-[10px] uppercase tracking-wider text-slate-400 font-extrabold">
                        {t("home_level_profile")}
                      </span>
                      <h3 className={`text-2xl font-black ${GameTheme.colors.primary.text}`}>
                        {t("home_level")} {stats.level}
                      </h3>
                    </div>
                    <div className="text-right">
                      <span className="text-[10px] uppercase tracking-wider text-amber-500 font-extrabold flex items-center gap-1 justify-end">
                        {t("home_best_score")}
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
                    <span>{stats.totalXP} {t("home_xp_accumulated")}</span>
                    <span>{progressPercent}% {t("home_percent_to_level")} {stats.level + 1}</span>
                  </div>
                </FloatingCard>

                {/* Big Visual JOGAR Button */}
                <PrimaryButton onClick={handlePlayClick}>
                  {t("home_play_now")}
                </PrimaryButton>

                {/* Compact, elegant Seed Input */}
                <FloatingCard className="p-3 flex items-center justify-between gap-4 shadow-soft">
                  <div className="flex items-center gap-2">
                    <Sliders className="w-4 h-4 text-slate-400" />
                    <span className="text-xs font-bold text-slate-500 uppercase tracking-wide">{t("home_seed_label")}</span>
                  </div>
                  <input
                    type="text"
                    maxLength={8}
                    placeholder={t("home_seed_placeholder")}
                    value={customSeedInput}
                    onChange={(e) => setCustomSeedInput(e.target.value.replace(/\D/g, ""))}
                    className={`w-32 bg-slate-50 border ${GameTheme.colors.borders.light} rounded-xl py-2 px-3 text-xs font-bold font-mono tracking-widest ${GameTheme.colors.primary.text} text-right placeholder-slate-300 focus:outline-none focus:border-[#6D3DF5]/30 focus:bg-white`}
                  />
                </FloatingCard>
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
                    {t("stats_historical_title")}
                  </h3>
                </div>

                <div className="grid grid-cols-2 gap-3.5">
                  <StatCard label={t("stats_matches")} value={stats.gamesPlayed} />
                  <StatCard label={t("stats_record")} value={stats.highScore} valueClassName="text-amber-500" />
                  <StatCard label={t("stats_total_xp")} value={stats.totalXP} valueClassName="text-[#6D3DF5]" />
                  <StatCard label={t("stats_max_combo")} value={`🔥 x${stats.maxCombo}`} valueClassName="text-[#EF4444]" />
                </div>

                <div className={`bg-slate-50/40 p-3 rounded-2xl border ${GameTheme.colors.borders.light} flex justify-between items-center text-sm`}>
                  <div>
                    <span className={GameTheme.typography.pillLabel}>{t("stats_answers")}</span>
                    <div className="flex gap-3 mt-1 text-xs">
                      <span className="text-[#22C55E] font-black">✓ {stats.totalCorrect}</span>
                      <span className="text-[#EF4444] font-black">✗ {stats.totalIncorrect}</span>
                    </div>
                  </div>
                  <div className="text-right">
                    <span className={GameTheme.typography.pillLabel}>{t("stats_avg_accuracy")}</span>
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
                className="flex flex-col gap-[var(--sp-xs)]"
              >
                <div className="flex items-center gap-2 pb-1">
                  <Trophy className="w-5 h-5 text-amber-500" />
                  <h3 className={GameTheme.typography.sectionTitle}>
                    {t("trophies_title")} ({stats.achievements.length}/5)
                  </h3>
                </div>

                <div className="flex flex-col gap-2.5">
                  {GAME_ACHIEVEMENTS.map((ach) => {
                    const unlocked = stats.achievements.includes(ach.id);
                    const stem = ACH_KEY_STEM[ach.id];
                    const title = stem ? t(`ach_${stem}_title` as DictKey) : ach.title;
                    const description = stem ? t(`ach_${stem}_desc` as DictKey) : ach.description;
                    return (
                      <TrophyCard
                        key={ach.id}
                        icon={ach.icon}
                        title={title}
                        description={description}
                        unlocked={unlocked}
                      />
                    );
                  })}
                </div>
              </motion.div>
            )}
          </AnimatePresence>
        </div>

        {/* Bottom Navigation — floating bar (owns its safe-area lift) */}
        <BottomNavigation
          tabs={[
            { id: "MAIN", label: t("tab_play") },
            { id: "STATS", label: t("tab_stats") },
            { id: "ACHIEVEMENTS", label: t("tab_trophies") },
          ]}
          activeTab={activeTab}
          onChange={(id) => setActiveTab(id as typeof activeTab)}
        />

      </div>
    </div>
  );
}
