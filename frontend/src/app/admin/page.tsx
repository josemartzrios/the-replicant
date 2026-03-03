/**
 * Admin Dashboard — placeholder for Phase 4.
 * Verifies that the admin layout and auth guard work correctly.
 */
export default function AdminPage() {
    return (
        <div className="max-w-4xl mx-auto">
            <h1 className="text-2xl font-bold tracking-tight mb-2">Dashboard</h1>
            <p className="text-[var(--color-text-muted)] font-mono text-sm">
                [ ADMIN PANEL — PHASE 4 ]
            </p>
            <div className="mt-8 glass-panel p-6">
                <p className="text-[var(--color-text-muted)]">
                    Post management, categories, and content tools will be available here.
                </p>
            </div>
        </div>
    );
}
