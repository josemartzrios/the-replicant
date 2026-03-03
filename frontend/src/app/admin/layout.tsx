"use client";

/**
 * Admin Layout
 *
 * Wraps all /admin/* routes with:
 * - Auth guard (redirects to /login if not authenticated)
 * - AdminHeader (brand + logout)
 * - Loading state while checking auth
 */

import { useEffect } from "react";
import { useRouter } from "next/navigation";
import { useAuth } from "@/lib/auth";
import { AdminHeader } from "@/components/layout/AdminHeader";
import { Loader2 } from "lucide-react";

export default function AdminLayout({
    children,
}: {
    children: React.ReactNode;
}) {
    const router = useRouter();
    const { isAuthenticated, isLoading } = useAuth();

    useEffect(() => {
        if (!isLoading && !isAuthenticated) {
            router.replace("/login");
        }
    }, [isAuthenticated, isLoading, router]);

    if (isLoading) {
        return (
            <div className="flex min-h-screen items-center justify-center">
                <Loader2 className="h-8 w-8 animate-spin text-[var(--color-accent)]" />
            </div>
        );
    }

    if (!isAuthenticated) {
        return null;
    }

    return (
        <div className="min-h-screen">
            <AdminHeader />
            <main className="p-4 sm:p-6">{children}</main>
        </div>
    );
}
