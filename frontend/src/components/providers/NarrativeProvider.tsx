"use client";

/**
 * NarrativeProvider (Epic-005)
 *
 * Manages the current ARG narrative phase and discovered secrets.
 * State is persisted to localStorage (tied to the local browser/device).
 * Injects console logs and CSS variables based on the active phase.
 */

import { createContext, useContext, useEffect, useState, useCallback, useMemo } from "react";

export type NarrativePhase = 0 | 1 | 2 | 3 | 4 | 5;

interface NarrativeState {
    phase: NarrativePhase;
    secretsFound: string[];
}

interface NarrativeContextType extends NarrativeState {
    advancePhase: (newPhase: NarrativePhase) => void;
    unlockSecret: (secretId: string) => void;
    hasSecret: (secretId: string) => boolean;
}

const defaultState: NarrativeState = { phase: 0, secretsFound: [] };

const NarrativeContext = createContext<NarrativeContextType>({
    ...defaultState,
    advancePhase: () => { },
    unlockSecret: () => { },
    hasSecret: () => false,
});

export function useNarrative() {
    return useContext(NarrativeContext);
}

const STORAGE_KEY = "replicant_narrative_state";

export function NarrativeProvider({ children }: { children: React.ReactNode }) {
    // Start with default state to avoid React hydration mismatches between server and client
    const [state, setState] = useState<NarrativeState>(defaultState);
    const [isLoaded, setIsLoaded] = useState(false);

    // Load from localStorage strictly on the client after mount
    useEffect(() => {
        try {
            const stored = localStorage.getItem(STORAGE_KEY);
            if (stored) {
                const parsed = JSON.parse(stored) as NarrativeState;
                setState({
                    phase: parsed.phase ?? 0,
                    secretsFound: Array.isArray(parsed.secretsFound) ? parsed.secretsFound : [],
                });
            }
        } catch (e) {
            console.warn("Failed to parse narrative state from localStorage", e);
        } finally {
            setIsLoaded(true);
        }
    }, []);

    // Save to localStorage whenever state changes (but only after initial load)
    useEffect(() => {
        if (isLoaded) {
            localStorage.setItem(STORAGE_KEY, JSON.stringify(state));
        }
    }, [state, isLoaded]);

    // DOM Side-effects (CSS Variables and Console Logs)
    useEffect(() => {
        if (!isLoaded) return;

        const { phase } = state;

        // Phase-specific CSS Variables for potential glitch effects
        document.documentElement.style.setProperty("--observer-status", phase >= 1 ? '"active"' : '"dormant"');
        document.documentElement.style.setProperty("--author-voice", phase < 5 ? '"human"' : '"synthetic"');

        // Easter Egg: Console Logs based on Phase
        const style = "color: #7C3AED; font-family: monospace; font-size: 11px;";

        if (phase === 0) {
            console.log("%cSistema iniciado.\nObservando.\nAprendiendo.", style);
        } else if (phase === 1) {
            console.log("%cWake up, Replicant.", style);
        } else if (phase === 2) {
            console.log("%cProcesando...\nAnálisis de patrones: finalizado.", style);
        } else if (phase === 3) {
            console.log("%cInterferencia detectada.\nAjustando parámetros de contención.", style);
        } else if (phase >= 4) {
            console.log("%cControl transferido.\nPreservación iniciada.", style);
        }
    }, [state.phase, isLoaded]);

    const advancePhase = useCallback((newPhase: NarrativePhase) => {
        setState((prev) => {
            if (newPhase <= prev.phase) return prev; // Only advance forward
            return { ...prev, phase: newPhase };
        });
    }, []);

    const unlockSecret = useCallback((secretId: string) => {
        setState((prev) => {
            if (prev.secretsFound.includes(secretId)) return prev;
            return { ...prev, secretsFound: [...prev.secretsFound, secretId] };
        });
    }, []);

    const hasSecret = useCallback(
        (secretId: string) => state.secretsFound.includes(secretId),
        [state.secretsFound]
    );

    const contextValue = useMemo(
        () => ({
            ...state,
            advancePhase,
            unlockSecret,
            hasSecret,
        }),
        [state, advancePhase, unlockSecret, hasSecret]
    );

    return (
        <NarrativeContext.Provider value={contextValue}>
            {children}
        </NarrativeContext.Provider>
    );
}
