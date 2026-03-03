/**
 * Homepage — placeholder for Phase 3.
 * Verifies that the design system, fonts, and theming work correctly.
 */
export default function Home() {
  return (
    <div className="flex min-h-screen items-center justify-center">
      <div className="glass-panel glow-amber p-12 text-center max-w-md">
        <h1 className="text-4xl font-bold tracking-tight mb-2">
          The Replicant
        </h1>
        <p className="font-mono text-sm text-[var(--color-text-muted)] mb-6">
          [ SYSTEM ONLINE ]
        </p>
        <p className="text-[var(--color-text-muted)] leading-relaxed">
          Cyberpunk developer blog.
          <br />
          Frontend MVP — Phase 1 complete.
        </p>
        <div className="mt-8 flex gap-3 justify-center">
          <a
            href="/login"
            className="inline-flex items-center px-6 py-3 rounded-xl bg-[var(--color-accent)] text-[var(--color-void)] font-mono font-semibold text-sm hover:bg-[var(--color-accent-hover)] transition-colors"
          >
            [ AUTHENTICATE ]
          </a>
        </div>
      </div>
    </div>
  );
}
