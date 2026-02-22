import { Skeleton } from '@repo/ui'

export default function CheckoutLoading() {
    return (
        <div className="container-shop py-8">
            <Skeleton className="h-4 w-24 mb-6" />
            <Skeleton className="h-8 w-24 mb-6" />
            <div className="grid grid-cols-1 lg:grid-cols-3 gap-8">
                <div className="lg:col-span-2 space-y-4">
                    <Skeleton className="h-10 w-full rounded-lg" />
                    <Skeleton className="h-10 w-full rounded-lg" />
                    <Skeleton className="h-10 w-full rounded-lg" />
                    <Skeleton className="h-24 w-full rounded-lg" />
                    <Skeleton className="h-10 w-40 rounded-lg" />
                </div>
                <div className="space-y-3">
                    <Skeleton className="h-6 w-24" />
                    {Array.from({ length: 3 }).map((_, i) => (
                        <Skeleton key={i} className="h-5 w-full" />
                    ))}
                    <Skeleton className="h-px w-full" />
                    <Skeleton className="h-6 w-full" />
                </div>
            </div>
        </div>
    )
}
