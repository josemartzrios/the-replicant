"use client";

/**
 * Homepage — Post Listing (US-010)
 *
 * Displays published blog posts in a card grid.
 * "Load more" pagination.
 *
 * Narrative Phase 1: Pure blog experience. No lore visible to casual readers.
 */

import { useState, useEffect } from "react";
import { PostCard } from "@/components/blog/PostCard";
import { SearchBar } from "@/components/blog/SearchBar";
import { api } from "@/lib/api";
import type { PostDTO, PaginationMeta } from "@/lib/types";
import { Loader2, BookOpen } from "lucide-react";

export default function HomePage() {
    const [posts, setPosts] = useState<PostDTO[]>([]);
    const [meta, setMeta] = useState<PaginationMeta | null>(null);
    const [isLoading, setIsLoading] = useState(true);
    const [isLoadingMore, setIsLoadingMore] = useState(false);
    const [error, setError] = useState("");

    useEffect(() => {
        loadPosts(0);
    }, []);

    async function loadPosts(page: number) {
        try {
            const response = await api.getPosts(page, 10);
            if (page === 0) {
                setPosts(response.data);
            } else {
                setPosts((prev) => [...prev, ...response.data]);
            }
            setMeta(response.meta);
        } catch {
            setError("Failed to load posts. Please try again.");
        } finally {
            setIsLoading(false);
            setIsLoadingMore(false);
        }
    }

    function handleLoadMore() {
        if (meta?.hasNext) {
            setIsLoadingMore(true);
            loadPosts(meta.page + 1);
        }
    }

    if (isLoading) {
        return (
            <div className="flex items-center justify-center py-20">
                <Loader2 className="h-8 w-8 animate-spin text-[var(--color-accent)]" />
            </div>
        );
    }

    return (
        <div className="max-w-2xl mx-auto">
            {/* Header */}
            <div className="mb-8">
                <h1 className="text-2xl font-bold tracking-tight mb-1">Latest Posts</h1>
                <p className="font-mono text-xs text-[var(--color-text-faint)]">
                    [ {meta?.totalElements ?? 0} ENTRIES ]
                </p>
            </div>

            {/* Search */}
            <SearchBar className="mb-6" />

            {/* Error */}
            {error && (
                <div className="rounded-lg bg-red-500/10 border border-red-500/20 px-4 py-3 text-sm text-red-400 mb-6">
                    {error}
                </div>
            )}

            {/* Posts */}
            {posts.length === 0 ? (
                <div className="text-center py-16">
                    <BookOpen className="h-12 w-12 text-[var(--color-text-faint)] mx-auto mb-4" />
                    <p className="text-[var(--color-text-muted)]">No posts published yet.</p>
                    <p className="text-sm text-[var(--color-text-faint)] mt-1">
                        Content will appear here once published.
                    </p>
                </div>
            ) : (
                <div className="space-y-4">
                    {posts.map((post) => (
                        <PostCard key={post.id} post={post} />
                    ))}
                </div>
            )}

            {/* Load More */}
            {meta?.hasNext && (
                <div className="flex justify-center mt-8">
                    <button
                        onClick={handleLoadMore}
                        disabled={isLoadingMore}
                        className="rounded-lg border border-[var(--color-border)] px-6 py-3 font-mono text-xs text-[var(--color-text-muted)] hover:text-[var(--color-accent)] hover:border-[rgba(245,158,11,0.3)] disabled:opacity-50 transition-all"
                    >
                        {isLoadingMore ? (
                            <span className="flex items-center gap-2">
                                <Loader2 className="h-3 w-3 animate-spin" />
                                LOADING...
                            </span>
                        ) : (
                            "[ LOAD MORE ]"
                        )}
                    </button>
                </div>
            )}
        </div>
    );
}
