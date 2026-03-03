"use client";

/**
 * PostCard — Blog post preview card for listings.
 *
 * Displays: title, excerpt, date (mono), category, tags, reading time.
 * Used on homepage, search, and category/tag filter pages.
 */

import Link from "next/link";
import type { PostDTO } from "@/lib/types";
import { Clock, Calendar } from "lucide-react";

interface PostCardProps {
    post: PostDTO;
}

export function PostCard({ post }: PostCardProps) {
    const publishedDate = post.publishedAt
        ? new Date(post.publishedAt).toLocaleDateString("en-US", {
            year: "numeric",
            month: "short",
            day: "numeric",
        })
        : "Draft";

    return (
        <Link href={`/posts/${post.slug}`} className="group block">
            <article className="rounded-xl border border-[var(--color-border)] bg-[var(--color-surface)] p-5 sm:p-6 transition-all duration-300 hover:border-[rgba(245,158,11,0.3)] hover:shadow-[0_0_15px_rgba(245,158,11,0.06)]">
                {/* Category + Reading Time */}
                <div className="flex items-center justify-between mb-3">
                    {post.category && (
                        <span className="font-mono text-xs text-[var(--color-accent)] uppercase tracking-wider">
                            {post.category.name}
                        </span>
                    )}
                    <span className="flex items-center gap-1 font-mono text-xs text-[var(--color-text-faint)]">
                        <Clock className="h-3 w-3" />
                        {post.readingTimeMinutes} min
                    </span>
                </div>

                {/* Title */}
                <h2 className="text-lg font-semibold leading-snug mb-2 group-hover:text-[var(--color-accent)] transition-colors">
                    {post.title}
                </h2>

                {/* Excerpt */}
                <p className="text-sm text-[var(--color-text-muted)] leading-relaxed mb-4 line-clamp-2">
                    {post.excerpt}
                </p>

                {/* Footer: Date + Tags */}
                <div className="flex items-center justify-between gap-3">
                    <span className="flex items-center gap-1.5 font-mono text-xs text-[var(--color-text-faint)]">
                        <Calendar className="h-3 w-3" />
                        {publishedDate}
                    </span>

                    {post.tags.length > 0 && (
                        <div className="flex gap-1.5 overflow-hidden">
                            {post.tags.slice(0, 3).map((tag) => (
                                <span
                                    key={tag.id}
                                    className="rounded bg-[var(--color-cosmic-muted)] px-2 py-0.5 font-mono text-[10px] text-[var(--color-cosmic)] whitespace-nowrap"
                                >
                                    {tag.name}
                                </span>
                            ))}
                        </div>
                    )}
                </div>
            </article>
        </Link>
    );
}
