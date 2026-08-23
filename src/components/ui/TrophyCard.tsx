/**
 * @license
 * SPDX-License-Identifier: Apache-2.0
 */

import React from "react";
import { Trophy } from "lucide-react";

interface Props {
  icon: string;
  title: string;
  description: string;
  unlocked: boolean;
}

/**
 * TrophyCard — achievement row. Locked state is dimmed.
 */
const TrophyCard: React.FC<Props> = ({ icon, title, description, unlocked }) => {
  return (
    <div
      className={`flex items-center gap-2.5 p-[var(--sp-sm)] rounded-2xl border transition-all ${
        unlocked
          ? "bg-white/90 border-purple-200 shadow-soft"
          : "bg-white/40 border-slate-100 opacity-60"
      }`}
    >
      <span className="text-2xl select-none">{icon}</span>
      <div className="flex-1 min-w-0">
        <h4 className="text-xs font-black text-slate-800 truncate">{title}</h4>
        <p className="text-[10px] text-slate-400 mt-0.5 leading-tight">{description}</p>
      </div>
      <div>
        {unlocked ? (
          <span className="text-[9px] bg-emerald-50 text-emerald-600 font-extrabold uppercase px-2 py-0.5 rounded-full border border-emerald-100">
            ✓ LIBERADO
          </span>
        ) : (
          <span className="text-[9px] bg-slate-50 text-slate-400 font-bold uppercase px-2 py-0.5 rounded-full border border-slate-100">
            BLOQUEADO
          </span>
        )}
      </div>
    </div>
  );
}

export function TrophyIcon() {
  return <Trophy className="w-5 h-5 text-amber-500" />;
}

export default TrophyCard;
