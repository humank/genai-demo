import { Skeleton } from '@repo/ui'

export function HomeSkeleton() {
    return (
        <div className="container-shop py-12">
            <Skeleton className="h-8 w-40 mb-6" />
            <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 xl:grid-cols-4 gap-6">
                {Array.from({ length: 8 }).map((_, i) => (
                    <div key={i} className="rounded-xl border border-stone-100 overflow-hidden">
                        <Skeleton className="aspect-square w-full" />
                        <div className="p-4 space-y-2">
                            <Skeleton className="h-3 w-16" />
                            <Skeleton className="h-4 w-full" />
                            <Skeleton className="h-5 w-20" />
                        </div>
                    </div>
                ))}
            </div>
        </div>
    )
}

export default function Loading() {
    return (
        <>
            {/* Hero skeleton */}
            <Skeleton className="w-full h-64 sm:h-80 lg:h-96" />
            <HomeSkeleton />
        </>
    )
}
