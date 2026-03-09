import { Metadata } from "next";
import Link from "next/link";
import { PostListClient } from "@/components/blog/PostListClient";
import { api } from "@/lib/api";
import type { PostDTO, PaginationMeta } from "@/lib/types";
import { SITE_NAME } from "@/lib/constants";
import { ArrowLeft } from "lucide-react";

export const revalidate = 60; // ISR

interface PageProps {
    params: Promise<{ slug: string }>;
}

export async function generateMetadata({ params }: PageProps): Promise<Metadata> {
    const resolvedParams = await params;
    const slug = resolvedParams.slug;
    const categoryName = slug.replace(/-/g, " ");
    const capitalizedName = categoryName.charAt(0).toUpperCase() + categoryName.slice(1);

    return {
        title: `${capitalizedName} | ${SITE_NAME}`,
        description: `Browse all posts in the ${capitalizedName} category.`,
    };
}

export default async function CategoryPage({ params }: PageProps) {
    const resolvedParams = await params;
    const slug = resolvedParams.slug;
    const categoryName = slug.replace(/-/g, " ");

    let initialPosts: PostDTO[] = [];
    let initialMeta: PaginationMeta = {
        page: 0, size: 10, totalElements: 0, totalPages: 0, hasNext: false, hasPrevious: false, timestamp: new Date().toISOString()
    };

    try {
        const response = await api.getPosts(0, 10, slug, undefined, { next: { revalidate: 60 } });
        initialPosts = response.data;
        initialMeta = response.meta;
    } catch {
        // Fail silently
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

            <h1 className="text-2xl font-bold tracking-tight mb-1 capitalize">
                {categoryName}
            </h1>
            <p className="font-mono text-xs text-[var(--color-text-faint)] mb-8">
                [ {initialMeta.totalElements} ENTRIES ]
            </p>

            <PostListClient
                initialPosts={initialPosts}
                initialMeta={initialMeta}
                endpoint="getPosts"
                fetchParams={{ category: slug }}
                emptyMessage="No posts in this category yet."
            />
        </div>
    );
}
