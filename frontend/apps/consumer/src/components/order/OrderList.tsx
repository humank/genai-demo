import type { Order } from '@repo/api-client'
import { OrderCard } from './OrderCard'

export function OrderList({ orders }: { orders: Order[] }) {
    return (
        <div className="space-y-3">
            {orders.map((order) => (
                <OrderCard key={order.id} order={order} />
            ))}
        </div>
    )
}
