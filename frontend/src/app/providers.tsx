"use client";

/**
 * Client-side providers wrapper.
 * Wraps the app with AuthProvider for global auth state.
 */

import { AuthProvider } from "@/lib/auth";
import { NarrativeProvider } from "@/components/providers/NarrativeProvider";
import type { ReactNode } from "react";

export function Providers({ children }: { children: ReactNode }) {
  return (
    <NarrativeProvider>
      <AuthProvider>{children}</AuthProvider>
    </NarrativeProvider>
  );
}
