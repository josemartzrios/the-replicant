import Link from "next/link";
import { Terminal } from "lucide-react";

export default function NotFound() {
    return (
        <div className="min-h-screen bg-[var(--color-void)] flex items-center justify-center p-4">
            <div className="max-w-md w-full border border-[var(--color-border)] rounded-lg bg-[var(--color-surface)] shadow-2xl overflow-hidden animate-in fade-in zoom-in duration-500">

                {/* Terminal Header */}
                <div className="flex items-center gap-2 px-4 py-2 border-b border-[var(--color-border)] bg-[var(--color-surface-hover)]">
                    <Terminal className="h-4 w-4 text-[var(--color-text-muted)]" />
                    <span className="font-mono text-xs text-[var(--color-text-muted)]">system_error.log</span>
                </div>

                {/* Terminal Body */}
                <div className="p-6 space-y-6">
                    <div className="space-y-4 font-mono">
                        <p className="text-red-500 font-bold text-sm">
                            <span className="mr-2">&gt;</span> ERROR 404: SECTOR NOT FOUND
                        </p>
                        <p className="text-[var(--color-text-muted)] text-sm typing-animation">
                            The neural pathway you requested has been severed or never existed.
                            Memory fragmentation detected in node <span className="text-[var(--color-accent)]">0x8F4A</span>.
                        </p>
                        <p className="text-[var(--color-text-faint)] text-xs mt-4">
                            [SYSTEM SUGGESTION]: Retreat to safe sector.
                        </p>
                    </div>

                    <Link
                        href="/"
                        className="group flex w-full items-center justify-center gap-2 rounded-lg bg-[var(--color-accent)]/10 border border-[var(--color-accent)]/50 px-4 py-3 font-mono text-sm font-semibold text-[var(--color-accent)] hover:bg-[var(--color-accent)] hover:text-[var(--color-void)] transition-all duration-300"
                    >
                        <span>[ RETURN TO MAINFRAME ]</span>
                    </Link>
                </div>

            </div>
        </div>
    );
}
