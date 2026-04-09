/**
 * Next.js Middleware — Server-side route protection.
 *
 * Checks for a client-set "session" cookie before rendering any /admin/* page.
 * Redirects to /login if missing.
 *
 * Architecture note: The real auth tokens (accessToken, refreshToken) are
 * HttpOnly cookies set by the Railway backend — they live on that domain and
 * are invisible to this Vercel-side middleware. The "session" cookie is a
 * non-HttpOnly routing hint set by the frontend on the Vercel domain after a
 * successful login. It carries NO sensitive data; it only tells the middleware
 * "a login happened." Actual security is enforced by the backend on every API
 * call via JWT validation.
 *
 * Defense in Depth (OWASP): Multiple layers of auth checking.
 */

import { NextResponse } from "next/server";
import type { NextRequest } from "next/server";

export function middleware(request: NextRequest) {
    const session = request.cookies.get("session");

    if (!session?.value) {
        const loginUrl = new URL("/login", request.url);
        loginUrl.searchParams.set("from", request.nextUrl.pathname);
        return NextResponse.redirect(loginUrl);
    }

    return NextResponse.next();
}

export const config = {
    matcher: ["/admin/:path*"],
};
