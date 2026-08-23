/**
 * @license
 * SPDX-License-Identifier: Apache-2.0
 */

import { useState, useEffect, useRef, useCallback } from "react";
import { SeededRandom } from "./random";
import { DifficultyManager } from "./difficulty";
import { sound } from "./sound";
import {
  ChallengeInstance,
  ChallengeType,
  generateChallenge,
} from "../challenge/types";
import {
  GameStats,
  INITIAL_STATS,
  calculateChallengeScore,
  checkAchievements,
  getXPForLevel,
  migrateStats,
} from "./gameEngine";

export type GameState = "HOME" | "PLAYING" | "PAUSED" | "GAMEOVER" | "ACHIEVEMENTS" | "STATS";

const ALL_TYPES: ChallengeType[] = ["MEMORY", "REFLEX", "MATH", "ATTENTION"];

export function useGame() {
  // Game states
  const [gameState, setGameState] = useState<GameState>("HOME");
  const [score, setScore] = useState(0);
  const [combo, setCombo] = useState(0);
  const [maxComboSession, setMaxComboSession] = useState(0);
  const [seed, setSeed] = useState<number>(0);
  const [currentChallenge, setCurrentChallenge] = useState<ChallengeInstance | null>(null);

  // High-precision clock timers
  const [gameTimeLeft, setGameTimeLeft] = useState(60);
  const [challengeTimeLeft, setChallengeTimeLeft] = useState(0);

  // Statistics and settings
  const [stats, setStats] = useState<GameStats>(INITIAL_STATS);
  const [soundOn, setSoundOn] = useState(true);
  const [vibeOn, setVibeOn] = useState(true);

  // New unlocks in the current game
  const [newAchievementsUnlocked, setNewAchievementsUnlocked] = useState<string[]>([]);
  const [xpGainedSession, setXpGainedSession] = useState(0);
  const [challengesCompletedSession, setChallengesCompletedSession] = useState(0);
  const [correctAnswersSession, setCorrectAnswersSession] = useState(0);

  // Core background instances
  const difficultyRef = useRef(new DifficultyManager());
  const rngRef = useRef<SeededRandom | null>(null);

  // Ref timers to avoid React state delay in the game loop
  const timerRef = useRef<number | null>(null);
  const lastTickRef = useRef<number>(0);
  const accumulatedTimeRef = useRef<number>(0); // how much of the 60s has passed (ms)
  const challengeTimeAccumulatedRef = useRef<number>(0); // how long has the active challenge been running (ms)
  const activeChallengeDurationRef = useRef<number>(0); // allowed duration (ms)

  /**
   * Freezes ONLY the per-challenge clock (native parity): during a Memory
   * watch phase the answer countdown does not run, exactly like the
   * `memoryWatching` flag in the Compose GameScreen.
   */
  const challengeClockPausedRef = useRef(false);

  // Track session details for achievements
  const sessionPerfectReflexesRef = useRef(0);
  const sessionMaxMemorySeqRef = useRef(0);

  // Load stats & settings from localStorage on mount
  useEffect(() => {
    if (typeof window !== "undefined") {
      const savedStats = localStorage.getItem("60s_game_stats");
      if (savedStats) {
        try {
          setStats(migrateStats(JSON.parse(savedStats)));
        } catch (e) {
          console.error("Failed to parse saved game stats:", e);
        }
      }
      setSoundOn(sound.getSoundEnabled());
      setVibeOn(sound.getVibrationEnabled());
    }
  }, []);

  // Save stats helper
  const saveStats = (updatedStats: GameStats) => {
    setStats(updatedStats);
    localStorage.setItem("60s_game_stats", JSON.stringify(updatedStats));
  };

  // Toggle settings
  const toggleSound = () => {
    const newVal = !soundOn;
    sound.setSoundEnabled(newVal);
    setSoundOn(newVal);
  };

  const toggleVibration = () => {
    const newVal = !vibeOn;
    sound.setVibrationEnabled(newVal);
    setVibeOn(newVal);
  };

  /**
   * Generates and presents the next challenge.
   * Type selection is uniform random — identical to native GameEngine.generate().
   */
  const loadNextChallenge = useCallback(() => {
    if (!rngRef.current) return;

    const level = difficultyRef.current.getLevel();
    const type = ALL_TYPES[rngRef.current.int(0, ALL_TYPES.length - 1)];
    const challenge = generateChallenge(type, rngRef.current, level);

    // A fresh challenge always starts with its clock running
    // (Memory re-freezes it when its watch phase mounts).
    challengeClockPausedRef.current = false;
    setCurrentChallenge(challenge);
    challengeTimeAccumulatedRef.current = 0;
    activeChallengeDurationRef.current = challenge.duration * 1000;
    setChallengeTimeLeft(challenge.duration);
  }, []);

  /**
   * Primary game over transition
   */
  const triggerGameOver = useCallback((completedEntireSession = true) => {
    if (timerRef.current) {
      cancelAnimationFrame(timerRef.current);
      timerRef.current = null;
    }

    setGameState("GAMEOVER");

    // Record session performance
    const totalXPToAdd = xpGainedSession;
    const finalScore = score;
    const finalMaxCombo = maxComboSession;

    let updatedXP = stats.totalXP + totalXPToAdd;
    let newLevel = stats.level;
    
    // Level up calculation based on cumulative XP
    while (updatedXP >= getXPForLevel(newLevel + 1)) {
      newLevel++;
    }

    // Prepare updated stats payload
    const updatedStats: GameStats = {
      ...stats,
      highScore: Math.max(stats.highScore, finalScore),
      totalXP: updatedXP,
      level: newLevel,
      gamesPlayed: stats.gamesPlayed + 1,
      totalCorrect: stats.totalCorrect + correctAnswersSession,
      totalIncorrect: stats.totalIncorrect + (challengesCompletedSession - correctAnswersSession),
      maxCombo: Math.max(stats.maxCombo, finalMaxCombo),
      maxMemorySequence: Math.max(stats.maxMemorySequence, sessionMaxMemorySeqRef.current),
      perfectReflexCount: stats.perfectReflexCount + sessionPerfectReflexesRef.current,
    };

    // Evaluate and unlock achievements
    const newlyUnlocked = checkAchievements(updatedStats, {
      maxCombo: finalMaxCombo,
      perfectReflexCount: sessionPerfectReflexesRef.current,
      maxMemorySequence: sessionMaxMemorySeqRef.current,
      score: finalScore,
      completed: completedEntireSession,
    });

    if (newlyUnlocked.length > 0) {
      updatedStats.achievements = [...updatedStats.achievements, ...newlyUnlocked];
      setNewAchievementsUnlocked(newlyUnlocked);
      sound.playRecord(); // Triumph sound!
    } else {
      setNewAchievementsUnlocked([]);
      sound.playCorrect(); // Standard game over chime
    }

    saveStats(updatedStats);
  }, [
    score,
    maxComboSession,
    xpGainedSession,
    challengesCompletedSession,
    correctAnswersSession,
    stats,
  ]);

  /**
   * High-accuracy monotonic loop using requestAnimationFrame
   */
  const gameLoop = useCallback((timestamp: number) => {
    if (gameState !== "PLAYING") return;

    if (!lastTickRef.current) {
      lastTickRef.current = timestamp;
    }

    const delta = timestamp - lastTickRef.current;
    lastTickRef.current = timestamp;

    // 1. Progress general game time (60 seconds)
    accumulatedTimeRef.current += delta;
    const rawTimeLeft = 60 - accumulatedTimeRef.current / 1000;
    
    if (rawTimeLeft <= 0) {
      setGameTimeLeft(0);
      triggerGameOver(true);
      return;
    } else {
      const prevSeconds = Math.ceil(60 - (accumulatedTimeRef.current - delta) / 1000);
      const currSeconds = Math.ceil(rawTimeLeft);
      
      // Heartbeat ticks during final 5 seconds
      if (currSeconds !== prevSeconds && currSeconds <= 5) {
        sound.playTick(currSeconds === 1);
      }
      setGameTimeLeft(rawTimeLeft);
    }

    // 2. Progress active challenge time (frozen during Memory watch phase)
    if (currentChallenge && !challengeClockPausedRef.current) {
      challengeTimeAccumulatedRef.current += delta;
      const chTimeLeft = Math.max(0, (activeChallengeDurationRef.current - challengeTimeAccumulatedRef.current) / 1000);
      setChallengeTimeLeft(chTimeLeft);

      if (challengeTimeAccumulatedRef.current >= activeChallengeDurationRef.current) {
        // Timeout counts as failure
        handleChallengeResult(false, 0);
      }
    } else if (currentChallenge) {
      const chTimeLeft = Math.max(0, (activeChallengeDurationRef.current - challengeTimeAccumulatedRef.current) / 1000);
      setChallengeTimeLeft(chTimeLeft);
    }

    timerRef.current = requestAnimationFrame(gameLoop);
  }, [gameState, currentChallenge, triggerGameOver]);

  // Handle requestAnimationFrame startup & cleanup
  useEffect(() => {
    if (gameState === "PLAYING") {
      lastTickRef.current = 0;
      timerRef.current = requestAnimationFrame(gameLoop);
    } else {
      if (timerRef.current) {
        cancelAnimationFrame(timerRef.current);
        timerRef.current = null;
      }
    }

    return () => {
      if (timerRef.current) {
        cancelAnimationFrame(timerRef.current);
      }
    };
  }, [gameState, gameLoop]);

  /**
   * Submits action result for the current active challenge.
   */
  const handleChallengeResult = useCallback((success: boolean, detailValue?: number) => {
    if (gameState !== "PLAYING" || !currentChallenge) return;

    const timeTakenMs = challengeTimeAccumulatedRef.current;
    const allowedTimeMs = activeChallengeDurationRef.current;

    setChallengesCompletedSession((prev) => prev + 1);

    if (success) {
      setCorrectAnswersSession((prev) => prev + 1);
      
      // Update combo state
      const nextCombo = combo + 1;
      setCombo(nextCombo);
      setMaxComboSession((prev) => Math.max(prev, nextCombo));

      // Calculate score and XP
      const currentLevel = difficultyRef.current.getLevel();
      const { score: scoreEarned, xp: xpEarned } = calculateChallengeScore({
        level: currentLevel,
        timeTaken: timeTakenMs / 1000,
        totalTime: allowedTimeMs / 1000,
        combo: nextCombo,
      });

      setScore((prev) => prev + scoreEarned);
      setXpGainedSession((prev) => prev + xpEarned);

      // Play audio and vibrations
      if (nextCombo > 0 && nextCombo % 3 === 0) {
        sound.playCombo(nextCombo);
      } else {
        sound.playCorrect();
      }
      sound.vibrate(35);

      // Save milestone details for achievements
      if (currentChallenge.type === "REFLEX" && timeTakenMs < 250) {
        sessionPerfectReflexesRef.current += 1;
      }
      if (currentChallenge.type === "MEMORY" && detailValue && detailValue > sessionMaxMemorySeqRef.current) {
        sessionMaxMemorySeqRef.current = detailValue;
      }

      // Adaptive difficulty progression (fixed formula — native parity)
      difficultyRef.current.recordResult(true);
    } else {
      // Mistake or timeout
      setCombo(0);
      sound.playIncorrect();
      sound.vibrate([60, 40, 60]);

      difficultyRef.current.recordResult(false);
    }

    // Advance loop
    loadNextChallenge();
  }, [gameState, currentChallenge, combo, loadNextChallenge]);

  /**
   * Initializes a brand new game match.
   */
  const startGame = useCallback((customSeed?: number) => {
    // Instantiate seed
    const activeSeed = customSeed || Math.floor(10000000 + Math.random() * 89999999);
    setSeed(activeSeed);

    // Initialize deterministic generator
    rngRef.current = new SeededRandom(activeSeed);
    difficultyRef.current.reset();

    // Reset runtime states
    setScore(0);
    setCombo(0);
    setMaxComboSession(0);
    setGameTimeLeft(60);
    accumulatedTimeRef.current = 0;
    challengeTimeAccumulatedRef.current = 0;
    challengeClockPausedRef.current = false;

    // Reset session trackers
    setXpGainedSession(0);
    setChallengesCompletedSession(0);
    setCorrectAnswersSession(0);
    sessionPerfectReflexesRef.current = 0;
    sessionMaxMemorySeqRef.current = 0;

    setGameState("PLAYING");

    // Instantly queue the first challenge — uniform random, like native.
    const level = difficultyRef.current.getLevel();
    const type = ALL_TYPES[rngRef.current.int(0, ALL_TYPES.length - 1)];
    const challenge = generateChallenge(type, rngRef.current, level);

    setCurrentChallenge(challenge);
    activeChallengeDurationRef.current = challenge.duration * 1000;
    setChallengeTimeLeft(challenge.duration);
  }, []);

  /**
   * Pauses / Resumes the active gameplay
   */
  const pauseGame = useCallback(() => {
    if (gameState === "PLAYING") {
      setGameState("PAUSED");
    }
  }, [gameState]);

  const resumeGame = useCallback(() => {
    if (gameState === "PAUSED") {
      setGameState("PLAYING");
    }
  }, [gameState]);

  /**
   * Quits match early
   */
  const quitGame = useCallback(() => {
    setGameState("HOME");
    setCurrentChallenge(null);
    challengeClockPausedRef.current = false;
  }, []);

  /**
   * Freeze/unfreeze the per-challenge countdown (Memory watch phase).
   */
  const setChallengeClockPaused = useCallback((paused: boolean) => {
    challengeClockPausedRef.current = paused;
  }, []);

  return {
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
    difficultyLevel: currentChallenge ? currentChallenge.difficultyLevel : difficultyRef.current.getLevel(),
    toggleSound,
    toggleVibration,
    startGame,
    pauseGame,
    resumeGame,
    quitGame,
    handleChallengeResult,
    setChallengeClockPaused,
  };
}
