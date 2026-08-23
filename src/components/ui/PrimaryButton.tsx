/**
 * @license
 * SPDX-License-Identifier: Apache-2.0
 */

import React from "react";
import { motion } from "motion/react";
import { Play } from "lucide-react";
import { sound } from "../../core/sound";

interface Props {
  children: React.ReactNode;
  onClick?: () => void;
  icon?: React.ReactNode;
  className?: string;
}

/**
 * PrimaryButton — main CTA. Minimum touch target 56dp, brand gradient,
 * full width by default.
 */
export default function PrimaryButton({ children, onClick, icon, className = "" }: Props) {
  return (
    <motion.button
      whileTap={{ scale: 0.96 }}
      onClick={() => { sound.playClick(); onClick?.(); }}
      className={`w-full min-h-[56px] px-6 bg-gradient-to-r from-[#6D3DF5] to-[#5124D6] text-white rounded-2xl font-black text-base flex items-center justify-center gap-2.5 shadow-btn cursor-pointer tracking-wide transition-all duration-150 active:brightness-110 ${className}`}
    >
      {icon ?? <Play className="fill-white stroke-none w-5 h-5" />}
      {children}
    </motion.button>
  );
}
