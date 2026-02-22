'use client'

import type { Payment } from '@repo/api-client/types'
import { PaymentStatus } from '@repo/api-client/types'
import {
    Button,
    Table, TableBody, TableCell, TableHead,
    TableHeader, TableRow
} from '@repo/ui'
import { format } from 'date-fns'
import { ChevronDown, ChevronRight } from 'lucide-react'
import { useState } from 'react'
import { PaymentDetail } from './PaymentDetail'
import { PaymentStatusBadge } from './PaymentStatusBadge'

interface PaymentTableProps {
    payments: Payment[]
    onRefund: (payment: Payment) => void
    onCancel: (payment: Payment) => void
}

export function PaymentTable({ payments, onRefund, onCancel }: PaymentTableProps) {
    const [expandedId, setExpandedId] = useState<string | null>(null)

    const toggleExpand = (id: string) => {
        setExpandedId(prev => prev === id ? null : id)
    }

    if (payments.length === 0) {
        return (
            <div className="text-center py-12 text-muted-foreground">
                目前沒有支付記錄
            </div>
        )
    }

    return (
        <Table>
            <TableHeader>
                <TableRow>
                    <TableHead className="w-8"></TableHead>
                    <TableHead>支付 ID</TableHead>
                    <TableHead>訂單 ID</TableHead>
                    <TableHead>金額</TableHead>
                    <TableHead>支付方式</TableHead>
                    <TableHead>狀態</TableHead>
                    <TableHead>建立時間</TableHead>
                    <TableHead className="text-right">操作</TableHead>
                </TableRow>
            </TableHeader>
            <TableBody>
                {payments.map((payment) => {
                    const isExpanded = expandedId === payment.id
                    const canRefund = payment.status === PaymentStatus.COMPLETED
                    const canCancel = payment.status === PaymentStatus.PENDING || payment.status === PaymentStatus.PROCESSING

                    return (
                        <>
                            <TableRow
                                key={payment.id}
                                className="cursor-pointer hover:bg-muted/50"
                                onClick={() => toggleExpand(payment.id)}
                            >
                                <TableCell>
                                    {isExpanded
                                        ? <ChevronDown className="h-4 w-4 text-muted-foreground" />
                                        : <ChevronRight className="h-4 w-4 text-muted-foreground" />}
                                </TableCell>
                                <TableCell className="font-mono text-xs">{payment.id}</TableCell>
                                <TableCell className="font-mono text-xs">{payment.orderId}</TableCell>
                                <TableCell className="font-semibold">
                                    {payment.amount.currency} {payment.amount.amount.toLocaleString()}
                                </TableCell>
                                <TableCell>{payment.method}</TableCell>
                                <TableCell><PaymentStatusBadge status={payment.status} /></TableCell>
                                <TableCell className="text-sm">
                                    {format(new Date(payment.createdAt), 'yyyy-MM-dd HH:mm')}
                                </TableCell>
                                <TableCell className="text-right">
                                    <div className="flex justify-end gap-1" onClick={(e) => e.stopPropagation()}>
                                        {canRefund && (
                                            <Button variant="ghost" size="sm" onClick={() => onRefund(payment)}>
                                                退款
                                            </Button>
                                        )}
                                        {canCancel && (
                                            <Button variant="ghost" size="sm" className="text-red-600" onClick={() => onCancel(payment)}>
                                                取消
                                            </Button>
                                        )}
                                    </div>
                                </TableCell>
                            </TableRow>
                            {isExpanded && (
                                <TableRow key={`${payment.id}-detail`}>
                                    <TableCell colSpan={8} className="p-2">
                                        <PaymentDetail payment={payment} onRefund={onRefund} onCancel={onCancel} />
                                    </TableCell>
                                </TableRow>
                            )}
                        </>
                    )
                })}
            </TableBody>
        </Table>
    )
}
