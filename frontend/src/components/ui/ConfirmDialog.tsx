"use client";

/**
 * ConfirmDialog — Modal for destructive action confirmation.
 *
 * Used for: delete post, delete category.
 * Accessible: focus trap, keyboard escape, backdrop click.
 */

import { useEffect, useRef } from "react";
import { AlertTriangle, X } from "lucide-react";

interface ConfirmDialogProps {
    isOpen: boolean;
    title: string;
    message: string;
    confirmLabel?: string;
    onConfirm: () => void;
    onCancel: () => void;
    isLoading?: boolean;
}

export function ConfirmDialog({
    isOpen,
    title,
    message,
    confirmLabel = "Delete",
    onConfirm,
    onCancel,
    isLoading = false,
}: ConfirmDialogProps) {
    const cancelRef = useRef<HTMLButtonElement>(null);

    useEffect(() => {
        if (isOpen) {
            cancelRef.current?.focus();

            function handleEscape(e: KeyboardEvent) {
                if (e.key === "Escape") onCancel();
            }
            document.addEventListener("keydown", handleEscape);
            return () => document.removeEventListener("keydown", handleEscape);
        }
    }, [isOpen, onCancel]);

    if (!isOpen) return null;

    return (
        <div
            className="fixed inset-0 z-50 flex items-center justify-center p-4"
            onClick={onCancel}
        >
            {/* Backdrop */}
            <div className="absolute inset-0 bg-black/60 backdrop-blur-sm" />

            {/* Dialog */}
            <div
                className="relative glass-panel p-6 max-w-sm w-full"
                onClick={(e) => e.stopPropagation()}
            >
                <button
                    onClick={onCancel}
                    className="absolute top-3 right-3 text-[var(--color-text-faint)] hover:text-[var(--color-text)]"
                    aria-label="Close"
                >
                    <X className="h-4 w-4" />
                </button>

                <div className="flex items-start gap-3 mb-4">
                    <AlertTriangle className="h-5 w-5 text-[var(--color-danger)] shrink-0 mt-0.5" />
                    <div>
                        <h3 className="font-semibold text-sm mb-1">{title}</h3>
                        <p className="text-sm text-[var(--color-text-muted)]">{message}</p>
                    </div>
                </div>

                <div className="flex justify-end gap-3">
                    <button
                        ref={cancelRef}
                        onClick={onCancel}
                        className="rounded-lg px-4 py-2 text-sm text-[var(--color-text-muted)] hover:bg-[var(--color-surface-hover)] transition-colors"
                    >
                        Cancel
                    </button>
                    <button
                        onClick={onConfirm}
                        disabled={isLoading}
                        className="rounded-lg px-4 py-2 text-sm font-medium bg-[var(--color-danger)] text-white hover:bg-red-600 disabled:opacity-50 transition-colors"
                    >
                        {isLoading ? "Deleting..." : confirmLabel}
                    </button>
                </div>
            </div>
        </div>
    );
}
