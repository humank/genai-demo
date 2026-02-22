import { Skeleton } from '@repo/ui'

export default function CartLoading() {
    return (
        <div className="container-shop py-8">
            <Skeleton className="h-8 w-32 mb-6" />
            <div className="grid grid-cols-1 lg:grid-cols-3 gap-8">
                <div className="lg:col-span-2 space-y-3">
                    {Array.from({ length: 3 }).map((_, i) => (
                        <Skeleton key={i} className="h-24 rounded-xl" />
                    ))}
                </div>
                <Skeleton className="h-48 rounded-xl" />
            </div>
        </div>
    )
}
