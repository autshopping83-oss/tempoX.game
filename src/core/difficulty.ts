/**
 * @license
 * SPDX-License-Identifier: Apache-2.0
 */

export interface ChallengeDifficulty {
  level: number;
  speed: number;          // multiplier for animations/displays (e.g. 1.0 - 3.0)
  objectCount: number;    // number of items/options (e.g. 3 - 8)
  reactionWindow: number; // time limit in seconds for action (e.g. 4.0s down to 0.7s)
  distraction: number;    // count or presence of misleading elements (e.g. 0 - 3)
  complexity: number;     // math complexity level, memory pattern length (e.g. 1 - 10)
}

export class DifficultyManager {
  private level: number = 1;
  private consecutiveCorrect: number = 0;
  private consecutiveIncorrect: number = 0;

  constructor() {
    this.reset();
  }

  reset() {
    this.level = 1;
    this.consecutiveCorrect = 0;
    this.consecutiveIncorrect = 0;
  }

  getLevel(): number {
    return this.level;
  }

  setLevel(val: number) {
    this.level = Math.max(1, Math.min(val, 15));
  }

  /**
   * Adaptive algorithm. Called by game engine on every challenge completed.
   * @param correct Whether the player solved it correctly
   * @param ratioSolved Time taken divided by time allowed (lower is faster, excellent < 0.4)
   */
  recordResult(correct: boolean, ratioSolved: number) {
    if (correct) {
      this.consecutiveIncorrect = 0;
      this.consecutiveCorrect++;

      // Rapidly increase level if performance is perfect and fast
      if (this.consecutiveCorrect >= 3 || (this.consecutiveCorrect >= 1 && ratioSolved < 0.35)) {
        this.level = Math.min(15, this.level + 1);
        this.consecutiveCorrect = 0;
      }
    } else {
      this.consecutiveCorrect = 0;
      this.consecutiveIncorrect++;

      // Reduce level if failing multiple times
      if (this.consecutiveIncorrect >= 2) {
        this.level = Math.max(1, this.level - 1);
        this.consecutiveIncorrect = 0;
      }
    }
  }

  /**
   * Generates parameters for the current level.
   */
  getDifficultyParams(): ChallengeDifficulty {
    const lvl = this.level;

    // Calculations based on level 1 to 15
    const speed = parseFloat((1.0 + (lvl - 1) * 0.15).toFixed(2)); // 1.0 to 3.1
    const objectCount = Math.min(8, 3 + Math.floor((lvl - 1) / 2)); // 3 to 8
    const reactionWindow = parseFloat(Math.max(0.65, 4.5 - (lvl - 1) * 0.3).toFixed(2)); // 4.5s down to 0.65s
    const distraction = Math.min(4, Math.floor((lvl - 1) / 3)); // 0 to 4
    const complexity = Math.min(10, 1 + Math.floor((lvl - 1) * 0.8)); // 1 to 10

    return {
      level: lvl,
      speed,
      objectCount,
      reactionWindow,
      distraction,
      complexity,
    };
  }
}
