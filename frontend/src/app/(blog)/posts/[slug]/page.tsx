import { Metadata } from "next";
import Link from "next/link";
import { notFound } from "next/navigation";
import { api } from "@/lib/api";
import { MarkdownRenderer } from "@/components/blog/MarkdownRenderer";
import { SITE_NAME, BASE_URL } from "@/lib/constants";
import {
    ArrowLeft,
    Calendar,
    Clock,
    User,
    Tag,
} from "lucide-react";

// ISR revalidation for posts (60 seconds)
export const revalidate = 60;

interface PageProps {
    params: Promise<{ slug: string }>;
}

export async function generateMetadata({ params }: PageProps): Promise<Metadata> {
    try {
        const resolvedParams = await params;
        const response = await api.getPostBySlug(resolvedParams.slug, { next: { revalidate: 60 } });
        const post = response.data;

        return {
            title: `${post.title} | ${SITE_NAME}`,
            description: post.excerpt || `Read ${post.title} on ${SITE_NAME}`,
            openGraph: {
                title: post.title,
                description: post.excerpt || `Read ${post.title} on ${SITE_NAME}`,
                type: "article",
                url: `${BASE_URL}/posts/${resolvedParams.slug}`,
                siteName: SITE_NAME,
            },
        };
    } catch {
        return {
            title: `Post Not Found | ${SITE_NAME}`,
        };
    }
}

export default async function PostDetailPage({ params }: PageProps) {
    let post;
    try {
        const resolvedParams = await params;
        const response = await api.getPostBySlug(resolvedParams.slug, { next: { revalidate: 60 } });
        post = response.data;
    } catch {
        notFound();
    }

    const publishedDate = post.publishedAt
        ? new Date(post.publishedAt).toLocaleDateString("en-US", {
            year: "numeric",
            month: "long",
            day: "numeric",
        })
        : "Draft";

    const jsonLd = {
        "@context": "https://schema.org",
        "@type": "BlogPosting",
        headline: post.title,
        description: post.excerpt || "",
        author: {
            "@type": "Person",
            name: post.author.name,
        },
        datePublished: post.publishedAt,
        dateModified: post.updatedAt || post.publishedAt,
        keywords: post.tags.map((t) => t.name).join(", "),
    };

    return (
        <article
            className="max-w-3xl mx-auto"
            data-author-verified={post.author.name.toLowerCase() === "admin user" ? "true" : "false"}
            data-system-status="nominal"
        >
            <script
                type="application/ld+json"
                dangerouslySetInnerHTML={{ __html: JSON.stringify(jsonLd) }}
            />

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
