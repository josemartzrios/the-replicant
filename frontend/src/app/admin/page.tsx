"use client";

/**
 * Admin Dashboard — Post List (US-009)
 *
 * Table view of all posts with:
 * - Status filter (all, draft, published)
 * - Title, status badge, category, date, actions
 * - Edit and delete actions
 * - Create new post button
 */

import { useState, useEffect, useCallback } from "react";
import Link from "next/link";
import { api } from "@/lib/api";
import { ConfirmDialog } from "@/components/ui/ConfirmDialog";
import { Toast } from "@/components/ui/Toast";
import type { PostDTO, PaginationMeta } from "@/lib/types";
import {
    Plus,
    Pencil,
    Trash2,
    Loader2,
    FileText,
    FolderOpen,
} from "lucide-react";

type PostStatus = "all" | "DRAFT" | "PUBLISHED";

export default function AdminDashboard() {
    const [posts, setPosts] = useState<PostDTO[]>([]);
    const [meta, setMeta] = useState<PaginationMeta | null>(null);
    const [isLoading, setIsLoading] = useState(true);
    const [statusFilter, setStatusFilter] = useState<PostStatus>("all");

    // Delete state
    const [deleteTarget, setDeleteTarget] = useState<PostDTO | null>(null);
    const [isDeleting, setIsDeleting] = useState(false);

    // Toast state
    const [toast, setToast] = useState<{
        message: string;
        type: "success" | "error";
    } | null>(null);

    const loadPosts = useCallback(
        async (page = 0) => {
            setIsLoading(true);
            try {
                const status = statusFilter === "all" ? undefined : statusFilter;
                const response = await api.getAdminPosts(page, 20, status);
                setPosts(response.data);
                setMeta(response.meta);
            } catch {
                setToast({ message: "Failed to load posts", type: "error" });
            } finally {
                setIsLoading(false);
            }
        },
        [statusFilter]
    );

    useEffect(() => {
        loadPosts();
    }, [loadPosts]);

    async function handleDelete() {
        if (!deleteTarget) return;
        setIsDeleting(true);
        try {
            await api.deletePost(deleteTarget.id);
            setToast({ message: "Post deleted successfully", type: "success" });
            setDeleteTarget(null);
            loadPosts();
        } catch {
            setToast({ message: "Failed to delete post", type: "error" });
        } finally {
            setIsDeleting(false);
        }
    }

    const filters: { label: string; value: PostStatus }[] = [
        { label: "All", value: "all" },
        { label: "Draft", value: "DRAFT" },
        { label: "Published", value: "PUBLISHED" },
    ];

    return (
        <div className="max-w-5xl mx-auto">
            {/* Header */}
            <div className="flex items-center justify-between mb-6">
                <div>
                    <h1 className="text-2xl font-bold tracking-tight">Posts</h1>
                    <p className="font-mono text-xs text-[var(--color-text-faint)]">
                        [ {meta?.totalElements ?? 0} ENTRIES ]
                    </p>
                </div>
                <div className="flex items-center gap-3">
                    <Link
                        href="/admin/categories"
                        className="flex items-center gap-2 rounded-lg border border-[var(--color-border)] px-4 py-2.5 font-mono text-xs text-[var(--color-text-muted)] hover:text-[var(--color-text)] hover:border-[var(--color-accent)] transition-colors"
                    >
                        <FolderOpen className="h-4 w-4" />
                        Categories
                    </Link>
                    <Link
                        href="/admin/posts/new"
                        className="flex items-center gap-2 rounded-lg bg-[var(--color-accent)] px-4 py-2.5 font-mono text-xs font-semibold text-[var(--color-void)] hover:bg-[var(--color-accent-hover)] transition-colors"
                    >
                        <Plus className="h-4 w-4" />
                        NEW POST
                    </Link>
                </div>
            </div>

            {/* Status Filters */}
            <div className="flex gap-1 mb-6">
                {filters.map(({ label, value }) => (
                    <button
                        key={value}
                        onClick={() => setStatusFilter(value)}
                        className={`rounded-lg px-4 py-2 font-mono text-xs transition-colors ${statusFilter === value
                                ? "bg-[var(--color-accent-muted)] text-[var(--color-accent)]"
                                : "text-[var(--color-text-muted)] hover:bg-[var(--color-surface-hover)]"
                            }`}
                    >
                        {label}
                    </button>
                ))}
            </div>

            {/* Table */}
            {isLoading ? (
                <div className="flex items-center justify-center py-20">
                    <Loader2 className="h-6 w-6 animate-spin text-[var(--color-accent)]" />
                </div>
            ) : posts.length === 0 ? (
                <div className="text-center py-20">
                    <FileText className="h-12 w-12 text-[var(--color-text-faint)] mx-auto mb-4" />
                    <p className="text-[var(--color-text-muted)] mb-2">No posts yet</p>
                    <Link
                        href="/admin/posts/new"
                        className="inline-flex items-center gap-2 font-mono text-xs text-[var(--color-accent)] hover:underline"
                    >
                        <Plus className="h-3 w-3" />
                        Create your first post
                    </Link>
                </div>
            ) : (
                <div className="rounded-xl border border-[var(--color-border)] overflow-hidden">
                    <table className="w-full text-sm">
                        <thead>
                            <tr className="border-b border-[var(--color-border)] bg-[var(--color-surface)]">
                                <th className="text-left px-4 py-3 font-mono text-[10px] text-[var(--color-text-faint)] uppercase tracking-wider">
                                    Title
                                </th>
                                <th className="text-left px-4 py-3 font-mono text-[10px] text-[var(--color-text-faint)] uppercase tracking-wider hidden sm:table-cell">
                                    Status
                                </th>
                                <th className="text-left px-4 py-3 font-mono text-[10px] text-[var(--color-text-faint)] uppercase tracking-wider hidden md:table-cell">
                                    Category
                                </th>
                                <th className="text-left px-4 py-3 font-mono text-[10px] text-[var(--color-text-faint)] uppercase tracking-wider hidden md:table-cell">
                                    Date
                                </th>
                                <th className="text-right px-4 py-3 font-mono text-[10px] text-[var(--color-text-faint)] uppercase tracking-wider">
                                    Actions
                                </th>
                            </tr>
                        </thead>
                        <tbody>
                            {posts.map((post) => (
                                <tr
                                    key={post.id}
                                    className="border-b border-[var(--color-border)] last:border-0 hover:bg-[var(--color-surface-hover)] transition-colors"
                                >
                                    <td className="px-4 py-3">
                                        <span className="font-medium">{post.title}</span>
                                    </td>
                                    <td className="px-4 py-3 hidden sm:table-cell">
                                        <span
                                            className={`inline-block rounded px-2 py-0.5 font-mono text-[10px] uppercase ${post.status === "PUBLISHED"
                                                    ? "bg-emerald-500/10 text-emerald-400"
                                                    : "bg-yellow-500/10 text-yellow-400"
                                                }`}
                                        >
                                            {post.status}
                                        </span>
                                    </td>
                                    <td className="px-4 py-3 text-[var(--color-text-muted)] hidden md:table-cell">
                                        {post.category?.name ?? "—"}
                                    </td>
                                    <td className="px-4 py-3 font-mono text-xs text-[var(--color-text-faint)] hidden md:table-cell">
                                        {post.publishedAt
                                            ? new Date(post.publishedAt).toLocaleDateString("en-US", {
                                                month: "short",
                                                day: "numeric",
                                            })
                                            : "—"}
                                    </td>
                                    <td className="px-4 py-3">
                                        <div className="flex items-center justify-end gap-1">
                                            <Link
                                                href={`/admin/posts/${post.id}/edit`}
                                                className="rounded p-2 text-[var(--color-text-faint)] hover:text-[var(--color-accent)] hover:bg-[var(--color-accent-muted)] transition-colors"
                                                aria-label={`Edit ${post.title}`}
                                            >
                                                <Pencil className="h-4 w-4" />
                                            </Link>
                                            <button
                                                onClick={() => setDeleteTarget(post)}
                                                className="rounded p-2 text-[var(--color-text-faint)] hover:text-red-400 hover:bg-red-500/10 transition-colors"
                                                aria-label={`Delete ${post.title}`}
                                            >
                                                <Trash2 className="h-4 w-4" />
                                            </button>
                                        </div>
                                    </td>
                                </tr>
                            ))}
                        </tbody>
                    </table>
                </div>
            )}

            {/* Pagination */}
            {meta && meta.totalPages > 1 && (
                <div className="flex justify-center gap-2 mt-6">
                    {Array.from({ length: meta.totalPages }, (_, i) => (
                        <button
                            key={i}
                            onClick={() => loadPosts(i)}
                            className={`rounded px-3 py-1.5 font-mono text-xs transition-colors ${meta.page === i
                                    ? "bg-[var(--color-accent-muted)] text-[var(--color-accent)]"
                                    : "text-[var(--color-text-muted)] hover:bg-[var(--color-surface-hover)]"
                                }`}
                        >
                            {i + 1}
                        </button>
                    ))}
                </div>
            )}

            {/* Delete Confirmation */}
            <ConfirmDialog
                isOpen={!!deleteTarget}
                title="Delete Post"
                message={`Are you sure you want to delete "${deleteTarget?.title}"? This action cannot be undone.`}
                onConfirm={handleDelete}
                onCancel={() => setDeleteTarget(null)}
                isLoading={isDeleting}
            />

            {/* Toast */}
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
