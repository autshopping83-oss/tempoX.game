/**
 * @license
 * SPDX-License-Identifier: Apache-2.0
 */

import React from "react";
import { AttentionChallengeInstance } from "../types";
import { motion } from "motion/react";
import { GameTriangle, GameCircle, GameSquare } from "../../components/GeometricShapes";
import { GameTheme, GameColors } from "../../core/GameTheme";

interface Props {
  challenge: AttentionChallengeInstance;
  onSolve: (success: boolean) => void;
}

export default function AttentionChallenge({ challenge, onSolve }: Props) {
  const { items, oddIndex } = challenge;

  const handleItemClick = (index: number) => {
    onSolve(index === oddIndex);
  };

  const count = items.length;
  let gridColsClass = "grid-cols-2";
  if (count > 9) {
    gridColsClass = "grid-cols-4 gap-2";
  } else if (count > 4) {
    gridColsClass = "grid-cols-3 gap-3";
  } else {
    gridColsClass = "grid-cols-2 gap-4";
  }

  // Render clean premium vector shape if applicable, else render beautiful text button
  const renderShapeOrCharacter = (label: string, rotation?: number, isOdd?: boolean) => {
    const finalRot = (rotation || 0);
    
    if (label === "▲") {
      return <GameTriangle size={56} color={GameColors.primary} rotation={finalRot} animateIn={false} />;
    }
    if (label === "▼") {
      return <GameTriangle size={56} color={GameColors.primary} rotation={finalRot + 180} animateIn={false} />;
    }
    if (label === "🔵") {
      return <GameCircle size={56} color={GameColors.blue} rotation={finalRot} animateIn={false} />;
    }
    if (label === "🔴") {
      return <GameCircle size={56} color={GameColors.red} rotation={finalRot} animateIn={false} />;
    }
    if (label === "🟢") {
      return <GameCircle size={56} color={GameColors.green} rotation={finalRot} animateIn={false} />;
    }
    if (label === "🟡") {
      return <GameCircle size={56} color={GameColors.yellow} rotation={finalRot} animateIn={false} />;
    }
    if (label === "🔲") {
      return <GameSquare size={56} color={GameColors.primary} rotation={finalRot} animateIn={false} />;
    }
    if (label === "🔳") {
      return <GameSquare size={56} color={GameColors.pink} rotation={finalRot} animateIn={false} />;
    }

    // Default character rendering with high-end typography
    return (
      <span 
        style={{ transform: `rotate(${finalRot}deg)` }}
        className="text-4xl font-mono font-black text-slate-800 transition-transform select-none"
      >
        {label}
      </span>
    );
  };

  return (
    <div className="flex flex-col items-center justify-center w-full h-full max-w-sm mx-auto">
      {/* Floating interactive items grid */}
      <div className="w-full flex items-center justify-center py-2">
        <div className={`grid ${gridColsClass} w-full aspect-square p-3.5 bg-white border ${GameTheme.colors.borders.light}/80 ${GameTheme.shapes.card} ${GameTheme.shadows.premium} overflow-hidden`}>
          {items.map((item, idx) => {
            const isOddItem = idx === oddIndex;
            return (
              <motion.button
                key={item.id}
                initial={{ scale: 0.8, opacity: 0 }}
                animate={{ scale: 1, opacity: 1 }}
                transition={{ type: "spring", stiffness: 200, damping: 18, delay: idx * 0.02 }}
                whileTap={{ scale: 0.92 }}
                onClick={() => handleItemClick(idx)}
                className={`aspect-square rounded-2xl bg-slate-50 hover:bg-slate-100/50 border ${GameTheme.colors.borders.light} flex items-center justify-center ${GameTheme.shadows.soft} cursor-pointer transition-colors duration-150 relative select-none`}
              >
                {renderShapeOrCharacter(item.label, item.rotation, isOddItem)}
              </motion.button>
            );
          })}
        </div>
      </div>
    </div>
  );
}
