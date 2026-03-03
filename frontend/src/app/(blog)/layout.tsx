"use client";

/**
 * Public Blog Layout — 3-column responsive layout.
 *
 * Desktop (lg+): Sidebar | Main Content | (right gutter)
 * Tablet (md-lg): Main Content only (with mobile header)
 * Mobile (<md): Single column (with mobile header)
 *
 * Fetches categories once for sidebar and mobile header.
 */

import { useState, useEffect, type ReactNode } from "react";
import { Sidebar } from "@/components/blog/Sidebar";
import { MobileHeader } from "@/components/blog/MobileHeader";
import { api } from "@/lib/api";
import type { CategoryDTO } from "@/lib/types";

export default function BlogLayout({ children }: { children: ReactNode }) {
    const [categories, setCategories] = useState<CategoryDTO[]>([]);

    useEffect(() => {
        async function fetchCategories() {
            try {
                const response = await api.getCategories();
                setCategories(response.data);
            } catch {
                // Silently fail — categories are optional for layout
            }
        }
        fetchCategories();
    }, []);

    return (
        <>
            {/* Narrative: Phase 1 — Hidden HTML comment for "archaeologists" */}
            {/* <!--
        Sistema iniciado.
        Observando.
        Aprendiendo.
      --> */}

            <MobileHeader categories={categories} />
            <div className="flex min-h-screen">
                <Sidebar categories={categories} />
                <main className="flex-1 min-w-0 p-4 sm:p-6 lg:p-8">{children}</main>
            </div>
        </>
    );
}
