/**
 * @license
 * SPDX-License-Identifier: Apache-2.0
 */

import React from "react";
import { AttentionChallengeInstance } from "../types";
import { motion } from "motion/react";

interface Props {
  challenge: AttentionChallengeInstance;
  onSolve: (success: boolean) => void;
}

/**
 * Odd-one-out grid (side x side, 3..6) with emoji symbol pairs —
 * identical generation to the native AttentionHost.
 */
export default function AttentionChallenge({ challenge, onSolve }: Props) {
  const { cols, rows, oddIndex, baseSymbol, oddSymbol } = challenge;

  return (
    <div className="flex flex-col items-center justify-center w-full h-full max-w-sm mx-auto">
      <div className="w-full aspect-square p-2 bg-white/40 border border-slate-100 rounded-[20px] overflow-hidden select-none">
        <div
          className="grid w-full h-full gap-[4px]"
          style={{ gridTemplateColumns: `repeat(${cols}, minmax(0, 1fr))`, gridTemplateRows: `repeat(${rows}, minmax(0, 1fr))` }}
        >
          {Array.from({ length: cols * rows }, (_, index) => {
            const odd = index === oddIndex;
            return (
              <motion.button
                key={index}
                data-attention-cell={index}
                initial={{ opacity: 0 }}
                animate={{ opacity: 1 }}
                whileTap={{ scale: 0.92 }}
                onClick={() => onSolve(odd)}
                className="rounded-xl flex items-center justify-center cursor-pointer active:bg-slate-100 transition-colors"
              >
                <span
                  style={{ fontSize: "clamp(18px, 5.5vw, 28px)" }}
                  className="leading-none pointer-events-none"
                >
                  {odd ? oddSymbol : baseSymbol}
                </span>
              </motion.button>
            );
          })}
        </div>
      </div>
    </div>
  );
}
