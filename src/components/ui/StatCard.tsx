/**
 * @license
 * SPDX-License-Identifier: Apache-2.0
 */

import React from "react";

interface Props {
  label: string;
  value: React.ReactNode;
  icon?: React.ReactNode;
  valueClassName?: string;
}

/**
 * StatCard — compact stat tile for grids (2–3 columns on phones).
 */
export default function StatCard({ label, value, icon, valueClassName = "text-slate-900" }: Props) {
  return (
    <div className="bg-white/85 backdrop-blur-sm border border-slate-100/80 rounded-2xl p-3 text-center shadow-soft">
      <div className="mx-auto w-7 h-7 rounded-full bg-indigo-50 text-[#6D3DF5] flex items-center justify-center mb-1.5">
        {icon}
      </div>
      <span className="text-[9px] text-slate-400 font-bold uppercase tracking-wider block">
        {label}
      </span>
      <p className={`text-sm font-black font-mono mt-0.5 ${valueClassName}`}>{value}</p>
    </div>
  );
}
