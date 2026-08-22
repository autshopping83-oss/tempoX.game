/**
 * @license
 * SPDX-License-Identifier: Apache-2.0
 */

import React, { useEffect, useRef, useState } from "react";

interface Props {
  value: number;
  className?: string;
}

/**
 * Animates numeric changes with a short eased count-up and a small
 * scale punch. Display-only: the real score state is never modified.
 */
export default function AnimatedScore({ value, className = "" }: Props) {
  const [display, setDisplay] = useState(value);
  const [punch, setPunch] = useState(false);
  const displayRef = useRef(value);
  const rafRef = useRef<number | null>(null);

  useEffect(() => {
    const from = displayRef.current;
    if (from === value) return;

    setPunch(true);
    const duration = 450;
    const start = performance.now();

    const step = (t: number) => {
      const k = Math.min(1, (t - start) / duration);
      const eased = 1 - Math.pow(1 - k, 3);
      const next = Math.round(from + (value - from) * eased);
      displayRef.current = next;
      setDisplay(next);
      if (k < 1) {
        rafRef.current = requestAnimationFrame(step);
      } else {
        setPunch(false);
      }
    };

    rafRef.current = requestAnimationFrame(step);
    return () => {
      if (rafRef.current) cancelAnimationFrame(rafRef.current);
    };
  }, [value]);

  return (
    <span
      data-score-display={display}
      style={{ display: "inline-block" }}
      className={`${punch ? "gf-punch" : ""} ${className}`}
    >
      {display.toLocaleString("pt-BR")}
    </span>
  );
}
