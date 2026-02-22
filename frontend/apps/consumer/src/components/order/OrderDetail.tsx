'use client'

import type { Order } from '@repo/api-client'
import { useCancelOrder, useOrder } from '@repo/api-client'
import {
    Button, Skeleton, Table, TableBody, TableCell, TableHead,
    TableHeader, TableRow, toast,
} from '@repo/ui'
import { format } from 'date-fns'
import { ArrowLeft } from 'lucide-react'
import Link from 'next/link'
import { OrderStatusBadge } from './OrderStatusBadge'

function formatPrice(amount: number) {
    return `NT$ ${amount.toLocaleString()}`
}

function formatDate(dateStr: string | undefined) {
    if (!dateStr) return '-'
    return format(new Date(dateStr), 'yyyy/MM/dd HH:mm')
}

export function OrderDetail({ orderId }: { orderId: string }) {
    const { data, isLoading } = useOrder(orderId)
    const cancelOrder = useCancelOrder()

    const order = data as Order | undefined

    if (isLoading) {
        return (
            <div className="container-shop py-8 space-y-6">
                <Skeleton className="h-5 w-24" />
                <Skeleton className="h-8 w-64" />
                <Skeleton className="h-48 rounded-xl" />
                <Skeleton className="h-32 rounded-xl" />
            </div>
        )
    }

    if (!order) {
        return (
            <div className="container-shop py-16 text-center">
                <h2 className="text-xl font-semibold text-foreground mb-2">找不到訂單</h2>
                <p className="text-muted-foreground mb-6">此訂單可能不存在</p>
                <Link href="/orders">
                    <Button variant="outline" className="cursor-pointer">
                        <ArrowLeft className="h-4 w-4 mr-2" />
                        返回訂單列表
                    </Button>
                </Link>
            </div>
        )
    }

    const canCancel = order.status === 'CREATED' || order.status === 'PENDING'

    const handleCancel = () => {
        cancelOrder.mutate(orderId, {
            onSuccess: () => {
                toast({ title: '訂單已取消' })
            },
            onError: () => {
                toast({ title: '取消失敗', description: '請稍後再試', variant: 'destructive' })
            },
        })
    }

    return (
        <div className="container-shop py-8">
            <Link
                href="/orders"
                className="inline-flex items-center text-sm text-muted-foreground hover:text-primary transition-colors mb-6 cursor-pointer"
            >
                <ArrowLeft className="h-4 w-4 mr-1" />
                返回訂單列表
            </Link>

            {/* Header */}
            <div className="flex items-start justify-between gap-4 mb-6 flex-wrap">
                <div>
                    <h1 className="text-2xl font-bold text-foreground mb-2">訂單詳情</h1>
                    <p className="text-sm font-mono text-muted-foreground">{order.id}</p>
                </div>
                <div className="flex items-center gap-3">
                    <OrderStatusBadge status={order.status} />
                    {canCancel && (
                        <Button
                            variant="outline"
                            size="sm"
                            className="text-destructive border-destructive/30 hover:bg-destructive/10 cursor-pointer"
                            onClick={handleCancel}
                            disabled={cancelOrder.isPending}
                        >
                            {cancelOrder.isPending ? '取消中...' : '取消訂單'}
                        </Button>
                    )}
                </div>
            </div>

            <div className="grid grid-cols-1 lg:grid-cols-3 gap-6">
                {/* Items table */}
                <div className="lg:col-span-2 bg-white rounded-xl border border-stone-100 overflow-hidden">
                    <Table>
                        <TableHeader>
                            <TableRow>
                                <TableHead>商品</TableHead>
                                <TableHead className="text-center">數量</TableHead>
                                <TableHead className="text-right">單價</TableHead>
                                <TableHead className="text-right">小計</TableHead>
                            </TableRow>
                        </TableHeader>
                        <TableBody>
                            {order.items?.map((item) => (
                                <TableRow key={item.productId}>
                                    <TableCell className="font-medium">{item.productName}</TableCell>
                                    <TableCell className="text-center">{item.quantity}</TableCell>
                                    <TableCell className="text-right">
                                        {formatPrice(item.unitPrice.amount)}
                                    </TableCell>
                                    <TableCell className="text-right font-medium">
                                        {formatPrice(item.totalPrice.amount)}
                                    </TableCell>
                                </TableRow>
                            ))}
                        </TableBody>
                    </Table>
                </div>

                {/* Summary sidebar */}
                <div className="space-y-4">
                    <div className="bg-white rounded-xl border border-stone-100 p-5 space-y-3">
                        <h3 className="font-semibold text-foreground">金額</h3>
                        <div className="space-y-2 text-sm">
                            <div className="flex justify-between">
                                <span className="text-muted-foreground">訂單金額</span>
                                <span>{formatPrice(order.totalAmount?.amount ?? 0)}</span>
                            </div>
                            <div className="flex justify-between font-bold text-base pt-2 border-t border-stone-100">
                                <span>實付金額</span>
                                <span className="text-primary">
                                    {formatPrice(order.effectiveAmount?.amount ?? order.totalAmount?.amount ?? 0)}
                                </span>
                            </div>
                        </div>
                    </div>

                    <div className="bg-white rounded-xl border border-stone-100 p-5 space-y-3">
                        <h3 className="font-semibold text-foreground">收件資訊</h3>
                        <p className="text-sm text-muted-foreground whitespace-pre-line">
                            {order.shippingAddress || '-'}
                        </p>
                    </div>

                    <div className="bg-white rounded-xl border border-stone-100 p-5 space-y-3">
                        <h3 className="font-semibold text-foreground">時間</h3>
                        <div className="space-y-1.5 text-sm">
                            <div className="flex justify-between">
                                <span className="text-muted-foreground">建立時間</span>
                                <span>{formatDate(order.createdAt)}</span>
                            </div>
                            <div className="flex justify-between">
                                <span className="text-muted-foreground">更新時間</span>
                                <span>{formatDate(order.updatedAt)}</span>
                            </div>
                        </div>
                    </div>
                </div>
            </div>
        </div>
    )
}
