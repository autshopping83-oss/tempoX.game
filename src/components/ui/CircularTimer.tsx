/**
 * @license
 * SPDX-License-Identifier: Apache-2.0
 */

import React from "react";
import { motion } from "motion/react";
import { gameFeel } from "../../core/gameFeel";

interface Props {
  secondsLeft: number;
  total?: number;
}

const RADIUS = 36;
const CIRCUMFERENCE = 2 * Math.PI * RADIUS;

/**
 * CircularTimer — canonical circular countdown ring.
 *
 - Progress ring gradient: purple -> pink while calm,
   orange -> red under pressure.
 - Urgency tiers drive glow/pulse classes (calm/warn/urgent/critical/final).
 */
export default function CircularTimer({ secondsLeft, total = 60 }: Props) {
  const ratio = Math.max(0, Math.min(1, secondsLeft / total));
  const offset = CIRCUMFERENCE - ratio * CIRCUMFERENCE;

  const secs = Math.max(0, Math.ceil(secondsLeft));
  const tier = gameFeel.urgency(secs);

  const ringStroke =
    tier === "calm" || tier === "warn"
      ? "url(#timer-grad-cool)"
      : tier === "urgent"
        ? "url(#timer-grad-hot)"
        : "url(#timer-grad-fire)";

  const numberColor =
    tier === "critical" || tier === "final"
      ? "#EF4444"
      : tier === "urgent"
        ? "#F59E0B"
        : "#0F172A";

  return (
    <div
      data-timer-root
      data-timer-tier={tier}
      className={`relative w-20 h-20 rounded-full bg-white/90 border border-white flex items-center justify-center select-none timer-${tier}`}
      style={{ boxShadow: "0 12px 28px -10px rgba(109,61,245,0.35), 0 4px 12px -4px rgba(15,23,42,0.08)" }}
    >
      <svg className="absolute inset-0 w-full h-full -rotate-90" viewBox="0 0 80 80">
        <defs>
          <linearGradient id="timer-grad-cool" x1="0%" y1="0%" x2="100%" y2="100%">
            <stop offset="0%" stopColor="#6D3DF5" />
            <stop offset="100%" stopColor="#EC4899" />
          </linearGradient>
          <linearGradient id="timer-grad-hot" x1="0%" y1="0%" x2="100%" y2="100%">
            <stop offset="0%" stopColor="#F59E0B" />
            <stop offset="100%" stopColor="#EF4444" />
          </linearGradient>
          <linearGradient id="timer-grad-fire" x1="0%" y1="0%" x2="100%" y2="100%">
            <stop offset="0%" stopColor="#EF4444" />
            <stop offset="100%" stopColor="#DC2626" />
          </linearGradient>
        </defs>

        <circle cx="40" cy="40" r={RADIUS} stroke="#EEF2F7" strokeWidth="5" fill="none" />

        <motion.circle
          cx="40"
          cy="40"
          r={RADIUS}
          stroke={ringStroke}
          strokeWidth="5"
          fill="none"
          strokeLinecap="round"
          strokeDasharray={CIRCUMFERENCE}
          initial={{ strokeDashoffset: CIRCUMFERENCE - ratio * CIRCUMFERENCE }}
          animate={{ strokeDashoffset: offset }}
          transition={{ duration: 0.15, ease: "linear" }}
        />
      </svg>

      <div className="relative z-10 flex flex-col items-center justify-center">
        <span
          className={`font-mono font-black leading-none tracking-tight ${secs <= 5 ? "animate-soft-pulse" : ""}`}
          style={{ fontSize: "26px", color: numberColor }}
        >
          {secs}
        </span>
        <span className="text-[7px] text-slate-400 font-extrabold uppercase tracking-[0.22em] mt-0.5">
          SEGUNDOS
        </span>
      </div>
    </div>
  );
}
