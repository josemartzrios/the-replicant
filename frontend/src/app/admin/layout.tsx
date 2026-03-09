"use client";
import React, { useState, useEffect } from "react";

/**
 * Admin Layout
 *
 * Wraps all /admin/* routes with:
 * - Auth guard (redirects to /login if not authenticated)
 * - AdminHeader (brand + logout)
 * - Loading state while checking auth
 */

import { useRouter } from "next/navigation";
import { useAuth } from "@/lib/auth";
import { AdminHeader } from "@/components/layout/AdminHeader";
import { Skeleton } from "@/components/ui/Skeleton";

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

    // Simple fade-in effect to prevent raw flash without destroying the DOM tree
    const [mounted, setMounted] = useState(false);
    useEffect(() => setMounted(true), []);

    return (
        <div suppressHydrationWarning className={`min-h-screen transition-opacity duration-300 ${!mounted || isLoading ? "opacity-0" : "opacity-100"}`}>
            {mounted && isAuthenticated && (
                <>
                    <AdminHeader />
                    <main className="p-4 sm:p-6">{children}</main>
                </>
            )}
        </div>
    );
}
