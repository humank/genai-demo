'use client'

import { OrderList } from '@/components/order/OrderList'
import type { Order } from '@repo/api-client'
import { useOrders } from '@repo/api-client'
import { EmptyState, Skeleton } from '@repo/ui'
import { Package } from 'lucide-react'

export default function OrdersPage() {
    const { data, isLoading } = useOrders({ page: 0, size: 20 })
    const orders: Order[] = (data as { content?: Order[] })?.content ?? (data as Order[] | undefined) ?? []

    if (isLoading) {
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

    return (
        <div className="container-shop py-8">
            <h1 className="text-2xl font-bold text-foreground mb-6">我的訂單</h1>
            {orders.length === 0 ? (
                <EmptyState
                    icon={Package}
                    title="尚無訂單"
                    description="您還沒有任何訂單紀錄"
                />
            ) : (
                <OrderList orders={orders} />
            )}
        </div>
    )
}
