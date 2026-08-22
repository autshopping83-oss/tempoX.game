/**
 * @license
 * SPDX-License-Identifier: Apache-2.0
 */

import { ChallengeInstance, ChallengeType } from "../challenge/types";

export interface GameStats {
  highScore: number;
  totalXP: number;
  level: number;
  gamesPlayed: number;
  totalCorrect: number;
  totalIncorrect: number;
  maxCombo: number;
  achievements: string[]; // IDs of unlocked achievements
}

export interface Achievement {
  id: string;
  title: string;
  description: string;
  icon: string;
}

export const GAME_ACHIEVEMENTS: Achievement[] = [
  {
    id: "elefante",
    icon: "🧠",
    title: "Memória de Elefante",
    description: "Completar sequência de 7 ou mais símbolos na Memória.",
  },
  {
    id: "reflexo",
    icon: "⚡",
    title: "Reflexo Perfeito",
    description: "Responder um desafio de reflexo em menos de 0.25 segundos.",
  },
  {
    id: "imparavel",
    icon: "🔥",
    title: "Imparável",
    description: "Alcançar Combo x10 durante uma partida.",
  },
  {
    id: "sobrevivente",
    icon: "⏱️",
    title: "Sobrevivente",
    description: "Completar uma partida inteira de 60 segundos.",
  },
  {
    id: "recordista",
    icon: "🏆",
    title: "Recordista",
    description: "Superar o seu próprio recorde histórico de pontuação.",
  },
];

export const INITIAL_STATS: GameStats = {
  highScore: 0,
  totalXP: 0,
  level: 1,
  gamesPlayed: 0,
  totalCorrect: 0,
  totalIncorrect: 0,
  maxCombo: 0,
  achievements: [],
};

/**
 * Calculates XP threshold required to reach a specific level.
 */
export function getXPForLevel(level: number): number {
  if (level <= 1) return 0;
  return Math.round(150 * Math.pow(level - 1, 1.6));
}

/**
 * Evaluates current profile to unlock achievements.
 * Returns array of newly unlocked achievement IDs.
 */
export function checkAchievements(
  stats: GameStats,
  currentSession: {
    maxCombo: number;
    perfectReflexCount: number;
    maxMemorySequence: number;
    score: number;
    completed: boolean;
  }
): string[] {
  const newlyUnlocked: string[] = [];
  const existing = new Set(stats.achievements);

  // 1. Memória de Elefante
  if (!existing.has("elefante") && currentSession.maxMemorySequence >= 7) {
    newlyUnlocked.push("elefante");
  }

  // 2. Reflexo Perfeito
  if (!existing.has("reflexo") && currentSession.perfectReflexCount >= 1) {
    newlyUnlocked.push("reflexo");
  }

  // 3. Imparável
  if (!existing.has("imparavel") && (currentSession.maxCombo >= 10 || stats.maxCombo >= 10)) {
    newlyUnlocked.push("imparavel");
  }

  // 4. Sobrevivente
  if (!existing.has("sobrevivente") && currentSession.completed) {
    newlyUnlocked.push("sobrevivente");
  }

  // 5. Recordista
  if (!existing.has("recordista") && currentSession.score > stats.highScore && stats.highScore > 0) {
    newlyUnlocked.push("recordista");
  }

  return newlyUnlocked;
}

/**
 * Calculates score for a successful challenge.
 */
export function calculateChallengeScore(params: {
  level: number;
  timeTaken: number;
  totalTime: number;
  combo: number;
}): { score: number; xp: number } {
  const baseScore = 100;
  const difficultyMultiplier = 1 + (params.level - 1) * 0.15; // +15% per level
  const speedRatio = params.timeTaken / params.totalTime;
  const speedMultiplier = Math.max(1.0, 2.5 - speedRatio * 1.5); // faster = up to 2.5x multiplier
  const comboMultiplier = 1 + Math.min(2.0, params.combo * 0.1); // combo caps at +200% (x3.0)

  const score = Math.round(baseScore * difficultyMultiplier * speedMultiplier * comboMultiplier);
  const xp = Math.round(15 * difficultyMultiplier * (1 + params.combo * 0.05));

  return { score, xp };
}
