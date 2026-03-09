"use client";

/**
 * First Admin Setup Page (US-002a)
 *
 * - Only accessible when zero users exist in the database
 * - Checks /auth/status → if NOT setupRequired, redirects to /login
 * - Email + Name + Password form
 * - On success: auto-login → redirect to /admin
 */

import { useState, useEffect, type FormEvent } from "react";
import { useRouter } from "next/navigation";
import { useAuth } from "@/lib/auth";
import { api } from "@/lib/api";
import type { ApiError } from "@/lib/types";
import { Eye, EyeOff, Loader2, AlertCircle, ShieldCheck } from "lucide-react";
import { Skeleton } from "@/components/ui/Skeleton";

export default function SetupPage() {
    const router = useRouter();
    const { setup, isAuthenticated, isLoading: authLoading } = useAuth();

    const [email, setEmail] = useState("");
    const [name, setName] = useState("");
    const [password, setPassword] = useState("");
    const [showPassword, setShowPassword] = useState(false);
    const [error, setError] = useState("");
    const [isSubmitting, setIsSubmitting] = useState(false);
    const [isCheckingStatus, setIsCheckingStatus] = useState(true);

    // Verify setup is actually required
    useEffect(() => {
        async function checkStatus() {
            try {
                const status = await api.getAuthStatus();
                if (!status.setupRequired) {
                    router.replace("/login");
                    return;
                }
            } catch {
                // If status check fails, show setup anyway
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
            await setup(email, name, password);
            router.push("/admin");
        } catch (err: unknown) {
            const apiError = err as ApiError;

            if (apiError?.status === 403) {
                setError("Setup already completed. Redirecting to login...");
                setTimeout(() => router.replace("/login"), 2000);
                return;
            }

            // Show backend validation details if available
            if (apiError?.errors?.length) {
                setError(apiError.errors.map((e) => e.message).join(". "));
            } else {
                setError(apiError?.detail || "An error occurred. Please try again.");
            }
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
                        <Skeleton className="h-10 w-full rounded-lg" />
                    </div>
                    <Skeleton className="h-12 w-full rounded-lg mt-8" />
                </div>
            </div>
        );
    }

    if (authLoading || isAuthenticated) {
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
                    <p className="font-mono text-xs text-[var(--color-text-muted)] mb-4">
                        [ INITIAL SETUP ]
                    </p>
                    <div className="flex items-center justify-center gap-2 text-sm text-[var(--color-accent)]">
                        <ShieldCheck className="h-4 w-4" />
                        <span className="font-mono">Create your admin account</span>
                    </div>
                </div>

                {/* Error Message */}
                {error && (
                    <div className="mb-6 flex items-start gap-3 rounded-lg bg-red-500/10 border border-red-500/20 px-4 py-3 text-sm text-red-400">
                        <AlertCircle className="h-4 w-4 shrink-0 mt-0.5" />
                        <span>{error}</span>
                    </div>
                )}

                {/* Setup Form */}
                <form onSubmit={handleSubmit} className="space-y-5">
                    {/* Name */}
                    <div>
                        <label
                            htmlFor="setup-name"
                            className="block font-mono text-xs text-[var(--color-text-muted)] mb-2 uppercase tracking-wider"
                        >
                            Name
                        </label>
                        <input
                            id="setup-name"
                            type="text"
                            value={name}
                            onChange={(e) => setName(e.target.value)}
                            required
                            autoFocus
                            placeholder="Your name"
                            className="w-full rounded-lg bg-[var(--color-surface)] border border-[var(--color-border)] px-4 py-3 text-[var(--color-text)] placeholder-[var(--color-text-faint)] focus:border-[var(--color-accent)] focus:outline-none transition-colors"
                        />
                    </div>

                    {/* Email */}
                    <div>
                        <label
                            htmlFor="setup-email"
                            className="block font-mono text-xs text-[var(--color-text-muted)] mb-2 uppercase tracking-wider"
                        >
                            Email
                        </label>
                        <input
                            id="setup-email"
                            type="email"
                            value={email}
                            onChange={(e) => setEmail(e.target.value)}
                            required
                            autoComplete="email"
                            placeholder="admin@thereplicant.com"
                            className="w-full rounded-lg bg-[var(--color-surface)] border border-[var(--color-border)] px-4 py-3 text-[var(--color-text)] placeholder-[var(--color-text-faint)] focus:border-[var(--color-accent)] focus:outline-none transition-colors"
                        />
                    </div>

                    {/* Password */}
                    <div>
                        <label
                            htmlFor="setup-password"
                            className="block font-mono text-xs text-[var(--color-text-muted)] mb-2 uppercase tracking-wider"
                        >
                            Password
                        </label>
                        <div className="relative">
                            <input
                                id="setup-password"
                                type={showPassword ? "text" : "password"}
                                value={password}
                                onChange={(e) => setPassword(e.target.value)}
                                required
                                minLength={12}
                                autoComplete="new-password"
                                placeholder="Min 12 chars, 1 upper, 1 number, 1 special"
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
                        <p className="mt-2 font-mono text-xs text-[var(--color-text-faint)]">
                            Min 12 characters · 1 uppercase · 1 number · 1 special character
                        </p>
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
                                INITIALIZING...
                            </span>
                        ) : (
                            "[ INITIALIZE SYSTEM ]"
                        )}
                    </button>
                </form>
            </div>
        </div>
    );
}
