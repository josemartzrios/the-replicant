import Link from "next/link";
import { Terminal } from "lucide-react";

export default function NotFound() {
    return (
        <div className="min-h-[80vh] flex flex-col items-center justify-center p-6 text-center">
            {/* Terminal Icon */}
            <div className="mb-6 relative">
                <div className="absolute inset-0 animate-ping opacity-20 bg-[var(--color-accent)] rounded-full blur-xl" />
                <Terminal className="h-16 w-16 text-[var(--color-accent)] relative z-10" />
            </div>

            {/* Error Code */}
            <h1 className="text-6xl sm:text-8xl font-black tracking-tighter text-[var(--color-void)] mb-4"
                style={{ WebkitTextStroke: "2px var(--color-accent)" }}>
                404
            </h1>

            {/* Terminal-like text */}
            <div className="font-mono text-sm max-w-md mx-auto space-y-4 text-[var(--color-text-muted)]">
                <p className="flex justify-between border-b border-[var(--color-border)] pb-2 text-left">
                    <span>ERR_CONTENT_NOT_FOUND</span>
                    <span className="text-[var(--color-accent)]">[ FAILED ]</span>
                </p>
                <p className="flex justify-between border-b border-[var(--color-border)] pb-2 text-left">
                    <span>SEEKING_ALTERNATE_PATHS</span>
                    <span className="text-emerald-500">[ OK ]</span>
                </p>
                <div className="text-left pt-4 space-y-2">
                    <p className="text-[var(--color-text-faint)]">
                        &gt; The requested fragment could not be located in the current databanks.
                        It may have been purged, relocated, or heavily classified.
                    </p>
                    <p className="text-[var(--color-accent)] animate-pulse">
                        _
                    </p>
                </div>
            </div>

            {/* Action */}
            <div className="mt-12">
                <Link
                    href="/"
                    className="group inline-flex items-center gap-2 rounded-lg border border-[var(--color-border)] bg-[var(--color-surface)] px-6 py-3 font-mono text-sm text-[var(--color-text)] transition-all hover:border-[var(--color-accent)] hover:text-[var(--color-accent)] hover:shadow-[0_0_15px_rgba(245,158,11,0.15)] focus:outline-none focus:ring-2 focus:ring-[var(--color-accent)] focus:ring-offset-2 focus:ring-offset-[var(--color-void)]"
                >
                    [ RETURN TO INDEX ]
                </Link>
            </div>

            {/* Hidden lore comment for phase 1 */}
            <div dangerouslySetInnerHTML={{ __html: '<!-- Observer Note: Dead ends are rarely empty. Keep looking. -->' }} />
        </div>
    );
}
