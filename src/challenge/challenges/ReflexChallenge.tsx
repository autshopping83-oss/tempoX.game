/**
 * @license
 * SPDX-License-Identifier: Apache-2.0
 */

import React from "react";
import { ReflexChallengeInstance } from "../types";

interface Props {
  challenge: ReflexChallengeInstance;
  onSolve: (success: boolean) => void;
}

/**
 * Fixed 3x3 grid — identical layout to the native ReflexHost:
 * one golden 🎯 target, red 💥 decoys, empty cells are transparent.
 */
export default function ReflexChallenge({ challenge, onSolve }: Props) {
  const { targetCell, decoyCells } = challenge;

  return (
    <div className="flex flex-col items-center justify-center w-full h-full max-w-sm mx-auto">
      <div className="relative w-full aspect-square select-none">
        <div className="absolute inset-0 grid grid-cols-3 grid-rows-3 gap-[10px]">
          {Array.from({ length: 9 }, (_, index) => {
            const isTarget = index === targetCell;
            const isDecoy = decoyCells.includes(index);

            return (
              <button
                key={index}
                data-reflex-cell={index}
                onClick={() => {
                  if (isTarget) onSolve(true);
                  else if (isDecoy) onSolve(false);
                }}
                aria-label={isTarget ? "target" : isDecoy ? "danger" : undefined}
                className="rounded-2xl flex items-center justify-center active:scale-95 transition-transform cursor-pointer"
                style={{
                  background: isTarget
                    ? "radial-gradient(circle at 35% 30%, #FFC93D, #F59E0B)"
                    : isDecoy
                      ? "radial-gradient(circle at 35% 30%, #F87171, #DC2626)"
                      : "transparent",
                  boxShadow: isTarget
                    ? "0 8px 22px -6px rgba(245, 158, 11, 0.55)"
                    : isDecoy
                      ? "0 8px 22px -6px rgba(220, 38, 38, 0.45)"
                      : "none",
                }}
              >
                {isTarget && (
                  <span style={{ fontSize: 32 }} className="leading-none pointer-events-none">
                    🎯
                  </span>
                )}
                {isDecoy && (
                  <span style={{ fontSize: 26 }} className="leading-none pointer-events-none">
                    💥
                  </span>
                )}
              </button>
            );
          })}
        </div>
      </div>
    </div>
  );
}
