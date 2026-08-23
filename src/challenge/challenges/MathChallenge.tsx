/**
 * @license
 * SPDX-License-Identifier: Apache-2.0
 */

import React from "react";
import { MathChallengeInstance } from "../types";
import { motion } from "motion/react";
import { GameTheme } from "../../core/GameTheme";

interface Props {
  challenge: MathChallengeInstance;
  onSolve: (success: boolean) => void;
}

/**
 * Multiple-choice equation — exactly 4 shuffled options in a 2x2 grid,
 * mirroring the native MathHost.
 */
export default function MathChallenge({ challenge, onSolve }: Props) {
  const { equation, options, correctAnswer } = challenge;

  const handleOptionClick = (val: number) => {
    onSolve(val === correctAnswer);
  };

  return (
    <div className="flex flex-col items-center justify-center w-full h-full max-w-sm mx-auto">
      {/* Equation display panel */}
      <div className={`w-full h-24 flex items-center justify-center bg-white border ${GameTheme.colors.borders.light} ${GameTheme.shapes.card} mb-5 relative overflow-hidden ${GameTheme.shadows.premium}`}>
        <div className="absolute inset-0 bg-[#3B82F6]/5 filter blur-xl" />
        <h1
          className="font-black text-slate-800 relative z-10 font-mono tracking-tight"
          style={{ fontSize: "clamp(28px, 9vw, 40px)" }}
        >
          {equation} = ?
        </h1>
      </div>

      {/* Multiple-choice grid options (2 columns) */}
      <div className="grid grid-cols-2 gap-3 w-full">
        {options.map((val, idx) => (
          <motion.button
            key={`${val}-${idx}`}
            initial={{ scale: 0.9, opacity: 0 }}
            animate={{ scale: 1, opacity: 1 }}
            transition={{ type: "spring", stiffness: 220, damping: 16, delay: idx * 0.03 }}
            whileTap={{ scale: 0.94 }}
            onClick={() => handleOptionClick(val)}
            className={`h-[62px] rounded-2xl bg-white border text-slate-800 font-mono font-black flex items-center justify-center shadow-soft active:scale-95 transition-all cursor-pointer hover:border-[#3B82F6]`}
            style={{ fontSize: 24, borderColor: "#E2E8F0" }}
          >
            {val}
          </motion.button>
        ))}
      </div>
    </div>
  );
}
