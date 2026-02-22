'use client'

import type { Payment } from '@repo/api-client/types'
import { PaymentStatus } from '@repo/api-client/types'
import { Button, Card, CardContent } from '@repo/ui'
import { format } from 'date-fns'
import { Ban, RotateCcw } from 'lucide-react'
import { PaymentStatusBadge } from './PaymentStatusBadge'

interface PaymentDetailProps {
    payment: Payment
    onRefund: (payment: Payment) => void
    onCancel: (payment: Payment) => void
}

export function PaymentDetail({ payment, onRefund, onCancel }: PaymentDetailProps) {
    const canRefund = payment.status === PaymentStatus.COMPLETED
    const canCancel = payment.status === PaymentStatus.PENDING || payment.status === PaymentStatus.PROCESSING

    return (
        <Card className="border-blue-200 bg-blue-50/30 dark:bg-blue-950/10">
            <CardContent className="p-4 grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-4">
                <div>
                    <p className="text-xs text-muted-foreground mb-1">支付 ID</p>
                    <p className="text-sm font-mono">{payment.id}</p>
                </div>
                <div>
                    <p className="text-xs text-muted-foreground mb-1">關聯訂單</p>
                    <p className="text-sm font-mono">{payment.orderId}</p>
                </div>
                <div>
                    <p className="text-xs text-muted-foreground mb-1">支付方式</p>
                    <p className="text-sm">{payment.method}</p>
                </div>
                <div>
                    <p className="text-xs text-muted-foreground mb-1">狀態</p>
                    <PaymentStatusBadge status={payment.status} />
                </div>
                <div>
                    <p className="text-xs text-muted-foreground mb-1">金額</p>
                    <p className="text-sm font-semibold">
                        {payment.amount.currency} {payment.amount.amount.toLocaleString()}
                    </p>
                </div>
                <div>
                    <p className="text-xs text-muted-foreground mb-1">建立時間</p>
                    <p className="text-sm">{format(new Date(payment.createdAt), 'yyyy-MM-dd HH:mm:ss')}</p>
                </div>
                {payment.processedAt && (
                    <div>
                        <p className="text-xs text-muted-foreground mb-1">處理時間</p>
                        <p className="text-sm">{format(new Date(payment.processedAt), 'yyyy-MM-dd HH:mm:ss')}</p>
                    </div>
                )}
                <div className="flex items-end gap-2">
                    {canRefund && (
                        <Button variant="outline" size="sm" onClick={() => onRefund(payment)}>
                            <RotateCcw className="h-3 w-3 mr-1" /> 退款
                        </Button>
                    )}
                    {canCancel && (
                        <Button variant="outline" size="sm" className="text-red-600 hover:text-red-700" onClick={() => onCancel(payment)}>
                            <Ban className="h-3 w-3 mr-1" /> 取消
                        </Button>
                    )}
                </div>
            </CardContent>
        </Card>
    )
}
