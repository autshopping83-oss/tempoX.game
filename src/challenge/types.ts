/**
 * @license
 * SPDX-License-Identifier: Apache-2.0
 */

import { SeededRandom } from "../core/random";

export type ChallengeType = "MEMORY" | "REFLEX" | "MATH" | "ATTENTION";

/**
 * Challenge instances mirror the native GameEngine.kt one-to-one so both
 * platforms deliver identical sizes, timings and progression.
 */

export interface BaseChallengeInstance {
  id: string;
  type: ChallengeType;
  duration: number; // answer window in seconds (watch phase excluded)
  difficultyLevel: number;
}

/** Watch a flashing sequence, then tap the shapes in order. */
export interface MemoryChallengeInstance extends BaseChallengeInstance {
  type: "MEMORY";
  sequence: number[]; // indices into MEMORY_SHAPES (0..5)
}

/** Tap the golden target fast; avoid red decoys (fixed 3x3 grid). */
export interface ReflexChallengeInstance extends BaseChallengeInstance {
  type: "REFLEX";
  targetCell: number; // 0..8
  decoyCells: number[];
}

/** Multiple-choice equation. */
export interface MathChallengeInstance extends BaseChallengeInstance {
  type: "MATH";
  equation: string;
  correctAnswer: number;
  options: number[]; // exactly 4, shuffled
}

/** Grid of identical symbols with one odd-one-out. */
export interface AttentionChallengeInstance extends BaseChallengeInstance {
  type: "ATTENTION";
  cols: number;
  rows: number;
  oddIndex: number;
  baseSymbol: string;
  oddSymbol: string;
}

export type ChallengeInstance =
  | MemoryChallengeInstance
  | ReflexChallengeInstance
  | MathChallengeInstance
  | AttentionChallengeInstance;

/** Shared shape palette (indices are stable across platforms). */
export const MEMORY_SHAPES = ["🔺", "🟦", "⭐", "🟢", "🔶", "🟣"];
export const MEMORY_TINTS = [
  "#EF4444", "#3B82F6", "#F59E0B",
  "#22C55E", "#F97316", "#A855F7",
];

/** Odd-one-out symbol pairs (same order as GameEngine.kt genAttention). */
const ATTENTION_PAIRS: Array<[string, string]> = [
  ["🔺", "🔻"], ["⭐", "🌟"], ["🔵", "🟣"],
  ["🟥", "🟧"], ["🌙", "🌛"], ["⚡", "🔥"],
];

let idCounter = 0;
function nextId(rng: SeededRandom): string {
  return `ch_${rng.int(100000, 999999)}_${idCounter++}`;
}

/**
 * Creates a deterministic ChallengeInstance using the exact same sizing
 * and timing formulas as the native GameEngine.kt:
 *
 *   MEMORY    len = min(9, 3 + (lv + 1) / 2)          limit = 8000 + len*400 ms
 *   REFLEX    grid 3x3, decoys min(3, 1 + lv/3)       limit = max(1200, 2600 - 160*lv)
 *   MATH      tiers lv<=2 add / lv<=4 sub-add / mult   limit = max(3000, 6000 - 250*lv)
 *             (lv>=7 may double operand a)
 *   ATTENTION side = min(6, 3 + lv/2)                 limit = max(2000, 4500 - 280*lv)
 */
export function generateChallenge(
  type: ChallengeType,
  rng: SeededRandom,
  level: number
): ChallengeInstance {
  switch (type) {
    case "MEMORY": {
      const length = Math.min(9, 3 + Math.floor((level + 1) / 2));
      const sequence: number[] = [];
      for (let i = 0; i < length; i++) {
        sequence.push(rng.int(0, MEMORY_SHAPES.length - 1));
      }
      return {
        id: nextId(rng),
        type: "MEMORY",
        duration: (8000 + length * 400) / 1000,
        difficultyLevel: level,
        sequence,
      };
    }

    case "REFLEX": {
      const targetCell = rng.int(0, 8);
      const pool = [0, 1, 2, 3, 4, 5, 6, 7, 8].filter((c) => c !== targetCell);
      const decoyCount = Math.min(3, 1 + Math.floor(level / 3));
      const decoyCells = rng.shuffle(pool).slice(0, decoyCount);
      return {
        id: nextId(rng),
        type: "REFLEX",
        duration: Math.max(1200, 2600 - 160 * level) / 1000,
        difficultyLevel: level,
        targetCell,
        decoyCells,
      };
    }

    case "MATH": {
      let equation: string;
      let answer: number;

      if (level <= 2) {
        const a = rng.int(1, 19);
        const b = rng.int(1, 19);
        answer = a + b;
        equation = `${a} + ${b}`;
      } else if (level <= 4) {
        const a = rng.int(2, 50);
        if (rng.next() < 0.5) {
          const b = rng.int(1, a); // may yield 0 — identical to native
          answer = a - b;
          equation = `${a} − ${b}`;
        } else {
          const b = rng.int(1, 19);
          answer = a + b;
          equation = `${a} + ${b}`;
        }
      } else {
        let a = rng.int(2, 12);
        const b = rng.int(2, 12);
        if (level >= 7 && rng.next() < 0.5) a *= 2;
        answer = a * b;
        equation = `${a} × ${b}`;
      }

      // Exactly 4 distinct options around the answer (±1..9), like native.
      const optionsSet = new Set<number>([answer]);
      let guard = 0;
      while (optionsSet.size < 4 && guard++ < 60) {
        const delta = rng.int(1, 9);
        const candidate = answer + (rng.next() < 0.5 ? delta : -delta);
        optionsSet.add(candidate);
      }

      return {
        id: nextId(rng),
        type: "MATH",
        duration: Math.max(3000, 6000 - 250 * level) / 1000,
        difficultyLevel: level,
        equation,
        correctAnswer: answer,
        options: rng.shuffle(Array.from(optionsSet)),
      };
    }

    case "ATTENTION": {
      const side = Math.min(6, 3 + Math.floor(level / 2));
      const pair = ATTENTION_PAIRS[rng.int(0, ATTENTION_PAIRS.length - 1)];
      return {
        id: nextId(rng),
        type: "ATTENTION",
        duration: Math.max(2000, 4500 - 280 * level) / 1000,
        difficultyLevel: level,
        cols: side,
        rows: side,
        oddIndex: rng.int(0, side * side - 1),
        baseSymbol: pair[0],
        oddSymbol: pair[1],
      };
    }
  }
}
