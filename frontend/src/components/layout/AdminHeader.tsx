"use client";

/**
 * Admin Header Component (US-005)
 *
 * Top bar for authenticated admin pages:
 * - Shows "The Replicant" brand + "Admin" label
 * - User name badge
 * - Logout button → POST /auth/logout → clear tokens → redirect /login
 */

import { useRouter } from "next/navigation";
import { useAuth } from "@/lib/auth";
import { LogOut, Terminal } from "lucide-react";

export function AdminHeader() {
    const router = useRouter();
    const { user, logout } = useAuth();

    async function handleLogout() {
        await logout();
        router.push("/login");
    }

    return (
        <header className="sticky top-0 z-50 border-b border-[var(--color-border)] bg-[var(--color-surface)]/80 backdrop-blur-xl">
            <div className="flex h-14 items-center justify-between px-4 sm:px-6">
                {/* Brand */}
                <div className="flex items-center gap-3">
                    <Terminal className="h-5 w-5 text-[var(--color-accent)]" />
                    <span className="font-bold text-sm tracking-tight">
                        The Replicant
                    </span>
                    <span className="font-mono text-xs text-[var(--color-text-faint)] hidden sm:inline">
                        / admin
                    </span>
                </div>

                {/* User + Logout */}
                <div className="flex items-center gap-4">
                    {user && (
                        <span className="font-mono text-xs text-[var(--color-text-muted)]">
                            {user.name}
                            <span className="ml-2 rounded bg-[var(--color-accent-muted)] px-1.5 py-0.5 text-[var(--color-accent)] text-[10px] uppercase">
                                {user.role}
                            </span>
                        </span>
                    )}
                    <button
                        onClick={handleLogout}
                        className="flex items-center gap-2 rounded-lg px-3 py-2 text-sm text-[var(--color-text-muted)] hover:text-[var(--color-text)] hover:bg-[var(--color-surface-hover)] transition-colors"
                        aria-label="Logout"
                    >
                        <LogOut className="h-4 w-4" />
                        <span className="font-mono text-xs hidden sm:inline">LOGOUT</span>
                    </button>
                </div>
            </div>
        </header>
    );
}
