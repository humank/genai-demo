import type { OrderStatus } from '@repo/api-client';
import { Badge } from '@repo/ui';

const statusConfig: Record<string, { label: string; className: string }> = {
    CREATED: { label: '已建立', className: 'bg-stone-100 text-stone-700 hover:bg-stone-100' },
    PENDING: { label: '待處理', className: 'bg-yellow-100 text-yellow-700 hover:bg-yellow-100' },
    CONFIRMED: { label: '已確認', className: 'bg-blue-100 text-blue-700 hover:bg-blue-100' },
    SHIPPED: { label: '已出貨', className: 'bg-purple-100 text-purple-700 hover:bg-purple-100' },
    DELIVERED: { label: '已送達', className: 'bg-green-100 text-green-700 hover:bg-green-100' },
    CANCELLED: { label: '已取消', className: 'bg-red-100 text-red-700 hover:bg-red-100' },
}

export function OrderStatusBadge({ status }: { status: OrderStatus | string }) {
    const config = statusConfig[status] ?? { label: status, className: 'bg-stone-100 text-stone-700' }
    return (
        <Badge variant="secondary" className={config.className}>
            {config.label}
        </Badge>
    )
}
