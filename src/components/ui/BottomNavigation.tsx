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
 * BottomNavigation — segmented tab bar docked to the bottom edge.
 * Respects gesture navigation area via safe-area inset.
 */
export default function BottomNavigation({ tabs, activeTab, onChange }: Props) {
  return (
    <div className="shrink-0 pt-safe pb-safe bg-white/80 backdrop-blur-md border-t border-slate-100 px-4 pt-2 pb-2">
      <div className="flex bg-slate-100/80 p-1 rounded-2xl border border-slate-200/40">
        {tabs.map((tab) => (
          <button
            key={tab.id}
            onClick={() => onChange(tab.id)}
            className={`flex-grow py-2.5 text-[11px] font-black uppercase tracking-wider rounded-xl transition-all cursor-pointer ${
              activeTab === tab.id
                ? "bg-white text-[#6D3DF5] shadow-sm"
                : "text-slate-400 hover:text-slate-600"
            }`}
          >
            {tab.label}
          </button>
        ))}
      </div>
    </div>
  );
}
