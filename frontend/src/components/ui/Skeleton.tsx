export function Skeleton({ className = "" }: { className?: string }) {
    return (
        <div
            className={`animate-pulse bg-[var(--color-surface-hover)] rounded ${className}`}
        />
    );
}

export function SkeletonCard({ className = "" }: { className?: string }) {
    return (
        <div className={`p-6 bg-[var(--color-surface)] rounded-xl border border-[var(--color-border)] ${className}`}>
            <Skeleton className="h-4 w-1/4 mb-4" />
            <Skeleton className="h-6 w-3/4 mb-4" />
            <Skeleton className="h-4 w-full mb-2" />
            <Skeleton className="h-4 w-5/6 mb-6" />
            <div className="flex gap-2">
                <Skeleton className="h-6 w-16" />
                <Skeleton className="h-6 w-16" />
            </div>
        </div>
    );
}

export function SkeletonTable({ rows = 5, className = "" }: { rows?: number; className?: string }) {
    return (
        <div className={`w-full overflow-hidden rounded-lg border border-[var(--color-border)] bg-[var(--color-surface)] ${className}`}>
            <div className="h-12 bg-[var(--color-border)] border-b border-[var(--color-border)]" />
            {Array.from({ length: rows }).map((_, i) => (
                <div key={i} className="flex h-16 items-center px-4 border-b border-[var(--color-border)] last:border-0">
                    <Skeleton className="h-4 w-1/3 mr-auto" />
                    <Skeleton className="h-4 w-24 mr-8" />
                    <Skeleton className="h-4 w-20" />
                </div>
            ))}
        </div>
    );
}
