import { Metadata } from "next";
import Link from "next/link";
import { SearchBar } from "@/components/blog/SearchBar";
import { PostListClient } from "@/components/blog/PostListClient";
import { api } from "@/lib/api";
import type { PostDTO, PaginationMeta } from "@/lib/types";
import { SITE_NAME } from "@/lib/constants";
import { SearchX, FolderOpen } from "lucide-react";

interface PageProps {
    searchParams: Promise<{ q?: string }>;
}

export async function generateMetadata({ searchParams }: PageProps): Promise<Metadata> {
    const resolvedParams = await searchParams;
    const query = resolvedParams.q || "";
    return {
        title: query ? `Search: ${query} | ${SITE_NAME}` : `Search | ${SITE_NAME}`,
        description: "Search for articles on The Replicant blog.",
        robots: "noindex, nofollow", // Prevents indexing of infinite search permutations
    };
}

export default async function SearchPage({ searchParams }: PageProps) {
    const resolvedParams = await searchParams;
    const query = resolvedParams.q || "";

    let initialPosts: PostDTO[] = [];
    let initialMeta: PaginationMeta = { page: 0, size: 10, totalElements: 0, totalPages: 0, hasNext: false, hasPrevious: false, timestamp: new Date().toISOString() };

    // Only search if query >= 2 chars
    const hasSearched = query.length >= 2;

    if (hasSearched) {
        try {
            // We use no-store for Search as it is highly dynamic
            const response = await api.searchPosts(query, 0, 10, { cache: "no-store" });
            initialPosts = response.data;
            initialMeta = response.meta;
        } catch {
            // Fail silently
        }
    }

    return (
        <div className="max-w-2xl mx-auto">
            <h1 className="text-2xl font-bold tracking-tight mb-1">Search</h1>
            <p className="font-mono text-xs text-[var(--color-text-faint)] mb-6">
                [ QUERY DATABASE ]
            </p>

            <SearchBar initialQuery={query} className="mb-8" />

            {/* Results */}
            {hasSearched && initialPosts.length > 0 && (
                <>
                    <p className="font-mono text-xs text-[var(--color-text-faint)] mb-4">
                        {initialMeta.totalElements} result{initialMeta.totalElements !== 1 ? "s" : ""} for &quot;{query}&quot;
                    </p>
                    <PostListClient
                        initialPosts={initialPosts}
                        initialMeta={initialMeta}
                        endpoint="searchPosts"
                        fetchParams={{ query }}
                    />
                </>
            )}

            {/* No Results */}
            {hasSearched && initialPosts.length === 0 && (
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
            {!hasSearched && (
                <div className="text-center py-16">
                    <p className="text-sm text-[var(--color-text-faint)]">
                        Type at least 2 characters to search
                    </p>
                </div>
            )}
        </div>
    );
}
