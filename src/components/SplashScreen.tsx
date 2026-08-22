/**
 * @license
 * SPDX-License-Identifier: Apache-2.0
 */

import React from "react";
import { motion } from "motion/react";

export default function SplashScreen() {
  return (
    <motion.div
      className="fixed inset-0 z-[100] flex flex-col items-center justify-center overflow-hidden select-none"
      style={{
        background:
          "radial-gradient(circle at 50% 38%, #1e1b4b 0%, #0b1026 48%, #05070a 100%)",
      }}
      initial={{ opacity: 0 }}
      animate={{ opacity: 1 }}
      exit={{ opacity: 0, scale: 1.04 }}
      transition={{ duration: 0.45, ease: "easeInOut" }}
    >
      {/* Soft ambient glow behind the logo */}
      <div className="absolute w-56 h-56 rounded-full bg-[#6D3DF5]/25 blur-[90px]" />
      <div className="absolute w-40 h-40 rounded-full bg-[#EC4899]/15 blur-[80px] translate-y-16" />

      {/* Floating geometric accents */}
      <span className="absolute top-[22%] left-[18%] text-lg text-[#FACC15]/70 animate-float-1">▲</span>
      <span className="absolute bottom-[26%] right-[16%] text-base text-[#3B82F6]/70 animate-float-2">⬦</span>
      <span className="absolute top-[30%] right-[24%] text-xs text-[#EC4899]/70 animate-float-3">●</span>

      {/* Official TEMPOX logo — opacity 0 → 1, scale 0.90 → 1 */}
      <motion.img
        src="/logo.png"
        alt="TEMPOX"
        draggable={false}
        initial={{ opacity: 0, scale: 0.9 }}
        animate={{ opacity: 1, scale: 1 }}
        transition={{ duration: 0.7, ease: [0.22, 1, 0.36, 1] }}
        className="relative z-10 w-44 sm:w-52 h-auto object-contain drop-shadow-[0_0_35px_rgba(109,61,245,0.55)]"
      />

      <motion.p
        initial={{ opacity: 0, y: 10 }}
        animate={{ opacity: 1, y: 0 }}
        transition={{ delay: 0.4, duration: 0.5, ease: "easeOut" }}
        className="relative z-10 mt-6 text-[11px] font-black uppercase tracking-[0.35em] text-slate-300"
      >
        THINK FAST. REACT FASTER.
      </motion.p>

      <motion.div
        initial={{ opacity: 0 }}
        animate={{ opacity: 1 }}
        transition={{ delay: 0.7 }}
        className="absolute bottom-12 flex gap-1.5 z-10"
      >
        <span className="w-1.5 h-1.5 rounded-full bg-[#6D3DF5] animate-soft-pulse" />
        <span className="w-1.5 h-1.5 rounded-full bg-[#EC4899] animate-soft-pulse [animation-delay:0.2s]" />
        <span className="w-1.5 h-1.5 rounded-full bg-[#FACC15] animate-soft-pulse [animation-delay:0.4s]" />
      </motion.div>
    </motion.div>
  );
}
