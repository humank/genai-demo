import { Skeleton } from '@repo/ui'

export function ProductSkeleton() {
    return (
        <div className="rounded-xl border border-stone-100 overflow-hidden">
            <Skeleton className="aspect-square w-full" />
            <div className="p-4 space-y-2">
                <Skeleton className="h-3 w-16" />
                <Skeleton className="h-4 w-full" />
                <Skeleton className="h-5 w-20 mt-2" />
                <Skeleton className="h-9 w-full mt-3" />
            </div>
        </div>
    )
}
