"use client";

import { useEffect } from "react";
import { usePathname } from "next/navigation";
import { useNarrative } from "@/components/providers/NarrativeProvider";

/**
 * ScrollTracker Component (Epic-005)
 * Silently tracks how far the user scrolls down reading articles.
 * Reaching 90% scroll depth on any post unlocks the 'deep_reader' secret
 * and advances the ARG to Phase 1.
 */
export function ScrollTracker() {
    const pathname = usePathname();
    const { phase, advancePhase, unlockSecret, hasSecret } = useNarrative();

    useEffect(() => {
        // Only track scrolling on individual post pages for lore drops
        if (!pathname?.startsWith("/posts/")) return;

        // If they already found this tier of secrets, don't spam calculations
        if (hasSecret("deep_reader_1") && phase >= 1) return;

        let ticking = false;

        const handleScroll = () => {
            if (!ticking) {
                window.requestAnimationFrame(() => {
                    const scrollHeight = document.documentElement.scrollHeight;
                    const scrollTop = document.documentElement.scrollTop;
                    const clientHeight = document.documentElement.clientHeight;

                    // Prevent division by zero on empty pages
                    if (scrollHeight <= clientHeight) return;

                    const scrollPercentage = (scrollTop / (scrollHeight - clientHeight)) * 100;

                    // Lore trigger threshold: user read almost the entire article
                    if (scrollPercentage > 90) {
                        if (!hasSecret("deep_reader_1")) {
                            unlockSecret("deep_reader_1");
                            setTimeout(() => {
                                console.log("%c[SYSTEM] Anomalous curiosity detected. Protocol Phase 1 initiated.", "color: #ef4444; font-weight: bold; font-family: monospace;");
                            }, 1000);
                        }

                        if (phase < 1) {
                            advancePhase(1);
                        }
                    }

                    ticking = false;
                });
                ticking = true;
            }
        };

        window.addEventListener("scroll", handleScroll, { passive: true });

        return () => {
            window.removeEventListener("scroll", handleScroll);
        };
    }, [pathname, phase, advancePhase, unlockSecret, hasSecret]);

    return null; // This is a silent observer
}
