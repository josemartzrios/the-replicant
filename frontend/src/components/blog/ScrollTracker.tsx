"use client";

/**
 * ScrollTracker (Epic-005: Lore Engagement Tracker)
 *
 * Shows scroll depth playfully as "XX% analyzed".
 * Adds a subtle surveillance aesthetic to the blog.
 */

import { useEffect, useState } from "react";
import { useNarrative } from "@/components/providers/NarrativeProvider";

export function ScrollTracker() {
    const { phase } = useNarrative();
    const [scrollPercentage, setScrollPercentage] = useState(0);

    useEffect(() => {
        function handleScroll() {
            const windowHeight = window.innerHeight;
            const fullHeight = document.documentElement.scrollHeight;
            const scrollTop = window.scrollY;

            // Prevent division by zero and calculate percentage
            const scrollableRange = fullHeight - windowHeight;
            let percentage = 0;

            if (scrollableRange > 0) {
                percentage = Math.round((scrollTop / scrollableRange) * 100);
            }

            setScrollPercentage(Math.min(100, Math.max(0, percentage)));
        }

        // Only add listener on mount
        window.addEventListener("scroll", handleScroll, { passive: true });

        // Initial calculation
        handleScroll();

        return () => window.removeEventListener("scroll", handleScroll);
    }, []);

    // In phase 1, we show a basic tracker. It gets weirder in later phases.
    let displayLabel = `${scrollPercentage}% analyzed`;

    if (phase >= 3) {
        displayLabel = scrollPercentage === 100 ? "Sync Complete." : `Processing: ${scrollPercentage}%`;
    }

    return (
        <div className="fixed bottom-4 right-4 z-40 hidden sm:flex items-center justify-center opacity-40 hover:opacity-100 transition-opacity">
            <div className="rounded-full bg-[var(--color-surface)] border border-[var(--color-border)] px-3 py-1.5 shadow-[0_0_15px_rgba(0,0,0,0.5)] flex items-center gap-2 backdrop-blur-md">
                {/* Progress Ring */}
                <div className="relative h-4 w-4">
                    <svg className="h-full w-full -rotate-90" viewBox="0 0 24 24">
                        {/* Background Track */}
                        <circle
                            cx="12"
                            cy="12"
                            r="10"
                            stroke="currentColor"
                            strokeWidth="3"
                            fill="transparent"
                            className="text-[var(--color-border)]"
                        />
                        {/* Progress */}
                        <circle
                            cx="12"
                            cy="12"
                            r="10"
                            stroke="currentColor"
                            strokeWidth="3"
                            fill="transparent"
                            strokeDasharray={2 * Math.PI * 10}
                            strokeDashoffset={
                                2 * Math.PI * 10 -
                                (scrollPercentage / 100) * (2 * Math.PI * 10)
                            }
                            className="text-[var(--color-accent)] transition-all duration-150"
                        />
                    </svg>
                </div>

                {/* Value Text */}
                <span className="font-mono text-[10px] uppercase tracking-widest text-[var(--color-text-muted)] w-24">
                    {displayLabel}
                </span>

                {/* Glitch Overlay (Active only in Phase 3+) */}
                {phase >= 3 && (
                    <div
                        className="absolute inset-0 bg-red-500 mix-blend-color-dodge opacity-0 hover:opacity-20 pointer-events-none transition-opacity"
                        style={{ animation: 'glitch 2s infinite' }}
                    />
                )}
            </div>
        </div>
    );
}
