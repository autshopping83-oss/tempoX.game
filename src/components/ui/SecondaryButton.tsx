/**
 * @license
 * SPDX-License-Identifier: Apache-2.0
 */

import React from "react";
import { motion } from "motion/react";

interface Props {
  children: React.ReactNode;
  onClick?: () => void;
  className?: string;
}

/**
 * SecondaryButton — supporting action. Minimum touch target 56dp,
 * neutral filled surface, full width by default.
 */
export default function SecondaryButton({ children, onClick, className = "" }: Props) {
  return (
    <motion.button
      whileTap={{ scale: 0.96 }}
      onClick={onClick}
      className={`w-full min-h-[56px] px-5 bg-slate-100 hover:bg-slate-200 text-slate-700 rounded-2xl font-extrabold text-sm flex items-center justify-center gap-2 cursor-pointer border border-slate-200/80 transition-colors ${className}`}
    >
      {children}
    </motion.button>
  );
}
