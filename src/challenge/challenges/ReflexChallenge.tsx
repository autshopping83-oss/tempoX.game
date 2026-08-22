/**
 * @license
 * SPDX-License-Identifier: Apache-2.0
 */

import React from "react";
import { ReflexChallengeInstance, ReflexTarget } from "../types";
import { motion } from "motion/react";
import { GameTarget } from "../../components/GeometricShapes";
import { GameTheme, GameColors } from "../../core/GameTheme";

interface Props {
  challenge: ReflexChallengeInstance;
  onSolve: (success: boolean) => void;
}

export default function ReflexChallenge({ challenge, onSolve }: Props) {
  const { targets } = challenge;

  const handleTargetClick = (target: ReflexTarget) => {
    if (target.isDecoy) {
      onSolve(false);
    } else {
      onSolve(true);
    }
  };

  return (
    <div className="flex flex-col items-center justify-center w-full h-full max-w-sm mx-auto">
      {/* Target spawning relative board */}
      <div className={`relative w-full aspect-square bg-white border ${GameTheme.colors.borders.light} ${GameTheme.shapes.card} ${GameTheme.shadows.premium} overflow-hidden select-none`}>
        {/* Soft elegant grid background */}
        <div className="absolute inset-0 bg-grid-light opacity-30" />

        {targets.map((target) => {
          const isPrimary = !target.isDecoy;
          
          return (
            <motion.div
              key={target.id}
              initial={{ scale: 0.7, opacity: 0 }}
              animate={{ scale: 1, opacity: 1 }}
              exit={{ scale: 0 }}
              transition={{ type: "spring", damping: 14, stiffness: 180 }}
              style={{
                position: "absolute",
                left: `${target.x}%`,
                top: `${target.y}%`,
                width: `${target.size}px`,
                height: `${target.size}px`,
                transform: "translate(-50%, -50%)",
              }}
              className="z-20 flex items-center justify-center"
            >
              {isPrimary ? (
                // Yellow Primary Target
                <GameTarget
                  size={target.size}
                  color="YELLOW"
                  animateIn={false}
                  onClick={() => handleTargetClick(target)}
                />
              ) : (
                // Danger Decoy target (Sleek red danger element)
                <motion.div
                  whileTap={{ scale: 0.9 }}
                  onClick={() => handleTargetClick(target)}
                  style={{ width: target.size, height: target.size }}
                  className="relative cursor-pointer bg-gradient-to-br from-[#EF4444] to-[#B91C1C] rounded-full flex flex-col items-center justify-center border border-white/50 shadow-lg shadow-red-500/20"
                >
                  {/* Inner design line */}
                  <div className="w-[80%] h-[80%] rounded-full border border-dashed border-white/20 flex flex-col items-center justify-center">
                    <span className="text-white text-lg font-black select-none pointer-events-none">✕</span>
                    <span className="text-[7px] text-white/80 font-black uppercase tracking-widest select-none pointer-events-none leading-none mt-0.5">PERIGO</span>
                  </div>
                </motion.div>
              )}
            </motion.div>
          );
        })}
      </div>
    </div>
  );
}
