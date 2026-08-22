/**
 * @license
 * SPDX-License-Identifier: Apache-2.0
 */

import React from "react";

interface Props {
  children: React.ReactNode;
  className?: string;
}

/**
 * FloatingCard — glassmorphism base surface.
 * 24dp corners, soft shadow, translucent blur fill.
 */
export default function FloatingCard({ children, className = "" }: Props) {
  return (
    <div
      className={`bg-white/85 backdrop-blur-md border border-white/70 rounded-3xl shadow-premium relative overflow-hidden ${className}`}
    >
      {children}
    </div>
  );
}
