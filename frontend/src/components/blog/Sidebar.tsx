"use client";

/**
 * Sidebar — Left navigation for public pages.
 *
 * Includes: logo/brand, navigation links, category list with post counts.
 * Responsive: hidden on mobile, visible on tablet+.
 */

import Link from "next/link";
import { usePathname } from "next/navigation";
import type { CategoryDTO } from "@/lib/types";
import { Home, Search, FolderOpen, Terminal } from "lucide-react";

interface SidebarProps {
    categories: CategoryDTO[];
}

const navLinks = [
    { href: "/", label: "Home", icon: Home },
    { href: "/search", label: "Search", icon: Search },
];

export function Sidebar({ categories }: SidebarProps) {
    const pathname = usePathname();

    return (
        <aside className="sticky top-0 h-screen w-56 shrink-0 border-r border-[var(--color-border)] bg-[var(--color-surface)] p-5 hidden lg:block overflow-y-auto">
            {/* Brand */}
            <Link href="/" className="flex items-center gap-2 mb-8">
                <Terminal className="h-5 w-5 text-[var(--color-accent)]" />
                <span className="font-bold text-sm tracking-tight">The Replicant</span>
            </Link>

            {/* Navigation */}
            <nav className="space-y-1 mb-8">
                {navLinks.map(({ href, label, icon: Icon }) => {
                    const isActive = pathname === href;
                    return (
                        <Link
                            key={href}
                            href={href}
                            className={`flex items-center gap-3 rounded-lg px-3 py-2.5 font-mono text-xs transition-colors ${isActive
                                    ? "bg-[var(--color-accent-muted)] text-[var(--color-accent)]"
                                    : "text-[var(--color-text-muted)] hover:text-[var(--color-text)] hover:bg-[var(--color-surface-hover)]"
                                }`}
                        >
                            <Icon className="h-4 w-4" />
                            {label}
                        </Link>
                    );
                })}
            </nav>

            {/* Categories */}
            {categories.length > 0 && (
                <div>
                    <h3 className="font-mono text-[10px] text-[var(--color-text-faint)] uppercase tracking-widest mb-3 px-3">
                        Categories
                    </h3>
                    <ul className="space-y-0.5">
                        {categories.map((cat) => {
                            const isActive = pathname === `/category/${cat.slug}`;
                            return (
                                <li key={cat.id}>
                                    <Link
                                        href={`/category/${cat.slug}`}
                                        className={`flex items-center justify-between rounded-lg px-3 py-2 text-xs transition-colors ${isActive
                                                ? "bg-[var(--color-accent-muted)] text-[var(--color-accent)]"
                                                : "text-[var(--color-text-muted)] hover:text-[var(--color-text)] hover:bg-[var(--color-surface-hover)]"
                                            }`}
                                    >
                                        <span className="flex items-center gap-2">
                                            <FolderOpen className="h-3.5 w-3.5" />
                                            {cat.name}
                                        </span>
                                        <span className="font-mono text-[10px] text-[var(--color-text-faint)]">
                                            {cat.postCount}
                                        </span>
                                    </Link>
                                </li>
                            );
                        })}
                    </ul>
                </div>
            )}
        </aside>
    );
}
