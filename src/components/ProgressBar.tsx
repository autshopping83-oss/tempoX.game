/**
 * @license
 * SPDX-License-Identifier: Apache-2.0
 */

import React from "react";

interface Props {
  pct: number;
  className?: string;
}

/**
 * Thick arcade progress bar: lilac track + purple->pink gradient fill
 * with a soft glow. Fixed 10-12dp height, highly visible.
 */
export default function ProgressBar({ pct, className = "" }: Props) {
  const clamped = Math.max(0, Math.min(100, pct));

  return (
    <div
      data-progress-track
      className={`w-full rounded-full bg-[#EDE9FE]/90 border border-white/70 overflow-hidden ${className}`}
      style={{ height: "clamp(10px, 1.6vh, 12px)" }}
    >
      <div
        data-progress-fill
        className="h-full rounded-full"
        style={{
          width: `${clamped}%`,
          background: "linear-gradient(90deg, #6D3DF5, #EC4899)",
          boxShadow: "0 0 10px rgba(236,72,153,0.45)",
          transition: "width 0.09s linear",
        }}
      />
    </div>
  );
}
