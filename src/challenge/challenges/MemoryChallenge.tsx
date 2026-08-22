/**
 * @license
 * SPDX-License-Identifier: Apache-2.0
 */

import React, { useState, useEffect } from "react";
import { MemoryChallengeInstance } from "../types";
import { motion } from "motion/react";
import { Check } from "lucide-react";
import { GameTriangle, GameCircle, GameSquare, GameDiamond } from "../../components/GeometricShapes";
import { gameFeel } from "../../core/gameFeel";

interface Props {
  challenge: MemoryChallengeInstance;
  onSolve: (success: boolean, sequenceLength: number) => void;
}

const INACTIVE_DOT = "#E9E4F8";

const SHAPE_MAP: Record<
  string,
  { label: string; color: string; renderShape: (size: number) => React.ReactNode }
> = {
  PURPLE: {
    label: "Triângulo",
    color: "#6D3DF5",
    renderShape: (s) => <GameTriangle size={s} color="#6D3DF5" animateIn={false} />,
  },
  BLUE: {
    label: "Círculo",
    color: "#3B82F6",
    renderShape: (s) => <GameCircle size={s} color="#3B82F6" animateIn={false} />,
  },
  GREEN: {
    label: "Quadrado",
    color: "#22C55E",
    renderShape: (s) => <GameSquare size={s} color="#22C55E" animateIn={false} />,
  },
  YELLOW: {
    label: "Losango",
    color: "#FACC15",
    renderShape: (s) => <GameDiamond size={s} color="#FACC15" animateIn={false} />,
  },
  RED: {
    label: "Alvo",
    color: "#EF4444",
    renderShape: (s) => <GameCircle size={s} color="#EF4444" animateIn={false} />,
  },
};

export default function MemoryChallenge({ challenge, onSolve }: Props) {
  const { sequence, options, flashTimeMs } = challenge;
  const [phase, setPhase] = useState<"WATCHING" | "PLAYING">("WATCHING");
  const [activeFlashIndex, setActiveFlashIndex] = useState<number>(-1);
  const [playerInput, setPlayerInput] = useState<string[]>([]);
  const [locked, setLocked] = useState(false);

  useEffect(() => {
    setPhase("WATCHING");
    setPlayerInput([]);
    setActiveFlashIndex(-1);
    setLocked(false);

    let idx = 0;
    const interval = setInterval(() => {
      if (idx < sequence.length) {
        setActiveFlashIndex(idx);
        setTimeout(() => {
          setActiveFlashIndex(-1);
        }, flashTimeMs * 0.7);
        idx++;
      } else {
        clearInterval(interval);
        setPhase("PLAYING");
      }
    }, flashTimeMs);

    return () => clearInterval(interval);
  }, [sequence, flashTimeMs]);

  const handleNodeClick = (color: string, el: HTMLElement) => {
    if (phase !== "PLAYING" || locked) return;

    const nextInput = [...playerInput, color];
    setPlayerInput(nextInput);

    const checkIndex = nextInput.length - 1;
    if (color === sequence[checkIndex]) {
      // Hit: green flash + punch on the pressed card
      gameFeel.flash(el, "green");
      gameFeel.punch(el);

      if (nextInput.length === sequence.length) {
        // Lock input instantly (visuals stay on the filled placeholders)
        // until the parent advances to the next challenge.
        setLocked(true);
        setTimeout(() => onSolve(true, sequence.length), 120);
      }
    } else {
      // Miss: shake + red flash on the wrong card
      gameFeel.shake(el);
      gameFeel.flash(el, "red");
      setTimeout(() => onSolve(false, sequence.length), 180);
    }
  };

  const watching = phase === "WATCHING";

  return (
    <div className="flex flex-col items-center justify-center w-full h-full">
      {/* Phase chip */}
      <div
        data-memory-phase={phase}
        className={`shrink-0 inline-flex items-center gap-1.5 px-3 py-1 rounded-full text-[10px] font-black uppercase tracking-widest select-none ${
          watching
            ? "bg-[#6D3DF5]/[0.07] text-[#6D3DF5]"
            : "bg-[#F59E0B]/[0.09] text-[#B45309]"
        }`}
      >
        <span>{watching ? "👀" : "✋"}</span>
        <span>{watching ? "OBSERVE A SEQUÊNCIA" : "SUA VEZ!"}</span>
      </div>

      {/* Stage: large sequence shapes */}
      <div className="shrink-0 h-14 mt-2 flex items-center justify-center gap-2 px-2">
        {watching
          ? sequence.map((color, idx) => {
              const meta = SHAPE_MAP[color];
              const isActive = idx === activeFlashIndex;
              return (
                <motion.div
                  key={`seq-${idx}`}
                  data-seq-item={color}
                  animate={{
                    scale: isActive ? 1.18 : 0.85,
                    opacity: isActive ? 1 : 0.3,
                  }}
                  transition={{ duration: 0.18 }}
                  className="w-11 h-11 rounded-xl flex items-center justify-center"
                  style={{
                    background: isActive ? `${meta.color}14` : "#FFFFFF",
                    border: `2px solid ${isActive ? meta.color : "#EEEAF7"}`,
                    boxShadow: isActive ? `0 0 16px ${meta.color}55` : "none",
                  }}
                >
                  {meta.renderShape(26)}
                </motion.div>
              );
            })
          : sequence.map((color, idx) => {
              const filled = idx < playerInput.length;
              return (
                <motion.div
                  key={`play-${idx}`}
                  initial={{ scale: 0.7, opacity: 0 }}
                  animate={{ scale: 1, opacity: 1 }}
                  transition={{ type: "spring", stiffness: 400, damping: 24 }}
                  className="w-9 h-9 rounded-xl flex items-center justify-center"
                  style={
                    filled
                      ? {
                          background: `${SHAPE_MAP[color].color}1A`,
                          border: `2px solid ${SHAPE_MAP[color].color}`,
                        }
                      : { border: "2px dashed #DDD6EE", background: "#FFFFFF" }
                  }
                >
                  {filled && (
                    <Check
                      className="w-4 h-4"
                      strokeWidth={3.5}
                      style={{ color: SHAPE_MAP[color].color }}
                    />
                  )}
                </motion.div>
              );
            })}
      </div>

      {/* Progress dots */}
      <div className="shrink-0 flex justify-center items-center gap-1.5 mt-2 mb-3 select-none">
        {sequence.map((color, idx) => {
          const lit = watching
            ? idx <= activeFlashIndex
            : idx < playerInput.length;
          const activeNow = watching && idx === activeFlashIndex;
          const c = SHAPE_MAP[color].color;
          return (
            <div
              key={`dot-${idx}`}
              className="w-3.5 h-3.5 rounded-full transition-all duration-150"
              style={{
                width: activeNow ? 17 : 14,
                height: activeNow ? 17 : 14,
                backgroundColor: lit || activeNow ? c : INACTIVE_DOT,
                boxShadow: activeNow ? `0 0 10px ${c}88` : "none",
              }}
            />
          );
        })}
      </div>

      {/* Answer cards */}
      <div className="grid grid-cols-2 gap-3 w-full shrink-0">
        {options.slice(0, 4).map((color, idx) => {
          const meta = SHAPE_MAP[color];
          return (
            <motion.button
              key={color}
              data-memory-option={color}
              disabled={watching || locked}
              onClick={(e) => handleNodeClick(color, e.currentTarget)}
              whileTap={watching || locked ? undefined : { scale: 0.96 }}
              initial={{ opacity: 0, y: 14 }}
              animate={{ opacity: 1, y: 0 }}
              transition={{
                delay: idx * 0.05,
                type: "spring",
                stiffness: 280,
                damping: 24,
              }}
              className="relative rounded-3xl border-2 flex flex-col items-center justify-center p-2 overflow-hidden"
              style={{
                minHeight: 104,
                background: watching ? "#FFFFFF" : "rgba(255,255,255,0.75)",
                borderColor: watching ? "#EEEAF7" : `${meta.color}40`,
              }}
            >
              {/* soft colored wash */}
              {!watching && (
                <div
                  className="absolute inset-0 pointer-events-none"
                  style={{
                    background: `linear-gradient(160deg, ${meta.color}14 0%, rgba(255,255,255,0) 55%)`,
                  }}
                />
              )}
              <div className="relative z-10 flex flex-col items-center justify-center">
                {meta.renderShape(46)}
                <span
                  className="text-[9px] font-black uppercase mt-2 tracking-[0.14em]"
                  style={{ color: meta.color }}
                >
                  {meta.label}
                </span>
              </div>
            </motion.button>
          );
        })}
      </div>
    </div>
  );
}
