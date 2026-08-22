/**
 * @license
 * SPDX-License-Identifier: Apache-2.0
 */

/**
 * GameTheme
 * 
 * Centralized configuration object for colors, typography, spacing, and shadows
 * to ensure absolute visual consistency across the "TEMPOX" arcade experience.
 * 
 * This complies with Section 4 (Design System) of the redesign brief.
 */

export const GameColors = {
  // Brand Palette
  background: "#F8FAFC",
  surface: "#FFFFFF",
  primary: "#6D3DF5",
  primaryDark: "#5124D6",
  
  // Game Mechanic Colors
  blue: "#3B82F6",    // Math Challenge
  green: "#22C55E",   // Attention / Success Challenge
  yellow: "#FACC15",  // Reflex Challenge / Active Accent
  orange: "#F59E0B",  // Secondary Accent
  red: "#EF4444",     // Failure / Penalty / Decoy
  pink: "#EC4899",    // Memory Challenge

  // Neutral Typography & Borders
  textPrimary: "#111827",
  textSecondary: "#64748B",
  border: "#E2E8F0",
};

export const GameTheme = {
  // Centralized Tailwind utility maps to avoid hardcoded values
  colors: {
    background: "bg-[#F8FAFC]",
    surface: "bg-[#FFFFFF]",
    
    // Core Button & Accent Colors
    primary: {
      text: "text-[#6D3DF5]",
      bg: "bg-[#6D3DF5]",
      bgGradient: "bg-gradient-to-r from-[#6D3DF5] to-[#5124D6]",
      hoverBg: "hover:bg-[#5124D6]",
      lightBg: "bg-[#6D3DF5]/10",
      border: "border-[#6D3DF5]",
      borderLight: "border-[#6D3DF5]/10",
    },
    
    // Game/State Alerts
    success: {
      text: "text-[#22C55E]",
      bg: "bg-[#22C55E]",
      bgGradient: "bg-gradient-to-r from-[#22C55E] to-[#16A34A]",
      lightBg: "bg-[#22C55E]/10",
      border: "border-[#22C55E]/20",
    },
    danger: {
      text: "text-[#EF4444]",
      bg: "bg-[#EF4444]",
      bgGradient: "bg-gradient-to-r from-[#EF4444] to-[#DC2626]",
      lightBg: "bg-[#EF4444]/10",
      border: "border-[#EF4444]/15",
    },
    warning: {
      text: "text-[#FACC15]",
      bg: "bg-[#FACC15]",
      lightBg: "bg-[#FACC15]/10",
      border: "border-[#FACC15]/20",
    },
    info: {
      text: "text-[#3B82F6]",
      bg: "bg-[#3B82F6]",
      lightBg: "bg-[#3B82F6]/10",
      border: "border-[#3B82F6]/10",
    },
    pink: {
      text: "text-[#EC4899]",
      bg: "bg-[#EC4899]",
      lightBg: "bg-[#EC4899]/10",
      border: "border-[#EC4899]/10",
    },

    // Neutrals
    text: {
      primary: "text-slate-900",      // #111827
      secondary: "text-slate-500",    // #64748B
      muted: "text-slate-400",
    },
    borders: {
      light: "border-slate-100",
      medium: "border-slate-200/80",
    }
  },

  // Consistent game typography styles
  typography: {
    logoLarge: "text-7xl font-extrabold tracking-tight text-slate-900 leading-none",
    logoSub: "text-2xl font-black tracking-[0.25em] text-[#6D3DF5] uppercase",
    
    sectionTitle: "text-base font-extrabold text-slate-900 tracking-tight uppercase",
    cardTitle: "text-xs font-black text-slate-800",
    body: "text-xs text-slate-600 leading-relaxed",
    
    // Numbers
    scoreMain: "text-7xl font-black font-mono tracking-tighter text-slate-900",
    scoreSmall: "font-mono text-2xl font-black text-slate-900 leading-none",
    timerText: "text-2xl font-black font-mono tracking-tight leading-none",
    
    // Badges/Metadata
    badgeText: "text-[10px] font-black uppercase tracking-[0.2em] px-3.5 py-1 rounded-full border",
    pillLabel: "text-[9px] text-slate-400 font-bold uppercase tracking-wider block",
  },

  // Outer and inner layout spacing mathematically sized
  // clamp() keeps the original metrics on normal/tall screens and
  // progressively compacts vertical rhythm on short mobile viewports.
  spacing: {
    outerPadding: "py-[clamp(1rem,3vh,1.5rem)] px-5",
    containerGap: "gap-[clamp(0.875rem,2.25vh,1.5rem)]",
    elementGap: "gap-3.5",
  },

  // Premium UI elevation shadow definitions
  shadows: {
    premium: "shadow-premium", // custom defined in index.css
    btnPrimary: "shadow-btn",
    btnSuccess: "shadow-btn-green",
    btnWarning: "shadow-btn-yellow",
    cardHover: "shadow-card-hover",
    soft: "shadow-soft",
  },

  // Shape and border radii rules
  shapes: {
    card: "rounded-3xl",
    button: "rounded-2xl",
    pill: "rounded-full",
  }
};
