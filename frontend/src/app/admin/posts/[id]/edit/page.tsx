"use client";

/**
 * Edit Post Form (US-007)
 *
 * Loads existing post data by ID and pre-fills the form.
 * Supports updating title, slug, content, excerpt, category, tags, status.
 */

import { useState, useEffect, type FormEvent } from "react";
import { useRouter, useParams } from "next/navigation";
import Link from "next/link";
import { api } from "@/lib/api";
import { MarkdownRenderer } from "@/components/blog/MarkdownRenderer";
import { Skeleton } from "@/components/ui/Skeleton";
import { Toast } from "@/components/ui/Toast";
import type { CategoryDTO, TagDTO, PostDTO, ApiError } from "@/lib/types";
import {
    ArrowLeft,
    Loader2,
    Eye,
    EyeOff,
    Save,
} from "lucide-react";

export default function EditPostPage() {
    const router = useRouter();
    const params = useParams();
    const postId = params.id as string;

    const [isLoadingPost, setIsLoadingPost] = useState(true);
    const [title, setTitle] = useState("");
    const [content, setContent] = useState("");
    const [excerpt, setExcerpt] = useState("");
    const [categoryId, setCategoryId] = useState("");
    const [selectedTags, setSelectedTags] = useState<string[]>([]);
    const [status, setStatus] = useState("DRAFT");
    const [showPreview, setShowPreview] = useState(false);
    const [isSubmitting, setIsSubmitting] = useState(false);

    const [categories, setCategories] = useState<CategoryDTO[]>([]);
    const [tags, setTags] = useState<TagDTO[]>([]);
    const [toast, setToast] = useState<{
        message: string;
        type: "success" | "error";
    } | null>(null);

    useEffect(() => {
        async function loadData() {
            try {
                const [catRes, tagRes] = await Promise.all([
                    api.getCategories(),
                    api.getTags(),
                ]);
                setCategories(catRes.data);
                setTags(tagRes.data);
            } catch {
                // Non-blocking
            }
        }
        loadData();
    }, []);

    // Load existing post
    useEffect(() => {
        async function loadPost() {
            try {
                // Use admin endpoint to get post by ID (including drafts)
                const response = await api.getAdminPosts(0, 100);
                const post = response.data.find((p: PostDTO) => p.id === postId);
                if (post) {
                    setTitle(post.title);
                    setContent(post.content);
                    setExcerpt(post.excerpt || "");
                    setCategoryId(post.category?.id || "");
                    setSelectedTags(post.tags.map((t) => t.name));
                    setStatus(post.status);
                }
            } catch {
                setToast({ message: "Failed to load post", type: "error" });
            } finally {
                setIsLoadingPost(false);
            }
        }
        if (postId) loadPost();
    }, [postId]);

    function toggleTag(tagName: string) {
        setSelectedTags((prev) =>
            prev.includes(tagName)
                ? prev.filter((n) => n !== tagName)
                : [...prev, tagName]
        );
    }

    async function handleSubmit(e: FormEvent) {
        e.preventDefault();
        setIsSubmitting(true);

        const payload = {
            title,
            content,
            excerpt: excerpt || undefined,
            categoryId: categoryId || undefined,
            tags: selectedTags.length > 0 ? selectedTags : undefined,
            status,
        };

        console.log("[EditPost] Sending PATCH payload:", JSON.stringify(payload, null, 2));

        try {
            await api.updatePost(postId, payload);
            setToast({ message: "Post updated successfully", type: "success" });
            setTimeout(() => router.push("/admin"), 500);
        } catch (err: unknown) {
            console.error("[EditPost] Update failed:", err);
            const apiError = err as ApiError;
            const msg =
                apiError?.errors?.[0]?.message ||
                apiError?.detail ||
                (typeof err === "object" && err !== null && "message" in err
                    ? (err as { message: string }).message
                    : "Failed to update post");
            setToast({ message: msg, type: "error" });
        } finally {
            setIsSubmitting(false);
        }
    }

    if (isLoadingPost) {
        return (
            <div className="max-w-4xl mx-auto py-12 px-4 sm:px-6 lg:px-8 space-y-6">
                <Skeleton className="h-6 w-32 mb-8" />
                <div className="space-y-4">
                    <Skeleton className="h-12 w-full" />
                    <Skeleton className="h-40 w-full" />
                    <Skeleton className="h-12 w-1/2" />
                    <Skeleton className="h-12 w-1/4" />
                </div>
            </div>
        );
    }

    return (
        <div className="max-w-3xl mx-auto" suppressHydrationWarning>
            <Link
                href="/admin"
                className="inline-flex items-center gap-2 font-mono text-xs text-[var(--color-text-muted)] hover:text-[var(--color-accent)] transition-colors mb-6"
            >
                <ArrowLeft className="h-3 w-3" />
                BACK TO POSTS
            </Link>

            <h1 className="text-2xl font-bold tracking-tight mb-6">Edit Post</h1>

            <form onSubmit={handleSubmit} className="space-y-5">
                {/* Title */}
                <div>
                    <label
                        htmlFor="post-title"
                        className="block font-mono text-xs text-[var(--color-text-muted)] mb-2 uppercase tracking-wider"
                    >
                        Title
                    </label>
                    <input
                        id="post-title"
                        type="text"
                        value={title}
                        onChange={(e) => setTitle(e.target.value)}
                        required
                        className="w-full rounded-lg bg-[var(--color-surface)] border border-[var(--color-border)] px-4 py-3 text-[var(--color-text)] focus:border-[var(--color-accent)] focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-[var(--color-accent)] focus-visible:ring-offset-1 focus-visible:ring-offset-[var(--color-surface)] transition-colors"
                    />
                </div>



                {/* Content */}
                <div>
                    <div className="flex items-center justify-between mb-2">
                        <label
                            htmlFor="post-content"
                            className="font-mono text-xs text-[var(--color-text-muted)] uppercase tracking-wider"
                        >
                            Content (Markdown)
                        </label>
                        <button
                            type="button"
                            onClick={() => setShowPreview(!showPreview)}
                            className="flex items-center gap-1.5 font-mono text-xs text-[var(--color-text-faint)] hover:text-[var(--color-accent)] transition-colors"
                        >
                            {showPreview ? (
                                <>
                                    <EyeOff className="h-3 w-3" />
                                    EDIT
                                </>
                            ) : (
                                <>
                                    <Eye className="h-3 w-3" />
                                    PREVIEW
                                </>
                            )}
                        </button>
                    </div>
                    {showPreview ? (
                        <div className="rounded-lg border border-[var(--color-border)] bg-[var(--color-surface)] p-6 min-h-[300px]">
                            {content ? (
                                <MarkdownRenderer content={content} />
                            ) : (
                                <p className="text-[var(--color-text-faint)] text-sm">
                                    Nothing to preview…
                                </p>
                            )}
                        </div>
                    ) : (
                        <textarea
                            id="post-content"
                            value={content}
                            onChange={(e) => setContent(e.target.value)}
                            required
                            rows={14}
                            className="w-full rounded-lg bg-[var(--color-surface)] border border-[var(--color-border)] px-4 py-3 font-mono text-sm text-[var(--color-text)] focus:border-[var(--color-accent)] focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-[var(--color-accent)] focus-visible:ring-offset-1 focus-visible:ring-offset-[var(--color-surface)] transition-colors resize-y"
                        />
                    )}
                </div>

                {/* Excerpt */}
                <div>
                    <label
                        htmlFor="post-excerpt"
                        className="block font-mono text-xs text-[var(--color-text-muted)] mb-2 uppercase tracking-wider"
                    >
                        Excerpt
                    </label>
                    <textarea
                        id="post-excerpt"
                        value={excerpt}
                        onChange={(e) => setExcerpt(e.target.value)}
                        rows={2}
                        className="w-full rounded-lg bg-[var(--color-surface)] border border-[var(--color-border)] px-4 py-3 text-sm text-[var(--color-text)] focus:border-[var(--color-accent)] focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-[var(--color-accent)] focus-visible:ring-offset-1 focus-visible:ring-offset-[var(--color-surface)] transition-colors resize-y"
                    />
                </div>

                {/* Category + Status */}
                <div className="grid grid-cols-1 sm:grid-cols-2 gap-5">
                    <div>
                        <label
                            htmlFor="post-category"
                            className="block font-mono text-xs text-[var(--color-text-muted)] mb-2 uppercase tracking-wider"
                        >
                            Category
                        </label>
                        <select
                            id="post-category"
                            value={categoryId}
                            onChange={(e) => setCategoryId(e.target.value)}
                            className="w-full rounded-lg bg-[var(--color-surface)] border border-[var(--color-border)] px-4 py-3 text-sm text-[var(--color-text)] focus:border-[var(--color-accent)] focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-[var(--color-accent)] focus-visible:ring-offset-1 focus-visible:ring-offset-[var(--color-surface)] transition-colors"
                        >
                            <option value="">None</option>
                            {categories.map((cat) => (
                                <option key={cat.id} value={cat.id}>
                                    {cat.name}
                                </option>
                            ))}
                        </select>
                    </div>

                    <div>
                        <label
                            htmlFor="post-status"
                            className="block font-mono text-xs text-[var(--color-text-muted)] mb-2 uppercase tracking-wider"
                        >
                            Status
                        </label>
                        <select
                            id="post-status"
                            value={status}
                            onChange={(e) => setStatus(e.target.value)}
                            className="w-full rounded-lg bg-[var(--color-surface)] border border-[var(--color-border)] px-4 py-3 text-sm text-[var(--color-text)] focus:border-[var(--color-accent)] focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-[var(--color-accent)] focus-visible:ring-offset-1 focus-visible:ring-offset-[var(--color-surface)] transition-colors"
                        >
                            <option value="DRAFT">Draft</option>
                            <option value="PUBLISHED">Published</option>
                        </select>
                    </div>
                </div>

                {/* Tags */}
                {tags.length > 0 && (
                    <div>
                        <p className="font-mono text-xs text-[var(--color-text-muted)] mb-2 uppercase tracking-wider">
                            Tags
                        </p>
                        <div className="flex flex-wrap gap-2">
                            {tags.map((tag) => {
                                const isSelected = selectedTags.includes(tag.name);
                                return (
                                    <button
                                        key={tag.id}
                                        type="button"
                                        onClick={() => toggleTag(tag.name)}
                                        className={`rounded-lg px-3 py-1.5 font-mono text-xs transition-colors ${isSelected
                                            ? "bg-[var(--color-cosmic)] text-white"
                                            : "bg-[var(--color-cosmic-muted)] text-[var(--color-cosmic)] hover:bg-[var(--color-cosmic)] hover:text-white"
                                            }`}
                                    >
                                        {tag.name}
                                    </button>
                                );
                            })}
                        </div>
                    </div>
                )}

                {/* Submit */}
                <div className="flex justify-end pt-4">
                    <button
                        type="submit"
                        disabled={isSubmitting}
                        className="flex items-center gap-2 rounded-lg bg-[var(--color-accent)] px-6 py-3 font-mono text-sm font-semibold text-[var(--color-void)] hover:bg-[var(--color-accent-hover)] disabled:opacity-50 transition-colors"
                    >
                        {isSubmitting ? (
                            <>
                                <Loader2 className="h-4 w-4 animate-spin" />
                                SAVING…
                            </>
                        ) : (
                            <>
                                <Save className="h-4 w-4" />
                                UPDATE POST
                            </>
                        )}
                    </button>
                </div>
            </form>

            {toast && (
                <Toast
                    message={toast.message}
                    type={toast.type}
                    onClose={() => setToast(null)}
                />
            )}
        </div>
    );
}
