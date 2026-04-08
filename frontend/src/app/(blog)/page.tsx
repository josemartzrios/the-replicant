import { Metadata } from "next";
import { SearchBar } from "@/components/blog/SearchBar";
import { PostListClient } from "@/components/blog/PostListClient";
import { api } from "@/lib/api";
import type { PostDTO, PaginationMeta } from "@/lib/types";
import { SITE_NAME } from "@/lib/constants";

export const metadata: Metadata = {
    title: `Latest Posts | ${SITE_NAME}`,
    description: "Explore the latest articles on technology, code, and the stories behind them.",
};

// ISR revalidation (60 seconds)
export const revalidate = 60;

export default async function HomePage() {
    let initialPosts: PostDTO[] = [];
    let initialMeta: PaginationMeta = {
        page: 0, size: 10, totalElements: 0, totalPages: 0, hasNext: false, hasPrevious: false, timestamp: new Date().toISOString()
    };

    try {
        const response = await api.getPosts(0, 10, undefined, undefined, { next: { revalidate: 60 } });
        initialPosts = response.data;
        initialMeta = response.meta;
    } catch (error) {
        console.error("Failed to fetch initial posts:", error);
    }

    return (
        <div className="max-w-2xl mx-auto">
            {/* Header */}
            <div className="mb-8">
                <h1 className="text-2xl font-bold tracking-tight mb-1">Latest Posts</h1>
                <p className="font-mono text-xs text-[var(--color-text-faint)]">
                    [ {initialMeta.totalElements} ENTRIES ]
                </p>
            </div>

            {/* Search */}
            <SearchBar className="mb-6" />

            {/* Posts & Pagination */}
            <PostListClient
                initialPosts={initialPosts}
                initialMeta={initialMeta}
                endpoint="getPosts"
            />
        </div>
    );
}
