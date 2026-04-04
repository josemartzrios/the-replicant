import { NextResponse } from "next/server";
import type { NextRequest } from "next/server";

/**
 * Server-side admin route guard.
 *
 * Checks for the presence of the httpOnly accessToken cookie before
 * rendering any /admin/* page. Redirects to /login if missing.
 *
 * Note: This does NOT validate the JWT signature — the backend enforces that.
 * This prevents unauthenticated requests from receiving admin HTML at all.
 */
export function middleware(request: NextRequest) {
    const accessToken = request.cookies.get("accessToken");

    if (!accessToken?.value) {
        const loginUrl = new URL("/login", request.url);
        loginUrl.searchParams.set("from", request.nextUrl.pathname);
        return NextResponse.redirect(loginUrl);
    }

    return NextResponse.next();
}

export const config = {
    matcher: "/admin/:path*",
};
