"use client";

/**
 * SearchBar — Debounced search input.
 *
 * Triggers search after 300ms delay, minimum 2 characters.
 * Navigates to /search?q={query}.
 */

import { useState, useCallback, useRef, useEffect } from "react";
import { useRouter } from "next/navigation";
import { Search } from "lucide-react";

interface SearchBarProps {
    initialQuery?: string;
    className?: string;
}

export function SearchBar({ initialQuery = "", className = "" }: SearchBarProps) {
    const router = useRouter();
    const [query, setQuery] = useState(initialQuery);
    const debounceRef = useRef<NodeJS.Timeout | null>(null);

    const handleSearch = useCallback(
        (value: string) => {
            if (debounceRef.current) clearTimeout(debounceRef.current);

            debounceRef.current = setTimeout(() => {
                if (value.trim().length >= 2) {
                    router.push(`/search?q=${encodeURIComponent(value.trim())}`);
                }
            }, 300);
        },
        [router]
    );

    useEffect(() => {
        return () => {
            if (debounceRef.current) clearTimeout(debounceRef.current);
        };
    }, []);

    function handleChange(value: string) {
        setQuery(value);
        handleSearch(value);
    }

    return (
        <div className={`relative ${className}`}>
            <Search className="absolute left-3 top-1/2 -translate-y-1/2 h-4 w-4 text-[var(--color-text-faint)]" />
            <input
                type="search"
                value={query}
                onChange={(e) => handleChange(e.target.value)}
                placeholder="Search posts..."
                className="w-full rounded-lg bg-[var(--color-surface)] border border-[var(--color-border)] pl-10 pr-4 py-2.5 text-sm text-[var(--color-text)] placeholder-[var(--color-text-faint)] focus:border-[var(--color-accent)] focus:outline-none transition-colors"
            />
        </div>
    );
}
