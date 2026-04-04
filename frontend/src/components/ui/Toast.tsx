"use client";

import { useEffect } from "react";
import { CheckCircle, XCircle, X } from "lucide-react";

interface ToastProps {
    message: string;
    type: "success" | "error";
    onClose: () => void;
}

export function Toast({ message, type, onClose }: ToastProps) {
    useEffect(() => {
        const timer = setTimeout(onClose, 4000);
        return () => clearTimeout(timer);
    }, [onClose]);

    const Icon = type === "success" ? CheckCircle : XCircle;
    const borderColor =
        type === "success" ? "border-emerald-500/30" : "border-red-500/30";
    const iconColor =
        type === "success" ? "text-emerald-400" : "text-red-400";

    return (
        <div
            role="status"
            aria-live="polite"
            aria-atomic="true"
            className="fixed bottom-6 right-6 z-50 animate-[slideUp_0.3s_ease-out]"
        >
            <div
                className={`glass-panel flex items-center gap-3 px-4 py-3 border ${borderColor} max-w-sm`}
            >
                <Icon className={`h-5 w-5 shrink-0 ${iconColor}`} aria-hidden="true" />
                <p className="text-sm text-[var(--color-text)]">{message}</p>
                <button
                    onClick={onClose}
                    aria-label="Dismiss notification"
                    className="ml-2 text-[var(--color-text-faint)] hover:text-[var(--color-text)] focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-[var(--color-accent)] rounded"
                >
                    <X className="h-3.5 w-3.5" aria-hidden="true" />
                </button>
            </div>
        </div>
    );
}
