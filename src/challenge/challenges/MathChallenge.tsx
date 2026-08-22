/**
 * @license
 * SPDX-License-Identifier: Apache-2.0
 */

import React from "react";
import { MathChallengeInstance } from "../types";
import { motion } from "motion/react";
import { GameTheme, GameColors } from "../../core/GameTheme";

interface Props {
  challenge: MathChallengeInstance;
  onSolve: (success: boolean) => void;
}

export default function MathChallenge({ challenge, onSolve }: Props) {
  const { equation, options, correctAnswer } = challenge;

  const handleOptionClick = (val: number) => {
    onSolve(val === correctAnswer);
  };

  return (
    <div className="flex flex-col items-center justify-center w-full h-full max-w-sm mx-auto">
      {/* Equation display panel */}
      <div className={`w-full h-28 flex items-center justify-center bg-white border ${GameTheme.colors.borders.light} ${GameTheme.shapes.card} mb-6 relative overflow-hidden ${GameTheme.shadows.premium}`}>
        <div className="absolute inset-0 bg-[#3B82F6]/5 filter blur-xl" />
        <h1 className="text-4xl md:text-5xl font-black text-slate-800 relative z-10 font-mono tracking-tight">
          {equation} = ?
        </h1>
      </div>

      {/* Multiple-choice grid options */}
      <div className="grid grid-cols-2 gap-4 w-full mb-2">
        {options.map((val, idx) => (
          <motion.button
            key={`${val}-${idx}`}
            initial={{ scale: 0.9, opacity: 0 }}
            animate={{ scale: 1, opacity: 1 }}
            transition={{ type: "spring", stiffness: 220, damping: 16, delay: idx * 0.03 }}
            whileTap={{ scale: 0.94 }}
            onClick={() => handleOptionClick(val)}
            className={`h-16 min-h-[64px] rounded-2xl bg-slate-50 hover:bg-slate-100/50 border border-slate-150 text-slate-800 font-mono text-2xl font-black flex items-center justify-center ${GameTheme.shadows.soft} active:scale-95 transition-all cursor-pointer hover:border-[#3B82F6]`}
          >
            {val}
          </motion.button>
        ))}
      </div>
    </div>
  );
}
