"use client";

/**
 * Login Page (US-001)
 *
 * - Checks /auth/status first → if setupRequired, redirects to /setup
 * - Email + Password form with validation
 * - On success: stores tokens → redirects to /admin
 * - OWASP: generic error message on failure (no user enumeration)
 */

import { useState, useEffect, type FormEvent } from "react";
import { useRouter } from "next/navigation";
import { useAuth } from "@/lib/auth";
import { api } from "@/lib/api";
import type { ApiError } from "@/lib/types";
import { Eye, EyeOff, Loader2, AlertCircle } from "lucide-react";
import { Skeleton } from "@/components/ui/Skeleton";

export default function LoginPage() {
    const router = useRouter();
    const { login, isAuthenticated, isLoading: authLoading } = useAuth();

    const [email, setEmail] = useState("");
    const [password, setPassword] = useState("");
    const [showPassword, setShowPassword] = useState(false);
    const [error, setError] = useState("");
    const [isSubmitting, setIsSubmitting] = useState(false);
    const [isCheckingStatus, setIsCheckingStatus] = useState(true);

    // Check auth status on mount
    useEffect(() => {
        async function checkStatus() {
            try {
                const status = await api.getAuthStatus();
                if (status.setupRequired) {
                    router.replace("/setup");
                    return;
                }
            } catch {
                // If status check fails, show login anyway
            }
            setIsCheckingStatus(false);
        }

        if (isAuthenticated) {
            router.replace("/admin");
        } else if (!authLoading) {
            checkStatus();
        }
    }, [isAuthenticated, authLoading, router]);

    async function handleSubmit(e: FormEvent) {
        e.preventDefault();
        setError("");
        setIsSubmitting(true);

        try {
            await login(email, password);
            router.push("/admin");
        } catch (err: unknown) {
            const apiError = err as ApiError;
            // OWASP: Generic message to prevent user enumeration
            setError(
                apiError?.status === 401
                    ? "Invalid credentials. Please try again."
                    : "An error occurred. Please try again later."
            );
        } finally {
            setIsSubmitting(false);
        }
    }

    if (isCheckingStatus) {
        return (
            <div className="min-h-screen flex items-center justify-center p-4">
                <div className="w-full max-w-md p-8 rounded-2xl bg-[var(--color-surface)] border border-[var(--color-border)]">
                    <div className="flex flex-col items-center mb-8">
                        <Skeleton className="h-8 w-8 mb-4 rounded-full" />
                        <Skeleton className="h-6 w-1/2 mb-1" />
                        <Skeleton className="h-4 w-3/4" />
                    </div>
                    <div className="space-y-4">
                        <Skeleton className="h-10 w-full rounded-lg" />
                        <Skeleton className="h-10 w-full rounded-lg" />
                    </div>
                    <Skeleton className="h-12 w-full rounded-lg mt-8" />
                </div>
            </div>
        );
    }

    if (authLoading) {
        return (
            <div className="flex min-h-screen items-center justify-center">
                <Loader2 className="h-8 w-8 animate-spin text-[var(--color-accent)]" />
            </div>
        );
    }

    return (
        <div className="flex min-h-screen items-center justify-center px-4">
            <div className="glass-panel w-full max-w-md p-8 sm:p-10">
                {/* Header */}
                <div className="text-center mb-8">
                    <h1 className="text-3xl font-bold tracking-tight mb-1">
                        The Replicant
                    </h1>
                    <p className="font-mono text-xs text-[var(--color-text-muted)]">
                        [ SYSTEM ACCESS ]
                    </p>
                </div>

                {/* Error Message */}
                {error && (
                    <div className="mb-6 flex items-center gap-3 rounded-lg bg-red-500/10 border border-red-500/20 px-4 py-3 text-sm text-red-400">
                        <AlertCircle className="h-4 w-4 shrink-0" />
                        <span>{error}</span>
                    </div>
                )}

                {/* Login Form */}
                <form onSubmit={handleSubmit} className="space-y-5">
                    {/* Email */}
                    <div>
                        <label
                            htmlFor="login-email"
                            className="block font-mono text-xs text-[var(--color-text-muted)] mb-2 uppercase tracking-wider"
                        >
                            Email
                        </label>
                        <input
                            id="login-email"
                            type="email"
                            value={email}
                            onChange={(e) => setEmail(e.target.value)}
                            required
                            autoComplete="email"
                            autoFocus
                            placeholder="admin@thereplicant.com"
                            className="w-full rounded-lg bg-[var(--color-surface)] border border-[var(--color-border)] px-4 py-3 text-[var(--color-text)] placeholder-[var(--color-text-faint)] focus:border-[var(--color-accent)] focus:outline-none transition-colors"
                        />
                    </div>

                    {/* Password */}
                    <div>
                        <label
                            htmlFor="login-password"
                            className="block font-mono text-xs text-[var(--color-text-muted)] mb-2 uppercase tracking-wider"
                        >
                            Password
                        </label>
                        <div className="relative">
                            <input
                                id="login-password"
                                type={showPassword ? "text" : "password"}
                                value={password}
                                onChange={(e) => setPassword(e.target.value)}
                                required
                                autoComplete="current-password"
                                placeholder="••••••••••••"
                                className="w-full rounded-lg bg-[var(--color-surface)] border border-[var(--color-border)] px-4 py-3 pr-12 text-[var(--color-text)] placeholder-[var(--color-text-faint)] focus:border-[var(--color-accent)] focus:outline-none transition-colors"
                            />
                            <button
                                type="button"
                                onClick={() => setShowPassword(!showPassword)}
                                className="absolute right-3 top-1/2 -translate-y-1/2 text-[var(--color-text-faint)] hover:text-[var(--color-text-muted)] transition-colors"
                                aria-label={showPassword ? "Hide password" : "Show password"}
                            >
                                {showPassword ? (
                                    <EyeOff className="h-5 w-5" />
                                ) : (
                                    <Eye className="h-5 w-5" />
                                )}
                            </button>
                        </div>
                    </div>

                    {/* Submit */}
                    <button
                        type="submit"
                        disabled={isSubmitting}
                        className="w-full rounded-lg bg-[var(--color-accent)] px-6 py-3 font-mono font-semibold text-sm text-[var(--color-void)] hover:bg-[var(--color-accent-hover)] disabled:opacity-50 disabled:cursor-not-allowed transition-colors"
                    >
                        {isSubmitting ? (
                            <span className="flex items-center justify-center gap-2">
                                <Loader2 className="h-4 w-4 animate-spin" />
                                AUTHENTICATING...
                            </span>
                        ) : (
                            "[ AUTHENTICATE ]"
                        )}
                    </button>
                </form>
            </div>
        </div>
    );
}
