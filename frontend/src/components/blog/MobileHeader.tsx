"use client";

/**
 * MobileHeader — Top bar for mobile/tablet screens.
 *
 * Shows brand + hamburger menu toggle + search.
 * Only visible on screens < lg (1024px).
 */

import { useState } from "react";
import Link from "next/link";
import { usePathname } from "next/navigation";
import type { CategoryDTO } from "@/lib/types";
import { Menu, X, Terminal, Home, Search, FolderOpen } from "lucide-react";

interface MobileHeaderProps {
    categories: CategoryDTO[];
}

export function MobileHeader({ categories }: MobileHeaderProps) {
    const [isOpen, setIsOpen] = useState(false);
    const pathname = usePathname();

    return (
        <div className="lg:hidden">
            {/* Top Bar */}
            <header className="sticky top-0 z-50 flex h-14 items-center justify-between border-b border-[var(--color-border)] bg-[var(--color-surface)]/90 backdrop-blur-xl px-4">
                <Link href="/" className="flex items-center gap-2">
                    <Terminal className="h-5 w-5 text-[var(--color-accent)]" />
                    <span className="font-bold text-sm">The Replicant</span>
                </Link>

                <button
                    onClick={() => setIsOpen(!isOpen)}
                    className="p-2 text-[var(--color-text-muted)] hover:text-[var(--color-text)]"
                    aria-label="Toggle menu"
                >
                    {isOpen ? <X className="h-5 w-5" /> : <Menu className="h-5 w-5" />}
                </button>
            </header>

            {/* Full-screen Overlay Menu */}
            {isOpen && (
                <>
                    {/* Backdrop */}
                    <div
                        className="fixed inset-0 top-14 z-40 bg-black/60"
                        onClick={() => setIsOpen(false)}
                        aria-hidden="true"
                    />
                    {/* Menu Panel */}
                    <nav className="fixed inset-x-0 top-14 bottom-0 z-50 bg-[var(--color-surface)] overflow-y-auto p-4 space-y-1">
                        <Link
                            href="/"
                            onClick={() => setIsOpen(false)}
                            className={`flex items-center gap-3 rounded-lg px-3 py-3 font-mono text-sm ${pathname === "/"
                                ? "text-[var(--color-accent)]"
                                : "text-[var(--color-text-muted)]"
                                }`}
                        >
                            <Home className="h-4 w-4" />
                            Home
                        </Link>
                        <Link
                            href="/search"
                            onClick={() => setIsOpen(false)}
                            className={`flex items-center gap-3 rounded-lg px-3 py-3 font-mono text-sm ${pathname === "/search"
                                ? "text-[var(--color-accent)]"
                                : "text-[var(--color-text-muted)]"
                                }`}
                        >
                            <Search className="h-4 w-4" />
                            Search
                        </Link>

                        {categories.length > 0 && (
                            <>
                                <div className="border-t border-[var(--color-border)] my-2" />
                                <p className="font-mono text-[10px] text-[var(--color-text-faint)] uppercase tracking-widest px-3 pt-1">
                                    Categories
                                </p>
                                {categories.map((cat) => (
                                    <Link
                                        key={cat.id}
                                        href={`/category/${cat.slug}`}
                                        onClick={() => setIsOpen(false)}
                                        className={`flex items-center justify-between rounded-lg px-3 py-3 text-sm ${pathname === `/category/${cat.slug}`
                                            ? "text-[var(--color-accent)]"
                                            : "text-[var(--color-text-muted)]"
                                            }`}
                                    >
                                        <span className="flex items-center gap-2">
                                            <FolderOpen className="h-3.5 w-3.5" />
                                            {cat.name}
                                        </span>
                                        <span className="font-mono text-xs text-[var(--color-text-faint)]">
                                            {cat.postCount}
                                        </span>
                                    </Link>
                                ))}
                            </>
                        )}
                    </nav>
                </>
            )}
        </div>
    );
}
