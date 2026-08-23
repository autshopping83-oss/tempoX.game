/**
 * @license
 * SPDX-License-Identifier: Apache-2.0
 */

import React, { useEffect, useState } from "react";
import { useGame } from "./core/useGame";
import HomeScreen from "./components/HomeScreen";
import ChallengeScreen from "./components/ChallengeScreen";
import ResultScreen from "./components/ResultScreen";
import SplashScreen from "./components/SplashScreen";
import { Smartphone, RotateCcw, Flame, Info, Sparkles, X, HelpCircle, ArrowRight } from "lucide-react";
import { motion, AnimatePresence } from "motion/react";
import AppHeader from "./components/ui/AppHeader";
import FloatingCard from "./components/ui/FloatingCard";

export default function App() {
  const {
    gameState,
    setGameState,
    score,
    combo,
    maxComboSession,
    seed,
    currentChallenge,
    gameTimeLeft,
    challengeTimeLeft,
    stats,
    soundOn,
    vibeOn,
    newAchievementsUnlocked,
    xpGainedSession,
    challengesCompletedSession,
    correctAnswersSession,
    difficultyLevel,
    toggleSound,
    toggleVibration,
    startGame,
    pauseGame,
    resumeGame,
    quitGame,
    handleChallengeResult,
  } = useGame();

  const [aboutOpen, setAboutOpen] = useState(false);
  const [showSplash, setShowSplash] = useState(true);

  useEffect(() => {
    const t = setTimeout(() => setShowSplash(false), 1900);
    return () => clearTimeout(t);
  }, []);

  const handleDoubleReward = (extraXP: number) => {
    const savedStats = localStorage.getItem("60s_game_stats");
    if (savedStats) {
      try {
        const parsed = JSON.parse(savedStats);
        parsed.totalXP += extraXP;
        localStorage.setItem("60s_game_stats", JSON.stringify(parsed));
      } catch (e) {
        console.error(e);
      }
    }
  };

  const isPlayingMode = gameState === "PLAYING" || gameState === "PAUSED";

  return (
    <div className="h-app w-full bg-slate-50 flex flex-col text-slate-800 font-sans antialiased selection:bg-[#6D3DF5]/10 overflow-hidden">

      {/* 1. TOP APP BAR (Only visible outside of active playing to maximize gaming space) */}
      {!isPlayingMode && (
        <AppHeader>
          {/* Brand Zone */}
          <div className="flex items-center gap-1.5">
            <img
              src="/logo.png"
              alt="TEMPOX"
              draggable={false}
              className="h-9 w-auto object-contain select-none drop-shadow-[0_0_10px_rgba(109,61,245,0.25)]"
            />
          </div>

          {/* Navigation Links Zone */}
          <nav className="flex items-center gap-1.5">
            <button
              onClick={() => {
                setGameState("HOME");
                setAboutOpen(false);
              }}
              className={`text-xs font-extrabold px-3 py-2 rounded-full transition-all cursor-pointer ${
                gameState === "HOME" ? "text-[#6D3DF5] bg-[#6D3DF5]/5" : "text-slate-400 hover:text-slate-600"
              }`}
            >
              Início
            </button>
            <button
              onClick={() => setAboutOpen(true)}
              className="text-xs font-extrabold px-3 py-2 rounded-full text-slate-400 hover:text-slate-600 cursor-pointer flex items-center gap-1"
            >
              <HelpCircle className="w-4 h-4" />
              <span>Regras</span>
            </button>
            <button
              onClick={() => startGame()}
              className="px-4 py-2.5 ml-1 bg-gradient-to-r from-[#6D3DF5] to-[#5124D6] hover:from-[#5124D6] hover:to-[#6D3DF5] text-white rounded-xl text-[11px] font-black uppercase tracking-wider transition-all active:scale-95 cursor-pointer shadow-soft"
            >
              START
            </button>
          </nav>
        </AppHeader>
      )}

      {/* 2. FULL-BLEED SCREEN CONTAINER — edge-to-edge mobile */}
      <main className="w-full flex-1 flex flex-col bg-[#F8FAFC] relative overflow-hidden">
        <AnimatePresence mode="wait">
          {gameState === "HOME" && (
            <motion.div
              key="screen-home"
              initial={{ opacity: 0 }}
              animate={{ opacity: 1 }}
              exit={{ opacity: 0 }}
              className="w-full h-full flex flex-col flex-1"
            >
              <HomeScreen
                stats={stats}
                soundOn={soundOn}
                vibeOn={vibeOn}
                onToggleSound={toggleSound}
                onToggleVibration={toggleVibration}
                onStartGame={startGame}
              />
            </motion.div>
          )}

          {isPlayingMode && (
            <motion.div
              key="screen-play"
              initial={{ opacity: 0 }}
              animate={{ opacity: 1 }}
              exit={{ opacity: 0 }}
              className="w-full h-full flex flex-col flex-1"
            >
              <ChallengeScreen
                challenge={currentChallenge}
                score={score}
                combo={combo}
                gameTimeLeft={gameTimeLeft}
                challengeTimeLeft={challengeTimeLeft}
                difficultyLevel={difficultyLevel}
                soundOn={soundOn}
                vibeOn={vibeOn}
                gameState={gameState as any}
                onPause={pauseGame}
                onResume={resumeGame}
                onQuit={quitGame}
                onSolveChallenge={handleChallengeResult}
                onToggleSound={toggleSound}
                onToggleVibration={toggleVibration}
                onRestart={() => startGame(seed)}
              />
            </motion.div>
          )}

          {gameState === "GAMEOVER" && (
            <motion.div
              key="screen-over"
              initial={{ opacity: 0 }}
              animate={{ opacity: 1 }}
              exit={{ opacity: 0 }}
              className="w-full h-full flex flex-col flex-grow"
            >
              <ResultScreen
                score={score}
                maxComboSession={maxComboSession}
                xpGainedSession={xpGainedSession}
                challengesCompletedSession={challengesCompletedSession}
                correctAnswersSession={correctAnswersSession}
                newAchievementsUnlocked={newAchievementsUnlocked}
                seed={seed}
                stats={stats}
                onPlayAgain={startGame}
                onGoHome={quitGame}
                onDoubleXPReward={handleDoubleReward}
              />
            </motion.div>
          )}
        </AnimatePresence>

        {/* Info dialog modal overlay - fully redesigned with off-white premium theme */}
        <AnimatePresence>
          {aboutOpen && (
            <motion.div
              initial={{ opacity: 0 }}
              animate={{ opacity: 1 }}
              exit={{ opacity: 0 }}
              className="absolute inset-0 bg-[#F8FAFC]/98 backdrop-blur-md z-50 flex flex-col justify-between pt-safe pb-safe py-6 px-5 text-slate-800 select-none overflow-hidden"
            >
              <div className="flex flex-col gap-[var(--sp-sm)] max-w-sm mx-auto w-full min-h-0">
                <div className="text-center">
                  <div className="w-9 h-9 rounded-full bg-indigo-50 text-[#6D3DF5] flex items-center justify-center mx-auto mb-2">
                    <Sparkles className="w-5 h-5 animate-pulse" />
                  </div>
                  <h3 className="text-xl font-black text-slate-900 mt-1 tracking-tight">
                    COMO JOGAR
                  </h3>
                  <p className="text-[10px] text-slate-400 mt-1 uppercase tracking-widest font-extrabold">
                    REGRAS DO TEMPOX
                  </p>
                </div>

                <FloatingCard className="flex flex-col gap-[var(--sp-xs)] text-xs text-slate-600 leading-relaxed p-[var(--sp-md)]">
                  <p>
                    <strong className="text-slate-800">TEMPOX</strong> é um jogo rápido de agilidade mental extrema. Você tem exatamente um minuto para resolver uma sequência sem fim de testes rápidos:
                  </p>
                  
                  <div className="flex flex-col gap-3.5 pl-1">
                    <div className="flex items-start gap-2.5">
                      <div className="w-5 h-5 rounded-full bg-pink-50 text-pink-500 flex items-center justify-center font-bold text-[10px] shrink-0 mt-0.5">🧠</div>
                      <div>
                        <h4 className="font-extrabold text-slate-800">Memória</h4>
                        <p className="text-[11px] text-slate-500">Decore e repita a sequência exibida de formas coloridas.</p>
                      </div>
                    </div>

                    <div className="flex items-start gap-2.5">
                      <div className="w-5 h-5 rounded-full bg-amber-50 text-amber-500 flex items-center justify-center font-bold text-[10px] shrink-0 mt-0.5">⚡</div>
                      <div>
                        <h4 className="font-extrabold text-slate-800">Reflexo</h4>
                        <p className="text-[11px] text-slate-500">Toque rápido no alvo dourado. Nunca toque nos de perigo vermelho!</p>
                      </div>
                    </div>

                    <div className="flex items-start gap-2.5">
                      <div className="w-5 h-5 rounded-full bg-blue-50 text-blue-500 flex items-center justify-center font-bold text-[10px] shrink-0 mt-0.5">➗</div>
                      <div>
                        <h4 className="font-extrabold text-slate-800">Matemática</h4>
                        <p className="text-[11px] text-slate-500">Faça cálculos de múltipla escolha sob pressão extrema.</p>
                      </div>
                    </div>

                    <div className="flex items-start gap-2.5">
                      <div className="w-5 h-5 rounded-full bg-emerald-50 text-emerald-500 flex items-center justify-center font-bold text-[10px] shrink-0 mt-0.5">👀</div>
                      <div>
                        <h4 className="font-extrabold text-slate-800">Atenção</h4>
                        <p className="text-[11px] text-slate-500">Localize instantaneamente o símbolo que é diferente dos outros.</p>
                      </div>
                    </div>
                  </div>

                  <p className="border-t border-slate-100 pt-3 text-[10px] text-slate-400 font-extrabold uppercase tracking-wide">
                    🔥 Acertos consecutivos geram COMBOS de pontos e aceleram os desafios! Erros quebram o combo.
                  </p>
                </FloatingCard>
              </div>

              <button
                onClick={() => setAboutOpen(false)}
                className="w-full max-w-xs mx-auto min-h-[56px] bg-[#6D3DF5] hover:bg-[#5124D6] text-white rounded-2xl font-black text-xs uppercase tracking-wider cursor-pointer shadow-btn transition-all flex items-center justify-center gap-1"
              >
                <span>ENTENDI, VAMOS LÁ</span>
                <ArrowRight className="w-3.5 h-3.5" />
              </button>
            </motion.div>
          )}
        </AnimatePresence>
      </main>

      {/* Splash Screen — official TEMPOX branding intro */}
      <AnimatePresence>
        {showSplash && <SplashScreen />}
      </AnimatePresence>
    </div>
  );
}
