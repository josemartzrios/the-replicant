"use client";

/**
 * useSEO — Client-side dynamic meta tag management.
 *
 * Since all blog pages use "use client", we can't use Next.js
 * static metadata export. This hook dynamically sets:
 * - document.title (with template)
 * - meta description
 * - meta robots (noindex for admin/search)
 * - Open Graph tags (og:title, og:description, og:type, og:url)
 * - JSON-LD structured data (for article pages)
 *
 * Cleanup: Removes injected tags on unmount to avoid stale meta.
 */

import { useEffect } from "react";
import { SITE_NAME, BASE_URL } from "@/lib/constants";

interface SEOOptions {
    /** Page title — appended with " | The Replicant" */
    title?: string;
    /** Meta description */
    description?: string;
    /** Open Graph type: "website" or "article" */
    ogType?: "website" | "article";
    /** Canonical URL path (e.g., "/posts/my-post") */
    path?: string;
    /** Prevent search engine indexing */
    noindex?: boolean;
    /** JSON-LD structured data object */
    jsonLd?: Record<string, unknown>;
}

export function useSEO({
    title,
    description,
    ogType = "website",
    path,
    noindex = false,
    jsonLd,
}: SEOOptions) {
    // Serialize jsonLd to stabilize the useEffect dependency —
    // object references change on every render, causing unnecessary
    // cleanup/re-inject cycles that can trigger removeChild errors.
    const jsonLdString = jsonLd ? JSON.stringify(jsonLd) : undefined;

    useEffect(() => {
        const injectedTags: HTMLElement[] = [];

        // Title
        if (title) {
            document.title = `${title} | ${SITE_NAME}`;
        }

        // Helper to inject a <meta> tag
        function setMeta(property: string, content: string) {
            const existing = document.querySelector(
                `meta[property="${property}"], meta[name="${property}"]`
            );
            if (existing && existing.parentNode) {
                existing.parentNode.removeChild(existing);
            }

            const meta = document.createElement("meta");
            if (property.startsWith("og:")) {
                meta.setAttribute("property", property);
            } else {
                meta.setAttribute("name", property);
            }
            meta.setAttribute("content", content);
            document.head.appendChild(meta);
            injectedTags.push(meta);
        }

        // Description
        if (description) {
            setMeta("description", description);
            setMeta("og:description", description);
        }

        // Open Graph
        if (title) {
            setMeta("og:title", title);
        }
        setMeta("og:type", ogType);
        setMeta("og:site_name", SITE_NAME);
        if (path) {
            setMeta("og:url", `${BASE_URL}${path}`);
        }

        // Robots
        if (noindex) {
            setMeta("robots", "noindex, nofollow");
        }

        // JSON-LD
        if (jsonLdString) {
            const script = document.createElement("script");
            script.type = "application/ld+json";
            script.textContent = jsonLdString;
            document.head.appendChild(script);
            injectedTags.push(script);
        }

        // Cleanup on unmount — guard against already-removed nodes
        return () => {
            injectedTags.forEach((tag) => {
                if (tag.parentNode) {
                    tag.parentNode.removeChild(tag);
                }
            });
            if (title) {
                document.title = SITE_NAME;
            }
        };
    }, [title, description, ogType, path, noindex, jsonLdString]);
}
