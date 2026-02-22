'use client'

import { PaymentStatus } from '@repo/api-client/types';
import { Badge, cn } from '@repo/ui';

const statusConfig: Record<PaymentStatus, { label: string; className: string }> = {
    [PaymentStatus.PENDING]: { label: '待處理', className: 'bg-yellow-100 text-yellow-800 border-yellow-200' },
    [PaymentStatus.PROCESSING]: { label: '處理中', className: 'bg-blue-100 text-blue-800 border-blue-200' },
    [PaymentStatus.COMPLETED]: { label: '已完成', className: 'bg-green-100 text-green-800 border-green-200' },
    [PaymentStatus.FAILED]: { label: '失敗', className: 'bg-red-100 text-red-800 border-red-200' },
    [PaymentStatus.CANCELLED]: { label: '已取消', className: 'bg-gray-100 text-gray-800 border-gray-200' },
}

export function PaymentStatusBadge({ status }: { status: PaymentStatus }) {
    const config = statusConfig[status] || { label: status, className: 'bg-gray-100 text-gray-800' }
    return (
        <Badge variant="outline" className={cn('font-medium', config.className)}>
            {config.label}
        </Badge>
    )
}
