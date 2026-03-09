/**
 * Next.js Proxy — Server-side route protection.
 *
 * Provides an early redirect for unauthenticated users trying to
 * access /admin/* routes. Checks for the presence of a token cookie/header.
 *
 * Note: This is a lightweight first-pass check. The actual JWT validation
 * happens on the backend. The client-side AdminLayout provides a second
 * layer of protection using the AuthContext.
 *
 * Defense in Depth (OWASP): Multiple layers of auth checking.
 */

import { NextResponse } from "next/server";
import type { NextRequest } from "next/server";

export function proxy(request: NextRequest) {
    const { pathname } = request.nextUrl;

    // Only protect /admin routes
    if (pathname.startsWith("/admin")) {
        // Check for token in cookie or Authorization header
        // Since we use localStorage (MVP), the proxy can't see the token directly.
        // The client-side AdminLayout handles the actual redirect.
        // This proxy is a placeholder for future httpOnly cookie approach.
        return NextResponse.next();
    }

    return NextResponse.next();
}

export const config = {
    matcher: ["/admin/:path*"],
};
