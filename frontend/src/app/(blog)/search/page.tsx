"use client";

/**
 * Search Page (US-012)
 *
 * Search posts by keyword with debounced input.
 * Shows results with post cards or a friendly empty state.
 */

import { useState, useEffect, Suspense } from "react";
import { useSearchParams } from "next/navigation";
import Link from "next/link";
import { PostCard } from "@/components/blog/PostCard";
import { SearchBar } from "@/components/blog/SearchBar";
import { api } from "@/lib/api";
import type { PostDTO } from "@/lib/types";
import { Loader2, SearchX, FolderOpen } from "lucide-react";

function SearchContent() {
    const searchParams = useSearchParams();
    const query = searchParams.get("q") || "";

    const [posts, setPosts] = useState<PostDTO[]>([]);
    const [isLoading, setIsLoading] = useState(false);
    const [hasSearched, setHasSearched] = useState(false);

    useEffect(() => {
        async function search() {
            if (query.length < 2) {
                setPosts([]);
                setHasSearched(false);
                return;
            }

            setIsLoading(true);
            try {
                const response = await api.searchPosts(query);
                setPosts(response.data);
                setHasSearched(true);
            } catch {
                setPosts([]);
                setHasSearched(true);
            } finally {
                setIsLoading(false);
            }
        }

        search();
    }, [query]);

    return (
        <div className="max-w-2xl mx-auto">
            <h1 className="text-2xl font-bold tracking-tight mb-1">Search</h1>
            <p className="font-mono text-xs text-[var(--color-text-faint)] mb-6">
                [ QUERY DATABASE ]
            </p>

            <SearchBar initialQuery={query} className="mb-8" />

            {/* Loading */}
            {isLoading && (
                <div className="flex items-center justify-center py-16">
                    <Loader2 className="h-6 w-6 animate-spin text-[var(--color-accent)]" />
                </div>
            )}

            {/* Results */}
            {!isLoading && hasSearched && posts.length > 0 && (
                <>
                    <p className="font-mono text-xs text-[var(--color-text-faint)] mb-4">
                        {posts.length} result{posts.length !== 1 ? "s" : ""} for &quot;{query}&quot;
                    </p>
                    <div className="space-y-4">
                        {posts.map((post) => (
                            <PostCard key={post.id} post={post} />
                        ))}
                    </div>
                </>
            )}

            {/* No Results */}
            {!isLoading && hasSearched && posts.length === 0 && (
                <div className="text-center py-16">
                    <SearchX className="h-12 w-12 text-[var(--color-text-faint)] mx-auto mb-4" />
                    <p className="text-[var(--color-text-muted)] mb-2">
                        No posts found for &quot;{query}&quot;
                    </p>
                    <p className="text-sm text-[var(--color-text-faint)] mb-4">
                        Try different keywords or browse by category
                    </p>
                    <Link
                        href="/"
                        className="inline-flex items-center gap-2 font-mono text-xs text-[var(--color-accent)] hover:underline"
                    >
                        <FolderOpen className="h-3 w-3" />
                        Browse all posts
                    </Link>
                </div>
            )}

            {/* Initial State */}
            {!isLoading && !hasSearched && (
                <div className="text-center py-16">
                    <p className="text-sm text-[var(--color-text-faint)]">
                        Type at least 2 characters to search
                    </p>
                </div>
            )}
        </div>
    );
}

export default function SearchPage() {
    return (
        <Suspense
            fallback={
                <div className="flex items-center justify-center py-20">
                    <Loader2 className="h-8 w-8 animate-spin text-[var(--color-accent)]" />
                </div>
            }
        >
            <SearchContent />
        </Suspense>
    );
}
