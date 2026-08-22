/**
 * @license
 * SPDX-License-Identifier: Apache-2.0
 */

import React from "react";

export interface TabItem {
  id: string;
  label: string;
}

interface Props {
  tabs: TabItem[];
  activeTab: string;
  onChange: (id: string) => void;
}

/**
 * BottomNavigation — floating docked tab bar (Android pattern).
 *
 *  - Floats 12–20dp above the system navigation bar
 *    (env(safe-area-inset-bottom) => adapts to gesture / 3-button nav)
 *  - 28dp corners, glassmorphism surface, soft premium shadow
 *  - Minimum bar height 64dp, minimum touch target 48dp per button
 */
export default function BottomNavigation({ tabs, activeTab, onChange }: Props) {
  return (
    <div
      className="shrink-0 w-full px-4"
      style={{
        paddingBottom: "calc(env(safe-area-inset-bottom, 0px) + clamp(12px, 1.8dvh, 20px))",
      }}
    >
      <nav
        className="min-h-[64px] flex items-stretch gap-1 p-1.5 bg-white/85 backdrop-blur-md border border-white/70 rounded-[28px] shadow-premium"
        aria-label="Navegação principal"
      >
        {tabs.map((tab) => (
          <button
            key={tab.id}
            onClick={() => onChange(tab.id)}
            aria-current={activeTab === tab.id ? "page" : undefined}
            className={`flex-1 min-h-[48px] px-2 my-auto rounded-[22px] text-[11px] font-black uppercase tracking-wider transition-all duration-150 cursor-pointer flex items-center justify-center active:scale-[0.97] ${
              activeTab === tab.id
                ? "bg-gradient-to-r from-[#6D3DF5] to-[#5124D6] text-white shadow-btn"
                : "bg-transparent text-slate-400 hover:text-slate-600"
            }`}
          >
            {tab.label}
          </button>
        ))}
      </nav>
    </div>
  );
}
