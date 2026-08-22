/**
 * @license
 * SPDX-License-Identifier: Apache-2.0
 */

import React, { useId } from "react";

/**
 * Living light background: soft lilac depth washes + floating arcade
 * illustrations (triangles, circles, squares, diamonds, stars,
 * lightning bolts, timers/clocks, dots and thin geometric lines).
 *
 * Everything is decorative: pointer-events-none, low opacity (0.04-0.15),
 * slow transform-only animations with staggered negative delays so
 * nothing moves in sync.
 */
export default function FloatingBackgroundShapes() {
  const uid = useId().replace(/[^a-zA-Z0-9]/g, "");
  const gPurple = `fbg-purple-${uid}`;
  const gOrange = `fbg-orange-${uid}`;
  const gBlue = `fbg-blue-${uid}`;
  const gGreen = `fbg-green-${uid}`;
  const gPink = `fbg-pink-${uid}`;

  return (
    <div className="absolute inset-0 overflow-hidden pointer-events-none z-0" aria-hidden="true">
      {/* Soft grid pattern */}
      <div className="absolute inset-0 bg-grid-light opacity-60" />

      {/* Depth washes (lilac / pink / blue) */}
      <div className="absolute -top-[10%] -left-[15%] w-64 h-64 rounded-full bg-[#8B5CF6]/10 blur-3xl animate-float-5" />
      <div className="absolute top-[35%] -right-[20%] w-72 h-72 rounded-full bg-[#EC4899]/[0.07] blur-3xl animate-float-2" />
      <div className="absolute -bottom-[15%] left-[10%] w-80 h-80 rounded-full bg-[#6D3DF5]/[0.06] blur-3xl animate-float-1" />

      <svg width="0" height="0" className="absolute">
        <defs>
          <linearGradient id={gPurple} x1="0%" y1="0%" x2="100%" y2="100%">
            <stop offset="0%" stopColor="#8B5CF6" />
            <stop offset="100%" stopColor="#6D3DF5" />
          </linearGradient>
          <linearGradient id={gOrange} x1="0%" y1="0%" x2="100%" y2="100%">
            <stop offset="0%" stopColor="#FACC15" />
            <stop offset="100%" stopColor="#F59E0B" />
          </linearGradient>
          <linearGradient id={gBlue} x1="0%" y1="0%" x2="100%" y2="100%">
            <stop offset="0%" stopColor="#60A5FA" />
            <stop offset="100%" stopColor="#3B82F6" />
          </linearGradient>
          <linearGradient id={gGreen} x1="0%" y1="0%" x2="100%" y2="100%">
            <stop offset="0%" stopColor="#4ADE80" />
            <stop offset="100%" stopColor="#22C55E" />
          </linearGradient>
          <linearGradient id={gPink} x1="0%" y1="0%" x2="100%" y2="100%">
            <stop offset="0%" stopColor="#F472B6" />
            <stop offset="100%" stopColor="#EC4899" />
          </linearGradient>
        </defs>
      </svg>

      {/* Triangle — top left */}
      <div className="absolute left-[7%] top-[9%] w-12 h-12 opacity-[0.13] animate-float-1 deco-extra" style={{ animationDelay: "-2s", animationDuration: "9s" }}>
        <svg viewBox="0 0 100 100" className="w-full h-full drop-shadow-sm">
          <path d="M50 12 L88 84 L12 84 Z" fill={`url(#${gOrange})`} />
        </svg>
      </div>

      {/* Timer / clock — top center-right */}
      <div className="absolute right-[16%] top-[5%] w-14 h-14 opacity-[0.11] animate-float-4" style={{ animationDelay: "-5s" }}>
        <svg viewBox="0 0 100 100" className="w-full h-full" fill="none">
          <circle cx="50" cy="54" r="34" stroke="#6D3DF5" strokeWidth="6" />
          <path d="M50 54 L50 34 M50 54 L63 61" stroke="#6D3DF5" strokeWidth="6" strokeLinecap="round" />
          <path d="M38 14 L62 14" stroke="#6D3DF5" strokeWidth="7" strokeLinecap="round" />
          <path d="M78 30 L86 22" stroke="#6D3DF5" strokeWidth="5" strokeLinecap="round" opacity="0.7" />
        </svg>
      </div>

      {/* Diamond — left middle */}
      <div className="absolute left-[5%] top-[42%] w-10 h-10 opacity-[0.12] animate-float-6" style={{ animationDelay: "-3.5s" }}>
        <svg viewBox="0 0 100 100" className="w-full h-full">
          <path d="M50 6 L94 50 L50 94 L6 50 Z" fill={`url(#${gPurple})`} />
        </svg>
      </div>

      {/* Circle — right middle */}
      <div className="absolute right-[7%] top-[46%] w-9 h-9 opacity-[0.1] animate-float-3" style={{ animationDelay: "-1.5s", animationDuration: "8s" }}>
        <svg viewBox="0 0 100 100" className="w-full h-full">
          <circle cx="50" cy="50" r="42" fill={`url(#${gBlue})`} />
        </svg>
      </div>

      {/* Lightning bolt — bottom left */}
      <div className="absolute left-[12%] bottom-[16%] w-9 h-9 opacity-[0.13] animate-float-2" style={{ animationDelay: "-4s" }}>
        <svg viewBox="0 0 24 24" className="w-full h-full" fill="#F59E0B">
          <path d="M13 2 L4.5 13.5 H10.5 L9 22 L19.5 9.5 H12.8 Z" />
        </svg>
      </div>

      {/* Star — bottom right */}
      <div className="absolute right-[13%] bottom-[22%] w-10 h-10 opacity-[0.14] animate-float-5" style={{ animationDelay: "-6s", animationDuration: "10s" }}>
        <svg viewBox="0 0 100 100" className="w-full h-full fill-[#FACC15]">
          <path d="M50 4 L58 36 L92 37 L65 57 L74 90 L50 70 L26 90 L35 57 L8 37 L42 36 Z" />
        </svg>
      </div>

      {/* Square — bottom center-left */}
      <div className="absolute left-[32%] bottom-[7%] w-8 h-8 opacity-[0.09] animate-float-1" style={{ animationDelay: "-2.8s", animationDuration: "12s" }} >
        <svg viewBox="0 0 100 100" className="w-full h-full">
          <rect x="12" y="12" width="76" height="76" rx="18" fill={`url(#${gGreen})`} />
        </svg>
      </div>

      {/* Sparkles ✦ (four-point stars) */}
      <div className="absolute right-[30%] top-[18%] w-6 h-6 opacity-[0.15] animate-float-6" style={{ animationDelay: "-1s" }}>
        <svg viewBox="0 0 24 24" className="w-full h-full fill-[#EC4899]">
          <path d="M12 0 C13 7 17 11 24 12 C17 13 13 17 12 24 C11 17 7 13 0 12 C7 11 11 7 12 0 Z" />
        </svg>
      </div>
      <div className="deco-extra absolute left-[45%] top-[6%] w-4 h-4 opacity-[0.13] animate-float-3" style={{ animationDelay: "-3s" }}>
        <svg viewBox="0 0 24 24" className="w-full h-full fill-[#8B5CF6]">
          <path d="M12 0 C13 7 17 11 24 12 C17 13 13 17 12 24 C11 17 7 13 0 12 C7 11 11 7 12 0 Z" />
        </svg>
      </div>

      {/* Thin speed lines */}
      <div className="deco-extra absolute left-[4%] top-[28%] w-16 h-[3px] rounded-full bg-[#6D3DF5]/[0.08] animate-float-x-soft" style={{ animationDelay: "-2s", "--line-rot": "24deg" } as React.CSSProperties} />
      <div className="deco-extra absolute right-[5%] top-[68%] w-12 h-[3px] rounded-full bg-[#EC4899]/[0.08] animate-float-x-soft" style={{ animationDelay: "-4.5s", "--line-rot": "-18deg" } as React.CSSProperties} />

      {/* Small dots */}
      <div className="absolute left-[24%] top-[16%] w-2 h-2 rounded-full bg-[#3B82F6]/25 animate-float-2" style={{ animationDelay: "-1.2s" }} />
      <div className="absolute right-[26%] bottom-[10%] w-3 h-3 rounded-full bg-[#EC4899]/20 animate-float-3" style={{ animationDelay: "-2.4s" }} />
      <div className="deco-extra absolute left-[52%] bottom-[40%] w-1.5 h-1.5 rounded-full bg-[#FACC15]/40 animate-float-1" style={{ animationDelay: "-0.6s" }} />
      <div className="deco-extra absolute left-[16%] bottom-[42%] w-2 h-2 rounded-full bg-[#22C55E]/25 animate-float-5" style={{ animationDelay: "-5.2s" }} />
    </div>
  );
}
