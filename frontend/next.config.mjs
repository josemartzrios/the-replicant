/** @type {import('next').NextConfig} */

// Derive the backend API origin for the connect-src CSP directive.
// In production, NEXT_PUBLIC_API_URL points to Railway (https://...).
// In development, it defaults to http://localhost:8080/api/v1.
const apiUrl = process.env.NEXT_PUBLIC_API_URL || "http://localhost:8080/api/v1";
const apiOrigin = (() => {
    try {
        const url = new URL(apiUrl);
        return `${url.protocol}//${url.host}`;
    } catch {
        return "http://localhost:8080";
    }
})();

const cspHeader = `
    default-src 'self';
    script-src 'self' 'unsafe-eval' 'unsafe-inline';
    style-src 'self' 'unsafe-inline';
    img-src 'self' blob: data: https:;
    font-src 'self' data:;
    object-src 'none';
    base-uri 'self';
    form-action 'self';
    frame-ancestors 'none';
    connect-src 'self' ${apiOrigin};
    upgrade-insecure-requests;
`;

const nextConfig = {
    async headers() {
        return [
            {
                source: '/(.*)',
                headers: [
                    {
                        key: 'Content-Security-Policy',
                        value: cspHeader.replace(/\n/g, ''),
                    },
                    {
                        key: 'X-Content-Type-Options',
                        value: 'nosniff',
                    },
                    {
                        key: 'X-Frame-Options',
                        value: 'DENY',
                    },
                    {
                        key: 'X-XSS-Protection',
                        value: '1; mode=block',
                    },
                    {
                        key: 'Referrer-Policy',
                        value: 'strict-origin-when-cross-origin',
                    },
                    {
                        key: 'Permissions-Policy',
                        value: 'camera=(), microphone=(), geolocation=(), browsing-topics=()',
                    }
                ],
            },
        ];
    },
};

export default nextConfig;
