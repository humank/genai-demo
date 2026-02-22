import type { Order } from '@repo/api-client'
import { Button, Card, CardContent } from '@repo/ui'
import { format } from 'date-fns'
import { ChevronRight } from 'lucide-react'
import Link from 'next/link'
import { OrderStatusBadge } from './OrderStatusBadge'

function formatPrice(amount: number) {
    return `NT$ ${amount.toLocaleString()}`
}

export function OrderCard({ order }: { order: Order }) {
    const itemCount = order.items?.length ?? 0
    const createdAt = order.createdAt
        ? format(new Date(order.createdAt), 'yyyy/MM/dd HH:mm')
        : '-'

    return (
        <Card className="rounded-xl shadow-sm border-stone-100 hover:shadow-md transition-shadow duration-200">
            <CardContent className="p-5">
                <div className="flex items-start justify-between gap-4">
                    <div className="space-y-2 min-w-0 flex-1">
                        <div className="flex items-center gap-3 flex-wrap">
                            <span className="text-sm font-mono font-medium text-foreground">
                                {order.id}
                            </span>
                            <OrderStatusBadge status={order.status} />
                        </div>
                        <div className="flex items-center gap-4 text-sm text-muted-foreground">
                            <span>{itemCount} 件商品</span>
                            <span>{createdAt}</span>
                        </div>
                        <p className="text-base font-bold text-primary">
                            {formatPrice(order.totalAmount?.amount ?? 0)}
                        </p>
                    </div>
                    <Link href={`/orders/${order.id}`}>
                        <Button variant="ghost" size="sm" className="cursor-pointer flex-shrink-0">
                            查看詳情
                            <ChevronRight className="h-4 w-4 ml-1" />
                        </Button>
                    </Link>
                </div>
            </CardContent>
        </Card>
    )
}
