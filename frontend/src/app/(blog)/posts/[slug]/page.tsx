"use client";

/**
 * Post Detail Page (US-011)
 *
 * Full article reading view with:
 * - Title, author, date, category, tags, reading time
 * - Markdown content rendered with syntax highlighting
 * - "Back to posts" link
 */

import { useState, useEffect } from "react";
import { useParams } from "next/navigation";
import Link from "next/link";
import { api } from "@/lib/api";
import { MarkdownRenderer } from "@/components/blog/MarkdownRenderer";
import type { PostDTO } from "@/lib/types";
import {
    Loader2,
    ArrowLeft,
    Calendar,
    Clock,
    User,
    Tag,
} from "lucide-react";

export default function PostDetailPage() {
    const params = useParams();
    const slug = params.slug as string;

    const [post, setPost] = useState<PostDTO | null>(null);
    const [isLoading, setIsLoading] = useState(true);
    const [error, setError] = useState("");

    useEffect(() => {
        async function loadPost() {
            try {
                const response = await api.getPostBySlug(slug);
                setPost(response.data);
            } catch {
                setError("Post not found.");
            } finally {
                setIsLoading(false);
            }
        }
        if (slug) loadPost();
    }, [slug]);

    if (isLoading) {
        return (
            <div className="flex items-center justify-center py-20">
                <Loader2 className="h-8 w-8 animate-spin text-[var(--color-accent)]" />
            </div>
        );
    }

    if (error || !post) {
        return (
            <div className="max-w-2xl mx-auto text-center py-20">
                <p className="text-[var(--color-text-muted)] mb-4">{error || "Post not found."}</p>
                <Link
                    href="/"
                    className="inline-flex items-center gap-2 font-mono text-sm text-[var(--color-accent)] hover:underline"
                >
                    <ArrowLeft className="h-4 w-4" />
                    Back to posts
                </Link>
            </div>
        );
    }

    const publishedDate = post.publishedAt
        ? new Date(post.publishedAt).toLocaleDateString("en-US", {
            year: "numeric",
            month: "long",
            day: "numeric",
        })
        : "Draft";

    return (
        <article className="max-w-2xl mx-auto">
            {/* Back Link */}
            <Link
                href="/"
                className="inline-flex items-center gap-2 font-mono text-xs text-[var(--color-text-muted)] hover:text-[var(--color-accent)] transition-colors mb-6"
            >
                <ArrowLeft className="h-3 w-3" />
                BACK TO POSTS
            </Link>

            {/* Category */}
            {post.category && (
                <Link
                    href={`/category/${post.category.slug}`}
                    className="block font-mono text-xs text-[var(--color-accent)] uppercase tracking-wider mb-3 hover:underline"
                >
                    {post.category.name}
                </Link>
            )}

            {/* Title */}
            <h1 className="text-3xl sm:text-4xl font-bold tracking-tight leading-tight mb-4">
                {post.title}
            </h1>

            {/* Metadata */}
            <div className="flex flex-wrap items-center gap-4 font-mono text-xs text-[var(--color-text-faint)] mb-8 pb-8 border-b border-[var(--color-border)]">
                <span className="flex items-center gap-1.5">
                    <User className="h-3 w-3" />
                    {post.author.name}
                </span>
                <span className="flex items-center gap-1.5">
                    <Calendar className="h-3 w-3" />
                    {publishedDate}
                </span>
                <span className="flex items-center gap-1.5">
                    <Clock className="h-3 w-3" />
                    {post.readingTimeMinutes} min read
                </span>
            </div>

            {/* Content */}
            <div className="mb-8">
                <MarkdownRenderer content={post.content} />
            </div>

            {/* Tags */}
            {post.tags.length > 0 && (
                <div className="flex items-center gap-2 flex-wrap pt-6 border-t border-[var(--color-border)]">
                    <Tag className="h-3.5 w-3.5 text-[var(--color-text-faint)]" />
                    {post.tags.map((tag) => (
                        <Link
                            key={tag.id}
                            href={`/tag/${tag.slug}`}
                            className="rounded bg-[var(--color-cosmic-muted)] px-2.5 py-1 font-mono text-xs text-[var(--color-cosmic)] hover:bg-[var(--color-cosmic)] hover:text-white transition-colors"
                        >
                            {tag.name}
                        </Link>
                    ))}
                </div>
            )}
        </article>
    );
}
