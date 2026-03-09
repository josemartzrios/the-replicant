"use client";

/**
 * Category Management Page (US-015)
 *
 * CRUD operations for categories:
 * - List all categories with post count
 * - Inline create form
 * - Edit category name/slug
 * - Delete with confirmation
 */

import { useState, useEffect, useCallback, type FormEvent } from "react";
import Link from "next/link";
import { api } from "@/lib/api";
import { ConfirmDialog } from "@/components/ui/ConfirmDialog";
import { Toast } from "@/components/ui/Toast";
import type { CategoryDTO, ApiError } from "@/lib/types";
import {
    ArrowLeft,
    Plus,
    Pencil,
    Trash2,
    Check,
    X,
    FolderOpen,
} from "lucide-react";
import { SkeletonTable } from "@/components/ui/Skeleton";

export default function CategoriesPage() {
    const [categories, setCategories] = useState<CategoryDTO[]>([]);
    const [isLoading, setIsLoading] = useState(true);

    // Create state
    const [newName, setNewName] = useState("");
    const [isCreating, setIsCreating] = useState(false);
    const [showCreate, setShowCreate] = useState(false);

    // Edit state
    const [editId, setEditId] = useState<string | null>(null);
    const [editName, setEditName] = useState("");
    const [isSavingEdit, setIsSavingEdit] = useState(false);

    // Delete state
    const [deleteTarget, setDeleteTarget] = useState<CategoryDTO | null>(null);
    const [isDeleting, setIsDeleting] = useState(false);

    // Toast
    const [toast, setToast] = useState<{
        message: string;
        type: "success" | "error";
    } | null>(null);

    const loadCategories = useCallback(async () => {
        try {
            const response = await api.getAdminCategories();
            setCategories(response.data);
        } catch {
            setToast({ message: "Failed to load categories", type: "error" });
        } finally {
            setIsLoading(false);
        }
    }, []);

    useEffect(() => {
        loadCategories();
    }, [loadCategories]);

    // Create
    async function handleCreate(e: FormEvent) {
        e.preventDefault();
        setIsCreating(true);
        try {
            await api.createCategory({
                name: newName,
            });
            setToast({ message: "Category created", type: "success" });
            setNewName("");
            setShowCreate(false);
            loadCategories();
        } catch (err: unknown) {
            const apiError = err as ApiError;
            setToast({
                message: apiError?.detail || "Failed to create category",
                type: "error",
            });
        } finally {
            setIsCreating(false);
        }
    }

    // Edit
    function startEdit(cat: CategoryDTO) {
        setEditId(cat.id);
        setEditName(cat.name);
    }

    function cancelEdit() {
        setEditId(null);
        setEditName("");
    }

    async function saveEdit() {
        if (!editId) return;
        setIsSavingEdit(true);
        try {
            await api.updateCategory(editId, { name: editName });
            setToast({ message: "Category updated", type: "success" });
            cancelEdit();
            loadCategories();
        } catch (err: unknown) {
            const apiError = err as ApiError;
            setToast({
                message: apiError?.detail || "Failed to update category",
                type: "error",
            });
        } finally {
            setIsSavingEdit(false);
        }
    }

    // Delete
    async function handleDelete() {
        if (!deleteTarget) return;
        setIsDeleting(true);
        try {
            await api.deleteCategory(deleteTarget.id);
            setToast({ message: "Category deleted", type: "success" });
            setDeleteTarget(null);
            loadCategories();
        } catch {
            setToast({ message: "Failed to delete category", type: "error" });
        } finally {
            setIsDeleting(false);
        }
    }

    return (
        <div className="max-w-2xl mx-auto" suppressHydrationWarning>
            <Link
                href="/admin"
                className="inline-flex items-center gap-2 font-mono text-xs text-[var(--color-text-muted)] hover:text-[var(--color-accent)] transition-colors mb-6"
            >
                <ArrowLeft className="h-3 w-3" />
                BACK TO DASHBOARD
            </Link>

            <div className="flex items-center justify-between mb-6">
                <div>
                    <h1 className="text-2xl font-bold tracking-tight">Categories</h1>
                    <p className="font-mono text-xs text-[var(--color-text-faint)]">
                        [ {categories.length} ENTRIES ]
                    </p>
                </div>
                <button
                    onClick={() => setShowCreate(!showCreate)}
                    className="flex items-center gap-2 rounded-lg bg-[var(--color-accent)] px-4 py-2.5 font-mono text-xs font-semibold text-[var(--color-void)] hover:bg-[var(--color-accent-hover)] transition-colors"
                >
                    <Plus className="h-4 w-4" />
                    NEW
                </button>
            </div>

            {/* Create Form */}
            {showCreate && (
                <form
                    onSubmit={handleCreate}
                    className="glass-panel p-4 mb-6 space-y-3"
                >
                    <div className="space-y-3">
                        <input
                            type="text"
                            value={newName}
                            onChange={(e) => setNewName(e.target.value)}
                            required
                            placeholder="Category name"
                            className="w-full rounded-lg bg-[var(--color-surface)] border border-[var(--color-border)] px-3 py-2 text-sm text-[var(--color-text)] placeholder-[var(--color-text-faint)] focus:border-[var(--color-accent)] focus:outline-none transition-colors"
                        />
                    </div>
                    <div className="flex justify-end gap-2">
                        <button
                            type="button"
                            onClick={() => setShowCreate(false)}
                            className="rounded-lg px-3 py-2 text-xs text-[var(--color-text-muted)] hover:bg-[var(--color-surface-hover)]"
                        >
                            Cancel
                        </button>
                        <button
                            type="submit"
                            disabled={isCreating}
                            className="rounded-lg bg-[var(--color-accent)] px-4 py-2 font-mono text-xs font-semibold text-[var(--color-void)] hover:bg-[var(--color-accent-hover)] disabled:opacity-50"
                        >
                            {isCreating ? "Creating..." : "Create"}
                        </button>
                    </div>
                </form>
            )}

            {/* List */}
            {isLoading ? (
                <div className="pt-24 pb-12">
                    <div className="max-w-5xl mx-auto px-4 sm:px-6 lg:px-8">
                        <SkeletonTable rows={5} className="mt-8" />
                    </div>
                </div>
            ) : categories.length === 0 ? (
                <div className="text-center py-16">
                    <FolderOpen className="h-12 w-12 text-[var(--color-text-faint)] mx-auto mb-4" />
                    <p className="text-[var(--color-text-muted)]">No categories yet</p>
                </div>
            ) : (
                <div className="rounded-xl border border-[var(--color-border)] divide-y divide-[var(--color-border)]">
                    {categories.map((cat) => (
                        <div
                            key={cat.id}
                            className="flex items-center justify-between px-4 py-3 hover:bg-[var(--color-surface-hover)] transition-colors"
                        >
                            {editId === cat.id ? (
                                // Edit mode
                                <div className="flex items-center gap-2 flex-1 mr-2">
                                    <input
                                        type="text"
                                        value={editName}
                                        onChange={(e) => setEditName(e.target.value)}
                                        className="rounded bg-[var(--color-surface)] border border-[var(--color-border)] px-2 py-1 text-sm text-[var(--color-text)] focus:border-[var(--color-accent)] focus:outline-none flex-1"
                                    />

                                    <button
                                        onClick={saveEdit}
                                        disabled={isSavingEdit}
                                        className="rounded p-1.5 text-emerald-400 hover:bg-emerald-500/10"
                                        aria-label="Save"
                                    >
                                        <Check className="h-4 w-4" />
                                    </button>
                                    <button
                                        onClick={cancelEdit}
                                        className="rounded p-1.5 text-[var(--color-text-faint)] hover:bg-[var(--color-surface-hover)]"
                                        aria-label="Cancel"
                                    >
                                        <X className="h-4 w-4" />
                                    </button>
                                </div>
                            ) : (
                                // View mode
                                <>
                                    <div className="flex items-center gap-3">
                                        <FolderOpen className="h-4 w-4 text-[var(--color-text-faint)]" />
                                        <div>
                                            <span className="text-sm font-medium">{cat.name}</span>
                                            <span className="font-mono text-xs text-[var(--color-text-faint)] ml-2">
                                                /{cat.slug}
                                            </span>
                                        </div>
                                    </div>
                                    <div className="flex items-center gap-2">
                                        <span className="font-mono text-xs text-[var(--color-text-faint)] mr-2">
                                            {cat.postCount} posts
                                        </span>
                                        <button
                                            onClick={() => startEdit(cat)}
                                            className="rounded p-2 text-[var(--color-text-faint)] hover:text-[var(--color-accent)] hover:bg-[var(--color-accent-muted)] transition-colors"
                                            aria-label={`Edit ${cat.name}`}
                                        >
                                            <Pencil className="h-3.5 w-3.5" />
                                        </button>
                                        <button
                                            onClick={() => setDeleteTarget(cat)}
                                            className="rounded p-2 text-[var(--color-text-faint)] hover:text-red-400 hover:bg-red-500/10 transition-colors"
                                            aria-label={`Delete ${cat.name}`}
                                        >
                                            <Trash2 className="h-3.5 w-3.5" />
                                        </button>
                                    </div>
                                </>
                            )}
                        </div>
                    ))}
                </div>
            )}

            {/* Delete Confirmation */}
            <ConfirmDialog
                isOpen={!!deleteTarget}
                title="Delete Category"
                message={`Delete "${deleteTarget?.name}"? Posts in this category won't be deleted.`}
                onConfirm={handleDelete}
                onCancel={() => setDeleteTarget(null)}
                isLoading={isDeleting}
            />

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
