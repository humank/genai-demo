import { Skeleton } from '@repo/ui'

export default function OrdersLoading() {
    return (
        <div className="container-shop py-8">
            <Skeleton className="h-8 w-32 mb-6" />
            <div className="space-y-3">
                {Array.from({ length: 4 }).map((_, i) => (
                    <Skeleton key={i} className="h-28 rounded-xl" />
                ))}
            </div>
        </div>
    )
}
