/**
 * @license
 * SPDX-License-Identifier: Apache-2.0
 */

export class SeededRandom {
  private seed: number;

  constructor(seed: number) {
    // Ensure we have a valid numeric seed, fallback to a random integer if 0 or invalid
    this.seed = seed || Math.floor(Math.random() * 10000000);
  }

  /**
   * Generates a pseudo-random number between 0 (inclusive) and 1 (exclusive)
   * using the Mulberry32 algorithm.
   */
  next(): number {
    let t = (this.seed += 0x6d2b79f5);
    t = Math.imul(t ^ (t >>> 15), t | 1);
    t ^= t + Math.imul(t ^ (t >>> 7), t | 61);
    return ((t ^ (t >>> 14)) >>> 0) / 4294967296;
  }

  /**
   * Generates a random float within the range [min, max).
   */
  float(min: number, max: number): number {
    return min + this.next() * (max - min);
  }

  /**
   * Generates a random integer within the range [min, max] (inclusive).
   */
  int(min: number, max: number): number {
    return Math.floor(this.float(min, max + 1));
  }

  /**
   * Picks a random element from an array.
   */
  choice<T>(arr: T[]): T {
    if (arr.length === 0) {
      throw new Error("Cannot pick from an empty array");
    }
    const idx = this.int(0, arr.length - 1);
    return arr[idx];
  }

  /**
   * Shuffles an array in place deterministically.
   */
  shuffle<T>(arr: T[]): T[] {
    const newArr = [...arr];
    for (let i = newArr.length - 1; i > 0; i--) {
      const j = this.int(0, i);
      const temp = newArr[i];
      newArr[i] = newArr[j];
      newArr[j] = temp;
    }
    return newArr;
  }
}
