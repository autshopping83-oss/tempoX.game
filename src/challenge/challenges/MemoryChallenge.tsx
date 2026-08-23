/**
 * @license
 * SPDX-License-Identifier: Apache-2.0
 */

import React, { useState, useEffect, useRef } from "react";
import { MemoryChallengeInstance, MEMORY_SHAPES, MEMORY_TINTS } from "../types";
import { motion } from "motion/react";
import { Check } from "lucide-react";
import { gameFeel } from "../../core/gameFeel";
import { useI18n } from "../../core/i18n";

interface Props {
  challenge: MemoryChallengeInstance;
  onSolve: (success: boolean, sequenceLength: number) => void;
  /** Freeze/unfreeze the per-challenge countdown while the player watches. */
  onWatchPhaseChange?: (paused: boolean) => void;
}

const INACTIVE_DOT = "#E9E4F8";
/** Flash cadence — identical to the native MemoryHost (700ms per shape). */
const FLASH_MS = 700;

export default function MemoryChallenge({ challenge, onSolve, onWatchPhaseChange }: Props) {
  const { sequence } = challenge;
  const { t } = useI18n();
  const [phase, setPhase] = useState<"WATCHING" | "PLAYING">("WATCHING");
  const [activeFlashIndex, setActiveFlashIndex] = useState<number>(-1);
  const [playerInput, setPlayerInput] = useState<number[]>([]);
  const [locked, setLocked] = useState(false);
  const watchCbRef = useRef(onWatchPhaseChange);
  watchCbRef.current = onWatchPhaseChange;

  useEffect(() => {
    setPhase("WATCHING");
    setPlayerInput([]);
    setActiveFlashIndex(-1);
    setLocked(false);

    // Countdown is frozen during the watch phase — native parity.
    watchCbRef.current?.(true);

    let idx = 0;
    const interval = setInterval(() => {
      if (idx < sequence.length) {
        setActiveFlashIndex(idx);
        setTimeout(() => {
          setActiveFlashIndex(-1);
        }, FLASH_MS * 0.7);
        idx++;
      } else {
        clearInterval(interval);
        setPhase("PLAYING");
        watchCbRef.current?.(false);
      }
    }, FLASH_MS);

    return () => {
      clearInterval(interval);
      // Never leak a frozen clock into the next challenge.
      watchCbRef.current?.(false);
    };
  }, [sequence]);

  const handleNodeClick = (shapeIdx: number, el: HTMLElement) => {
    if (phase !== "PLAYING" || locked) return;

    const nextInput = [...playerInput, shapeIdx];
    setPlayerInput(nextInput);

    const checkIndex = nextInput.length - 1;
    if (shapeIdx === sequence[checkIndex]) {
      // Hit: green flash + punch on the pressed card
      gameFeel.flash(el, "green");
      gameFeel.punch(el);

      if (nextInput.length === sequence.length) {
        // Lock input instantly until the parent advances to the next challenge.
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
        <span>{watching ? t("memory_watch_phase") : t("memory_input_phase")}</span>
      </div>

      {/* Progress dots — one per sequence step */}
      <div className="shrink-0 flex flex-wrap justify-center items-center gap-1.5 mt-2 mb-3 select-none max-w-full px-2">
        {sequence.map((_, idx) => {
          const lit = watching ? idx <= activeFlashIndex : idx < playerInput.length;
          const activeNow = watching && idx === activeFlashIndex;
          const c =
            watching && activeNow
              ? MEMORY_TINTS[sequence[idx]]
              : !watching && idx < playerInput.length
                ? MEMORY_TINTS[sequence[idx]]
                : INACTIVE_DOT;
          return (
            <div
              key={`dot-${idx}`}
              className="rounded-full transition-all duration-150"
              style={{
                width: activeNow ? 17 : 14,
                height: activeNow ? 17 : 14,
                backgroundColor: lit || activeNow ? c : INACTIVE_DOT,
                boxShadow: activeNow ? `0 0 10px ${MEMORY_TINTS[sequence[idx]]}88` : "none",
              }}
            />
          );
        })}
      </div>

      {/* Answer palette: all 6 shapes, 2 rows x 3 columns (native layout) */}
      <div className="grid grid-cols-3 gap-2.5 w-full shrink-0 max-w-[300px] mx-auto">
        {MEMORY_SHAPES.map((shape, idx) => {
          const tint = MEMORY_TINTS[idx];
          const wrongFlash = playerInput.length > 0 &&
            phase === "PLAYING" &&
            playerInput[playerInput.length - 1] === idx &&
            playerInput.length <= sequence.length &&
            sequence[playerInput.length - 1] !== idx;

          return (
            <motion.button
              key={shape}
              data-memory-option={idx}
              disabled={watching || locked}
              onClick={(e) => handleNodeClick(idx, e.currentTarget)}
              whileTap={watching || locked ? undefined : { scale: 0.94 }}
              initial={{ opacity: 0, y: 14 }}
              animate={{ opacity: 1, y: 0 }}
              transition={{
                delay: idx * 0.04,
                type: "spring",
                stiffness: 280,
                damping: 24,
              }}
              className={`relative rounded-2xl border-2 flex items-center justify-center select-none overflow-hidden ${
                wrongFlash ? "gf-shake" : ""
              }`}
              style={{
                minHeight: 76,
                background: wrongFlash ? `${tint}80` : `${tint}29`,
                borderColor: tint,
              }}
            >
              {/* filled checkmarks for completed steps sit above the grid */}
              <span style={{ fontSize: 34 }} className="relative z-10 leading-none pointer-events-none">
                {shape}
              </span>
            </motion.button>
          );
        })}
      </div>

      {/* Input progress: sequence steps already matched */}
      {!watching && (
        <div className="shrink-0 flex justify-center items-center gap-1 mt-3 select-none">
          {sequence.map((seqIdx, i) => (
            <motion.div
              key={`filled-${i}`}
              initial={{ scale: 0.6, opacity: 0 }}
              animate={{ scale: 1, opacity: 1 }}
              className="w-6 h-6 rounded-lg flex items-center justify-center"
              style={
                i < playerInput.length
                  ? { background: MEMORY_TINTS[seqIdx], color: "#FFFFFF" }
                  : { background: "#FFFFFF", border: "2px dashed #DDD6EE" }
              }
            >
              {i < playerInput.length && (
                <Check className="w-3.5 h-3.5" strokeWidth={3.5} />
              )}
            </motion.div>
          ))}
        </div>
      )}
    </div>
  );
}
