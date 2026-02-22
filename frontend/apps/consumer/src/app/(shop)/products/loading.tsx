import { Skeleton } from '@repo/ui'

export default function ProductsLoading() {
    return (
        <div className="container-shop py-8">
            <Skeleton className="h-8 w-32 mb-6" />
            <div className="space-y-4 mb-8">
                <Skeleton className="h-10 w-full max-w-md rounded-lg" />
                <div className="flex gap-2">
                    {Array.from({ length: 5 }).map((_, i) => (
                        <Skeleton key={i} className="h-9 w-20 rounded-lg" />
                    ))}
                </div>
            </div>
            <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 xl:grid-cols-4 gap-6">
                {Array.from({ length: 8 }).map((_, i) => (
                    <div key={i} className="rounded-xl border border-stone-100 overflow-hidden">
                        <Skeleton className="aspect-square w-full" />
                        <div className="p-4 space-y-2">
                            <Skeleton className="h-3 w-16" />
                            <Skeleton className="h-4 w-full" />
                            <Skeleton className="h-5 w-20" />
                            <Skeleton className="h-9 w-full" />
                        </div>
                    </div>
                ))}
            </div>
        </div>
    )
}
