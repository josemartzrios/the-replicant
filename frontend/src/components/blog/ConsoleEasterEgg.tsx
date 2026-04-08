"use client";

import { useEffect } from "react";
import { useNarrative } from "@/components/providers/NarrativeProvider";

/**
 * ConsoleEasterEgg Component (Epic-005)
 * Injects Matrix/Hacker style messages into the Chrome DevTools
 * to reward curious users who inspect the site.
 */
export function ConsoleEasterEgg() {
    const { phase, hasSecret, unlockSecret } = useNarrative();

    useEffect(() => {
        // Run once on mount to detect developers
        if (!hasSecret("dev_tools_inspector")) {
            unlockSecret("dev_tools_inspector");

            const asciiArt = `
             ___
            /   \\
           |     |
            \\___/
            (O.O)
            ( > )
            `;

            console.log(
                `%c${asciiArt}
You're looking under the hood. Good.
The truth is hidden in the source.
`,
                "color: #10b981; font-family: monospace;"
            );
        }

        // Print phase specific hints if they are advanced
        if (phase >= 1) {
            console.log("%c[HINT]: Check the /404 sector for anomalies.", "color: #8b5cf6; font-family: monospace; font-size: 10px;");
        }

    }, [hasSecret, unlockSecret, phase]);

    return null; // Silent component
}
