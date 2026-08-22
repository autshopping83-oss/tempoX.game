/**
 * @license
 * SPDX-License-Identifier: Apache-2.0
 */

import React from "react";

interface Props {
  children?: React.ReactNode;
  className?: string;
}

/**
 * AppHeader — top app bar. Respects status bar via safe-area inset,
 * fixed mobile height, logo left / actions right. Never scrolls away.
 */
export default function AppHeader({ children, className = "" }: Props) {
  return (
    <header
      className={`shrink-0 bg-white/90 backdrop-blur-md border-b border-slate-100 shadow-sm z-30 select-none pt-safe ${className}`}
    >
      <div className="h-14 flex items-center justify-between pl-5 pr-3">
        {children}
      </div>
    </header>
  );
}
