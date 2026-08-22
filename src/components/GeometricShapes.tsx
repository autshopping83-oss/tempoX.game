/**
 * @license
 * SPDX-License-Identifier: Apache-2.0
 */

import React from "react";
import { motion } from "motion/react";

interface ShapeProps {
  size?: number;
  color?: string; // Hex color or tailwind class name
  rotation?: number;
  scale?: number;
  onClick?: () => void;
  className?: string;
  animateIn?: boolean;
}

// Convert common color names to nice gradient / solid colors
function getShapeColor(color?: string): { fill: string; stroke: string; glow: string } {
  const norm = (color || "").toUpperCase();
  if (norm.includes("PURPLE") || norm.includes("6D3DF5")) {
    return { fill: "url(#grad-sh-purple)", stroke: "#6D3DF5", glow: "rgba(109, 61, 245, 0.2)" };
  }
  if (norm.includes("BLUE") || norm.includes("3B82F6")) {
    return { fill: "url(#grad-sh-blue)", stroke: "#3B82F6", glow: "rgba(59, 130, 246, 0.2)" };
  }
  if (norm.includes("GREEN") || norm.includes("22C55E")) {
    return { fill: "url(#grad-sh-green)", stroke: "#22C55E", glow: "rgba(34, 197, 94, 0.2)" };
  }
  if (norm.includes("YELLOW") || norm.includes("FACC15")) {
    return { fill: "url(#grad-sh-yellow)", stroke: "#FACC15", glow: "rgba(250, 204, 21, 0.2)" };
  }
  if (norm.includes("RED") || norm.includes("EF4444")) {
    return { fill: "url(#grad-sh-red)", stroke: "#EF4444", glow: "rgba(239, 68, 68, 0.2)" };
  }
  if (norm.includes("PINK") || norm.includes("EC4899")) {
    return { fill: "url(#grad-sh-pink)", stroke: "#EC4899", glow: "rgba(236, 72, 153, 0.2)" };
  }
  // Fallbacks
  return { fill: "url(#grad-sh-purple)", stroke: "#6D3DF5", glow: "rgba(109, 61, 245, 0.15)" };
}

// Global SVG gradients for our shapes to use
export function ShapeGradients() {
  return (
    <svg width="0" height="0" className="absolute">
      <defs>
        <linearGradient id="grad-sh-purple" x1="0%" y1="0%" x2="100%" y2="100%">
          <stop offset="0%" stopColor="#8B5CF6" />
          <stop offset="100%" stopColor="#6D3DF5" />
        </linearGradient>
        <linearGradient id="grad-sh-blue" x1="0%" y1="0%" x2="100%" y2="100%">
          <stop offset="0%" stopColor="#60A5FA" />
          <stop offset="100%" stopColor="#3B82F6" />
        </linearGradient>
        <linearGradient id="grad-sh-green" x1="0%" y1="0%" x2="100%" y2="100%">
          <stop offset="0%" stopColor="#4ADE80" />
          <stop offset="100%" stopColor="#22C55E" />
        </linearGradient>
        <linearGradient id="grad-sh-yellow" x1="0%" y1="0%" x2="100%" y2="100%">
          <stop offset="0%" stopColor="#FDE047" />
          <stop offset="100%" stopColor="#FACC15" />
        </linearGradient>
        <linearGradient id="grad-sh-red" x1="0%" y1="0%" x2="100%" y2="100%">
          <stop offset="0%" stopColor="#FCA5A5" />
          <stop offset="100%" stopColor="#EF4444" />
        </linearGradient>
        <linearGradient id="grad-sh-pink" x1="0%" y1="0%" x2="100%" y2="100%">
          <stop offset="0%" stopColor="#F472B6" />
          <stop offset="100%" stopColor="#EC4899" />
        </linearGradient>
      </defs>
    </svg>
  );
}

export function GameTriangle({
  size = 64,
  color,
  rotation = 0,
  scale = 1,
  onClick,
  className = "",
  animateIn = true,
}: ShapeProps) {
  const colors = getShapeColor(color);
  return (
    <motion.div
      initial={animateIn ? { scale: 0, opacity: 0 } : undefined}
      animate={{ scale, rotate: rotation, opacity: 1 }}
      whileTap={{ scale: 0.9 }}
      onClick={onClick}
      style={{ width: size, height: size }}
      className={`relative cursor-pointer select-none ${className}`}
    >
      <svg
        viewBox="0 0 100 100"
        fill="none"
        xmlns="http://www.w3.org/2000/svg"
        className="w-full h-full drop-shadow-[0_8px_16px_rgba(0,0,0,0.06)] filter"
      >
        <path
          d="M50 12 L92 82 C94 85, 92 88, 88 88 L12 88 C8 88, 6 85, 8 82 Z"
          fill={colors.fill}
        />
        <path
          d="M50 12 L50 88 L88 88 Z"
          fill="rgba(255,255,255,0.08)"
        />
        <path
          d="M50 12 L92 82 C94 85, 92 88, 88 88 L12 88 C8 88, 6 85, 8 82 Z"
          stroke="rgba(255,255,255,0.2)"
          strokeWidth="2"
        />
      </svg>
    </motion.div>
  );
}

export function GameCircle({
  size = 64,
  color,
  rotation = 0,
  scale = 1,
  onClick,
  className = "",
  animateIn = true,
}: ShapeProps) {
  const colors = getShapeColor(color);
  return (
    <motion.div
      initial={animateIn ? { scale: 0, opacity: 0 } : undefined}
      animate={{ scale, rotate: rotation, opacity: 1 }}
      whileTap={{ scale: 0.9 }}
      onClick={onClick}
      style={{ width: size, height: size }}
      className={`relative cursor-pointer select-none ${className}`}
    >
      <svg
        viewBox="0 0 100 100"
        fill="none"
        xmlns="http://www.w3.org/2000/svg"
        className="w-full h-full drop-shadow-[0_8px_16px_rgba(0,0,0,0.06)] filter"
      >
        <circle cx="50" cy="50" r="42" fill={colors.fill} />
        <circle cx="50" cy="50" r="30" stroke="rgba(255,255,255,0.2)" strokeWidth="3" />
        <path d="M50 8 A42 42 0 0 1 92 50" stroke="rgba(255,255,255,0.4)" strokeWidth="3" strokeLinecap="round" />
      </svg>
    </motion.div>
  );
}

export function GameSquare({
  size = 64,
  color,
  rotation = 0,
  scale = 1,
  onClick,
  className = "",
  animateIn = true,
}: ShapeProps) {
  const colors = getShapeColor(color);
  return (
    <motion.div
      initial={animateIn ? { scale: 0, opacity: 0 } : undefined}
      animate={{ scale, rotate: rotation, opacity: 1 }}
      whileTap={{ scale: 0.9 }}
      onClick={onClick}
      style={{ width: size, height: size }}
      className={`relative cursor-pointer select-none ${className}`}
    >
      <svg
        viewBox="0 0 100 100"
        fill="none"
        xmlns="http://www.w3.org/2000/svg"
        className="w-full h-full drop-shadow-[0_8px_16px_rgba(0,0,0,0.06)] filter"
      >
        <rect x="10" y="10" width="80" height="80" rx="18" fill={colors.fill} />
        <rect x="22" y="22" width="56" height="56" rx="10" stroke="rgba(255,255,255,0.15)" strokeWidth="3" fill="none" />
        <path d="M10 28 L90 28" stroke="rgba(255,255,255,0.06)" strokeWidth="3" />
      </svg>
    </motion.div>
  );
}

export function GameDiamond({
  size = 64,
  color,
  rotation = 0,
  scale = 1,
  onClick,
  className = "",
  animateIn = true,
}: ShapeProps) {
  const colors = getShapeColor(color);
  return (
    <motion.div
      initial={animateIn ? { scale: 0, opacity: 0 } : undefined}
      animate={{ scale, rotate: rotation, opacity: 1 }}
      whileTap={{ scale: 0.9 }}
      onClick={onClick}
      style={{ width: size, height: size }}
      className={`relative cursor-pointer select-none ${className}`}
    >
      <svg
        viewBox="0 0 100 100"
        fill="none"
        xmlns="http://www.w3.org/2000/svg"
        className="w-full h-full drop-shadow-[0_8px_16px_rgba(0,0,0,0.06)] filter"
      >
        <path d="M50 8 L92 50 L50 92 L8 50 Z" fill={colors.fill} />
        <path d="M50 8 L50 92 L92 50 Z" fill="rgba(255,255,255,0.06)" />
        <path d="M50 22 L78 50 L50 78 L22 50 Z" stroke="rgba(255,255,255,0.2)" strokeWidth="2.5" fill="none" />
      </svg>
    </motion.div>
  );
}

export function GameTarget({
  size = 120,
  color = "YELLOW",
  scale = 1,
  onClick,
  className = "",
  animateIn = true,
}: ShapeProps) {
  const colors = getShapeColor(color);
  return (
    <motion.div
      initial={animateIn ? { scale: 0.7, opacity: 0 } : undefined}
      animate={{ scale, opacity: 1 }}
      whileTap={{ scale: 1.1 }}
      onClick={onClick}
      style={{ width: size, height: size }}
      className={`relative cursor-pointer select-none flex items-center justify-center ${className}`}
    >
      {/* Ripple background ring */}
      <div className="absolute inset-0 rounded-full bg-yellow-400/20 animate-ping opacity-30" />
      
      <svg
        viewBox="0 0 120 120"
        fill="none"
        xmlns="http://www.w3.org/2000/svg"
        className="w-full h-full drop-shadow-[0_12px_24px_rgba(250,204,21,0.25)] filter"
      >
        {/* Outer Ring */}
        <circle cx="60" cy="60" r="54" fill="#FFFFFF" stroke="#E2E8F0" strokeWidth="4" />
        
        {/* Middle Ring */}
        <circle cx="60" cy="60" r="38" fill="rgba(250, 204, 21, 0.08)" stroke="#FACC15" strokeWidth="2" strokeDasharray="6 4" />
        
        {/* Inner Solid Target Circle */}
        <circle cx="60" cy="60" r="26" fill={colors.fill} />
        <circle cx="60" cy="60" r="16" fill="rgba(255, 255, 255, 0.25)" />
      </svg>
    </motion.div>
  );
}
