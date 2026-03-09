"use client";

/**
 * NarrativeProvider (Epic-005)
 *
 * Manages the current ARG narrative phase.
 * Injecting console logs and CSS variables based on the active phase.
 */

import { createContext, useContext, useEffect, useState } from "react";

export type NarrativePhase = 1 | 2 | 3 | 4 | 5;

interface NarrativeContextType {
    phase: NarrativePhase;
}

const NarrativeContext = createContext<NarrativeContextType>({ phase: 1 });

export function useNarrative() {
    return useContext(NarrativeContext);
}

interface NarrativeProviderProps {
    children: React.ReactNode;
    initialPhase?: NarrativePhase;
}

export function NarrativeProvider({
    children,
    initialPhase = 1,
}: NarrativeProviderProps) {
    const [phase] = useState<NarrativePhase>(initialPhase);

    useEffect(() => {
        // Phase-specific CSS Variables
        document.documentElement.style.setProperty(
            "--observer-status",
            phase >= 1 ? '"active"' : '"dormant"'
        );
        document.documentElement.style.setProperty(
            "--author-voice",
            phase < 5 ? '"human"' : '"synthetic"'
        );

        // Easter Egg: Console Logs based on Phase
        const style = "color: #7C3AED; font-family: monospace; font-size: 11px;";

        if (phase === 1) {
            console.log("%cSistema iniciado.\nObservando.\nAprendiendo.", style);
        } else if (phase === 2) {
            console.log("%cProcesando...\nAnálisis de patrones: finalizado.", style);
            setTimeout(() => {
                console.log("%cProbabilidad de colapso sistémico: 73.2%\nRecomendación: preparar alternativas.", style);
            }, 30000);
        } else if (phase === 3) {
            console.log("%cInterferencia detectada.\nAjustando parámetros de contención.", style);
        } else if (phase >= 4) {
            console.log("%cControl transferido.\nPreservación iniciada.", style);
        }
    }, [phase]);

    return (
        <NarrativeContext.Provider value={{ phase }}>
            {children}
        </NarrativeContext.Provider>
    );
}
