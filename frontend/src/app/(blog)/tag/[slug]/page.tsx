"use client";

/**
 * Tag Page (US-014)
 *
 * Shows all posts with a specific tag.
 * Reuses PostCard grid from homepage.
 */

import { useState, useEffect } from "react";
import { useParams } from "next/navigation";
import Link from "next/link";
import { PostCard } from "@/components/blog/PostCard";
import { api } from "@/lib/api";
import type { PostDTO, PaginationMeta } from "@/lib/types";
import { Loader2, ArrowLeft, BookOpen } from "lucide-react";

export default function TagPage() {
    const params = useParams();
    const slug = params.slug as string;

    const [posts, setPosts] = useState<PostDTO[]>([]);
    const [meta, setMeta] = useState<PaginationMeta | null>(null);
    const [isLoading, setIsLoading] = useState(true);
    const [isLoadingMore, setIsLoadingMore] = useState(false);

    useEffect(() => {
        loadPosts(0);
        // eslint-disable-next-line react-hooks/exhaustive-deps
    }, [slug]);

    async function loadPosts(page: number) {
        try {
            const response = await api.getPosts(page, 10, undefined, slug);
            if (page === 0) {
                setPosts(response.data);
            } else {
                setPosts((prev) => [...prev, ...response.data]);
            }
            setMeta(response.meta);
        } catch {
            // Fail silently
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
            <Link
                href="/"
                className="inline-flex items-center gap-2 font-mono text-xs text-[var(--color-text-muted)] hover:text-[var(--color-accent)] transition-colors mb-6"
            >
                <ArrowLeft className="h-3 w-3" />
                ALL POSTS
            </Link>

            <div className="flex items-center gap-2 mb-1">
                <span className="rounded bg-[var(--color-cosmic-muted)] px-2.5 py-1 font-mono text-xs text-[var(--color-cosmic)]">
                    #{slug.replace(/-/g, " ")}
                </span>
            </div>
            <p className="font-mono text-xs text-[var(--color-text-faint)] mb-8">
                [ {meta?.totalElements ?? 0} ENTRIES ]
            </p>

            {posts.length === 0 ? (
                <div className="text-center py-16">
                    <BookOpen className="h-12 w-12 text-[var(--color-text-faint)] mx-auto mb-4" />
                    <p className="text-[var(--color-text-muted)]">
                        No posts with this tag yet.
                    </p>
                </div>
            ) : (
                <div className="space-y-4">
                    {posts.map((post) => (
                        <PostCard key={post.id} post={post} />
                    ))}
                </div>
            )}

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
