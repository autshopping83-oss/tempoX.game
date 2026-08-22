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
} from "./gameEngine";

export type GameState = "HOME" | "PLAYING" | "PAUSED" | "GAMEOVER" | "ACHIEVEMENTS" | "STATS";

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
  const previousTypesRef = useRef<ChallengeType[]>([]);

  // Ref timers to avoid React state delay in the game loop
  const timerRef = useRef<number | null>(null);
  const lastTickRef = useRef<number>(0);
  const accumulatedTimeRef = useRef<number>(0); // how much of the 60s has passed (ms)
  const challengeTimeAccumulatedRef = useRef<number>(0); // how long has the active challenge been running (ms)
  const activeChallengeDurationRef = useRef<number>(0); // allowed duration (ms)

  // Track session details for achievements
  const sessionPerfectReflexesRef = useRef(0);
  const sessionMaxMemorySeqRef = useRef(0);

  // Load stats & settings from localStorage on mount
  useEffect(() => {
    if (typeof window !== "undefined") {
      const savedStats = localStorage.getItem("60s_game_stats");
      if (savedStats) {
        try {
          setStats(JSON.parse(savedStats));
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
   * Challenge scheduler: avoids repeats and picks the next challenge type.
   */
  const getNextChallengeType = useCallback((): ChallengeType => {
    const types: ChallengeType[] = ["MEMORY", "REFLEX", "MATH", "ATTENTION"];
    if (!rngRef.current) return "MATH";

    const last = previousTypesRef.current[previousTypesRef.current.length - 1];
    const secondLast = previousTypesRef.current[previousTypesRef.current.length - 2];

    // Filter out types to maintain high variety
    let pool = types.filter((t) => t !== last);
    if (pool.length === 0) pool = types;

    // Avoid alternating MEMORY -> REFLEX -> MEMORY -> REFLEX too quickly if possible
    if (secondLast && pool.includes(secondLast) && pool.length > 1) {
      pool = pool.filter((t) => t !== secondLast);
    }

    const nextType = rngRef.current.choice(pool);
    
    // Maintain history of past 4 items
    previousTypesRef.current.push(nextType);
    if (previousTypesRef.current.length > 4) {
      previousTypesRef.current.shift();
    }

    return nextType;
  }, []);

  /**
   * Generates and presents the next challenge.
   */
  const loadNextChallenge = useCallback(() => {
    if (!rngRef.current) return;

    const diffParams = difficultyRef.current.getDifficultyParams();
    const type = getNextChallengeType();
    const challenge = generateChallenge(type, rngRef.current, diffParams);

    setCurrentChallenge(challenge);
    challengeTimeAccumulatedRef.current = 0;
    activeChallengeDurationRef.current = challenge.duration * 1000;
    setChallengeTimeLeft(challenge.duration);
  }, [getNextChallengeType]);

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

    // 2. Progress active challenge time
    if (currentChallenge) {
      challengeTimeAccumulatedRef.current += delta;
      const chTimeLeft = Math.max(0, (activeChallengeDurationRef.current - challengeTimeAccumulatedRef.current) / 1000);
      setChallengeTimeLeft(chTimeLeft);

      if (challengeTimeAccumulatedRef.current >= activeChallengeDurationRef.current) {
        // Timeout counts as failure
        handleChallengeResult(false, 0);
      }
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
    const ratioSolved = timeTakenMs / allowedTimeMs;

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

      // Adaptive difficulty progression
      difficultyRef.current.recordResult(true, ratioSolved);
    } else {
      // Mistake or timeout
      setCombo(0);
      sound.playIncorrect();
      sound.vibrate([60, 40, 60]);

      // Adaptive difficulty degradation
      difficultyRef.current.recordResult(false, ratioSolved);
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
    previousTypesRef.current = [];

    // Reset runtime states
    setScore(0);
    setCombo(0);
    setMaxComboSession(0);
    setGameTimeLeft(60);
    accumulatedTimeRef.current = 0;
    challengeTimeAccumulatedRef.current = 0;

    // Reset session trackers
    setXpGainedSession(0);
    setChallengesCompletedSession(0);
    setCorrectAnswersSession(0);
    sessionPerfectReflexesRef.current = 0;
    sessionMaxMemorySeqRef.current = 0;

    setGameState("PLAYING");
    
    // Instantly queue the first challenge
    const type = "REFLEX"; // start with simple tactile reflex
    const diffParams = difficultyRef.current.getDifficultyParams();
    const challenge = generateChallenge(type, rngRef.current, diffParams);
    
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
  };
}
