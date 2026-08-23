/**
 * @license
 * SPDX-License-Identifier: Apache-2.0
 */

/**
 * Fixed difficulty progression — mirrors native GameEngine.kt:
 *   level = min(10, 1 + totalCorrect / 5)   (integer division, never decreases)
 */
export class DifficultyManager {
  private totalCorrect: number = 0;

  constructor() {
    this.reset();
  }

  reset() {
    this.totalCorrect = 0;
  }

  getLevel(): number {
    return Math.min(10, 1 + Math.floor(this.totalCorrect / 5));
  }

  /** Called by the game engine on every challenge completed. */
  recordResult(correct: boolean) {
    if (correct) {
      this.totalCorrect++;
    }
  }
}
