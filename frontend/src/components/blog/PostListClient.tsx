"use client";

/**
 * PostListClient — Reusable Client Component for Pagination
 *
 * Renders the initial list of posts passed from the Server Component
 * and handles "Load More" fetching natively.
 */

import { useState } from "react";
import { PostCard } from "@/components/blog/PostCard";
import { api } from "@/lib/api";
import type { PostDTO, PaginationMeta } from "@/lib/types";
import { Loader2, BookOpen } from "lucide-react";

interface PostListClientProps {
    initialPosts: PostDTO[];
    initialMeta: PaginationMeta;
    endpoint: "getPosts" | "searchPosts";
    fetchParams?: {
        category?: string;
        tag?: string;
        query?: string;
    };
    emptyMessage?: string;
}

export function PostListClient({
    initialPosts,
    initialMeta,
    endpoint,
    fetchParams,
    emptyMessage = "No posts published yet.",
}: PostListClientProps) {
    const [posts, setPosts] = useState<PostDTO[]>(initialPosts);
    const [meta, setMeta] = useState<PaginationMeta>(initialMeta);
    const [isLoadingMore, setIsLoadingMore] = useState(false);

    async function handleLoadMore() {
        if (!meta.hasNext) return;

        setIsLoadingMore(true);
        const nextPage = meta.page + 1;

        try {
            let response;
            if (endpoint === "searchPosts") {
                response = await api.searchPosts(fetchParams?.query || "", nextPage, 10);
            } else {
                response = await api.getPosts(
                    nextPage,
                    10,
                    fetchParams?.category,
                    fetchParams?.tag
                );
            }

            setPosts((prev) => [...prev, ...response.data]);
            setMeta(response.meta);
        } catch (error) {
            console.error("Failed to load more posts", error);
        } finally {
            setIsLoadingMore(false);
        }
    }

    if (posts.length === 0) {
        return (
            <div className="text-center py-16">
                <BookOpen className="h-12 w-12 text-[var(--color-text-faint)] mx-auto mb-4" />
                <p className="text-[var(--color-text-muted)]">{emptyMessage}</p>
                <p className="text-sm text-[var(--color-text-faint)] mt-1">
                    Content will appear here once published.
                </p>
            </div>
        );
    }

    return (
        <>
            <div className="space-y-4">
                {posts.map((post) => (
                    <PostCard key={post.id} post={post} />
                ))}
            </div>

            {meta.hasNext && (
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
        </>
    );
}
