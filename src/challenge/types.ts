/**
 * @license
 * SPDX-License-Identifier: Apache-2.0
 */

import { SeededRandom } from "../core/random";
import { ChallengeDifficulty } from "../core/difficulty";

export type ChallengeType = "MEMORY" | "REFLEX" | "MATH" | "ATTENTION";

export interface BaseChallengeInstance {
  id: string;
  type: ChallengeType;
  duration: number; // Duration limit in seconds
  difficultyLevel: number;
}

export interface MemoryChallengeInstance extends BaseChallengeInstance {
  type: "MEMORY";
  sequence: string[]; // e.g. ["#A855F7", "#3B82F6", "#22C55E", "#FACC15"] (represented by color names or ids)
  options: string[];  // full set of options for the user to pick from
  flashTimeMs: number; // time to flash each symbol
}

export interface ReflexTarget {
  id: string;
  x: number; // % from left (10 - 90)
  y: number; // % from top (15 - 85)
  isDecoy: boolean; // if clicked, counts as failure!
  label?: string; // "CLIQUE" vs "NÃO CLIQUE"
  size: number; // width/height in px
}

export interface ReflexChallengeInstance extends BaseChallengeInstance {
  type: "REFLEX";
  targets: ReflexTarget[];
  maxClicksRequired: number; // usually 1 or 2 high speed taps
}

export interface MathChallengeInstance extends BaseChallengeInstance {
  type: "MATH";
  equation: string;
  correctAnswer: number;
  options: number[];
}

export interface AttentionItem {
  id: string;
  label: string; // e.g. "O" or "🔵"
  color: string; // Tailwind hex or class color
  rotation?: number; // degree
}

export interface AttentionChallengeInstance extends BaseChallengeInstance {
  type: "ATTENTION";
  items: AttentionItem[];
  oddIndex: number;
  question: string; // e.g. "Qual é diferente?" ou "Toque na cor intrusa"
}

export type ChallengeInstance =
  | MemoryChallengeInstance
  | ReflexChallengeInstance
  | MathChallengeInstance
  | AttentionChallengeInstance;

/**
 * Creates a unique deterministic ChallengeInstance based on a random seed,
 * the chosen challenge type, and the active difficulty parameters.
 */
export function generateChallenge(
  type: ChallengeType,
  rng: SeededRandom,
  diff: ChallengeDifficulty
): ChallengeInstance {
  const id = `ch_${rng.int(100000, 999999)}`;
  const duration = diff.reactionWindow;

  switch (type) {
    case "MEMORY": {
      // Available symbols for memory
      const allColors = ["PURPLE", "BLUE", "GREEN", "YELLOW", "RED"];
      // Choose a sequence size based on difficulty/complexity
      const length = Math.max(3, Math.min(8, 2 + Math.floor(diff.complexity / 1.5)));

      // Round palette: exactly 4 colors (fits the 2x2 answer grid) and
      // guaranteed to contain every color the sequence can use.
      const palette = [...allColors];
      palette.splice(rng.int(0, palette.length - 1), 1);

      const sequence: string[] = [];
      for (let i = 0; i < length; i++) {
        sequence.push(rng.choice(palette));
      }

      // Flash duration depends on speed
      const flashTimeMs = Math.max(250, 800 - (diff.speed - 1) * 150);

      return {
        id,
        type: "MEMORY",
        duration: duration + (length * flashTimeMs) / 1000 + 1.5, // add presentation buffer
        difficultyLevel: diff.level,
        sequence,
        options: palette,
        flashTimeMs,
      };
    }

    case "REFLEX": {
      const targets: ReflexTarget[] = [];
      // Always have 1 primary target, plus potential decoys if distraction is enabled
      const decoyCount = Math.min(3, diff.distraction);
      
      // Spawn primary target
      targets.push({
        id: "target_primary",
        x: rng.int(15, 85),
        y: rng.int(20, 80),
        isDecoy: false,
        label: "⚡",
        size: Math.max(55, 90 - diff.level * 2), // gets smaller with higher difficulty
      });

      // Spawn decoys
      for (let i = 0; i < decoyCount; i++) {
        // Keep generating until we don't overlap too much
        let x = rng.int(15, 85);
        let y = rng.int(20, 80);
        // Quick fallback check to prevent stacking directly on top
        if (Math.abs(x - targets[0].x) < 15 && Math.abs(y - targets[0].y) < 15) {
          x = (x + 30) % 80 + 10;
          y = (y + 30) % 80 + 10;
        }

        targets.push({
          id: `decoy_${i}`,
          x,
          y,
          isDecoy: true,
          label: "🔴",
          size: Math.max(50, 85 - diff.level * 2),
        });
      }

      // Scurry order of target display
      const shuffledTargets = rng.shuffle(targets);

      return {
        id,
        type: "REFLEX",
        duration: Math.max(0.7, duration * 0.9), // Reflex challenges are extra fast
        difficultyLevel: diff.level,
        targets: shuffledTargets,
        maxClicksRequired: 1,
      };
    }

    case "MATH": {
      let a = 0;
      let b = 0;
      let op = "+";
      let equation = "";
      let correctAnswer = 0;

      const lvl = diff.complexity; // 1 to 10
      if (lvl <= 2) {
        // Simple 1-digit or small 2-digit sums
        a = rng.int(2, 15);
        b = rng.int(2, 15);
        op = "+";
        correctAnswer = a + b;
      } else if (lvl <= 4) {
        // Subtractions or medium sums
        a = rng.int(20, 50);
        b = rng.int(5, 25);
        op = rng.choice(["+", "-"]);
        if (op === "+") {
          correctAnswer = a + b;
        } else {
          correctAnswer = a - b;
        }
      } else if (lvl <= 6) {
        // Harder sums/subtractions or easy multiplications
        const choice = rng.choice(["add_sub", "mul"]);
        if (choice === "add_sub") {
          a = rng.int(40, 99);
          b = rng.int(15, 60);
          op = rng.choice(["+", "-"]);
          correctAnswer = op === "+" ? a + b : a - b;
        } else {
          a = rng.int(2, 9);
          b = rng.int(3, 9);
          op = "×";
          correctAnswer = a * b;
        }
      } else if (lvl <= 8) {
        // Multiplication and easy divisions
        const choice = rng.choice(["mul", "div"]);
        if (choice === "mul") {
          a = rng.int(6, 12);
          b = rng.int(5, 12);
          op = "×";
          correctAnswer = a * b;
        } else {
          // Division generated by multiplying first to keep integer answer
          b = rng.int(3, 10);
          correctAnswer = rng.int(3, 12);
          a = b * correctAnswer;
          op = "÷";
        }
      } else {
        // Multi-step complex equations or triple sums
        a = rng.int(5, 15);
        b = rng.int(3, 8);
        const c = rng.int(10, 30);
        // Format: (a * b) + c or (a * b) - c
        const op2 = rng.choice(["+", "-"]);
        equation = `(${a} × ${b}) ${op2} ${c}`;
        correctAnswer = op2 === "+" ? (a * b) + c : (a * b) - c;
      }

      if (!equation) {
        equation = `${a} ${op} ${b}`;
      }

      // Generate options (including correct answer and distinct wrong options)
      const optionsSet = new Set<number>();
      optionsSet.add(correctAnswer);

      // Distractors
      const distractorCount = Math.min(5, 2 + Math.floor(diff.objectCount / 2));
      while (optionsSet.size < distractorCount) {
        const offset = rng.choice([-10, -5, -2, -1, 1, 2, 5, 10, rng.int(-20, 20)]);
        const distVal = correctAnswer + offset;
        if (distVal > 0 && distVal !== correctAnswer) {
          optionsSet.add(distVal);
        }
      }

      const options = rng.shuffle(Array.from(optionsSet));

      return {
        id,
        type: "MATH",
        duration: Math.max(1.2, duration * 1.3), // Math gets a bit more time
        difficultyLevel: diff.level,
        equation,
        correctAnswer,
        options,
      };
    }

    case "ATTENTION": {
      // Find the intruder/odd one out.
      // We can use geometric symbols, letters, or emoji patterns.
      // E.g. grids of 🔴 with one 🔵, or grids of letters.
      const patterns = [
        { standard: "🔵", odd: "🔴", text: "Qual é a cor intrusa?" },
        { standard: "🟢", odd: "🟡", text: "Qual é o círculo intruso?" },
        { standard: "M", odd: "N", text: "Qual letra é diferente?" },
        { standard: "W", odd: "V", text: "Qual letra é diferente?" },
        { standard: "O", odd: "Q", text: "Qual letra é diferente?" },
        { standard: "E", odd: "F", text: "Qual letra é diferente?" },
        { standard: "8", odd: "B", text: "Qual caractere é diferente?" },
        { standard: "▲", odd: "▼", text: "Aponte o intruso" },
        { standard: "⭐", odd: "🌟", text: "Toque na estrela intrusa" },
        { standard: "🔲", odd: "🔳", text: "Ache o quadrado intruso" },
      ];

      const selectedPattern = rng.choice(patterns);

      // Determine size of the grid based on difficulty
      // Low difficulty: 2x2 or 3x3. High difficulty: 4x4.
      const totalItems = Math.min(16, Math.max(4, 3 + diff.objectCount));
      const items: AttentionItem[] = [];
      const oddIndex = rng.int(0, totalItems - 1);

      // Colors to apply
      const normalColor = "text-slate-200";
      
      for (let i = 0; i < totalItems; i++) {
        const isOdd = i === oddIndex;
        // Apply rotation to make it harder at high level
        const shouldRotate = diff.level >= 6 && rng.next() > 0.5;
        const rotation = shouldRotate ? rng.choice([90, 180, 270]) : undefined;

        items.push({
          id: `att_${i}`,
          label: isOdd ? selectedPattern.odd : selectedPattern.standard,
          color: isOdd ? "text-slate-100" : normalColor,
          rotation,
        });
      }

      return {
        id,
        type: "ATTENTION",
        duration: Math.max(1.0, duration * 1.1),
        difficultyLevel: diff.level,
        items,
        oddIndex,
        question: selectedPattern.text,
      };
    }

    default:
      throw new Error(`Unknown challenge type: ${type}`);
  }
}
