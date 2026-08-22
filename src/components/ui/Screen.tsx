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
 * Screen — base layout primitive for ALL game screens (current & future).
 *
 * Guarantees:
 *  - exactly one viewport tall (100% of parent, which is 100dvh)
 *  - NO vertical scroll: content must be sized with fluid tokens
 *  - no horizontal overflow
 *  - flex column so sections can shrink/grow proportionally
 *
 * Safe areas: compose with `AppHeader` (pt-safe), `BottomNavigation` /
 * footers (pb-safe) — or add pt-safe/pb-safe utilities directly.
 */
export default function Screen({ children, className = "" }: Props) {
  return (
    <div className={`relative flex flex-col w-full h-full min-h-0 overflow-hidden select-none ${className}`}>
      {children}
    </div>
  );
}
