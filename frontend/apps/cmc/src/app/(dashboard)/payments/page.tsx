'use client'

import { CancelPaymentDialog } from '@/components/payment/CancelPaymentDialog'
import { PaymentTable } from '@/components/payment/PaymentTable'
import { RefundDialog } from '@/components/payment/RefundDialog'
import { usePayments } from '@repo/api-client/hooks'
import type { Payment } from '@repo/api-client/types'
import { PaymentStatus } from '@repo/api-client/types'
import {
    Button, Input, Select, SelectContent, SelectItem,
    SelectTrigger, SelectValue
} from '@repo/ui'
import { CreditCard, Filter, RefreshCw, Search } from 'lucide-react'
import { useState } from 'react'

export default function PaymentsPage() {
    const [currentPage, setCurrentPage] = useState(0)
    const [searchTerm, setSearchTerm] = useState('')
    const [statusFilter, setStatusFilter] = useState<string>('ALL')
    const pageSize = 20

    const { data: paymentsResponse, isLoading, error, refetch } = usePayments({
        page: currentPage,
        size: pageSize,
    })

    const [refundTarget, setRefundTarget] = useState<Payment | null>(null)
    const [cancelTarget, setCancelTarget] = useState<Payment | null>(null)

    const payments = paymentsResponse?.content || []

    const filteredPayments = payments.filter((p) => {
        const matchesSearch =
            p.id.toLowerCase().includes(searchTerm.toLowerCase()) ||
            p.orderId.toLowerCase().includes(searchTerm.toLowerCase())
        const matchesStatus = statusFilter === 'ALL' || p.status === statusFilter
        return matchesSearch && matchesStatus
    })

    if (error) {
        return (
            <div className="flex flex-col items-center justify-center p-16 text-center">
                <p className="text-red-500 mb-4">載入支付記錄時發生錯誤</p>
                <Button onClick={() => refetch()} variant="outline">
                    <RefreshCw className="h-4 w-4 mr-2" /> 重試
                </Button>
            </div>
        )
    }

    return (
        <div className="space-y-6">
            <div className="flex flex-col sm:flex-row justify-between items-start sm:items-center gap-4">
                <div>
                    <div className="flex items-center gap-2 mb-1">
                        <CreditCard className="h-6 w-6 text-blue-600" />
                        <h1 className="text-3xl font-bold text-foreground">支付管理</h1>
                    </div>
                    <p className="text-muted-foreground">查看和管理所有支付記錄、退款和取消操作</p>
                </div>
                <Button variant="outline" size="sm" onClick={() => refetch()}>
                    <RefreshCw className="h-4 w-4 mr-2" /> 刷新
                </Button>
            </div>

            <div className="modern-card p-4">
                <div className="flex flex-col sm:flex-row gap-3">
                    <div className="relative flex-1">
                        <Search className="absolute left-3 top-1/2 -translate-y-1/2 h-4 w-4 text-muted-foreground" />
                        <Input
                            placeholder="搜尋支付 ID 或訂單 ID..."
                            value={searchTerm}
                            onChange={(e) => setSearchTerm(e.target.value)}
                            className="pl-10"
                        />
                    </div>
                    <Select value={statusFilter} onValueChange={setStatusFilter}>
                        <SelectTrigger className="w-full sm:w-48">
                            <Filter className="h-4 w-4 mr-2" />
                            <SelectValue placeholder="篩選狀態" />
                        </SelectTrigger>
                        <SelectContent>
                            <SelectItem value="ALL">所有狀態</SelectItem>
                            <SelectItem value={PaymentStatus.PENDING}>待處理</SelectItem>
                            <SelectItem value={PaymentStatus.PROCESSING}>處理中</SelectItem>
                            <SelectItem value={PaymentStatus.COMPLETED}>已完成</SelectItem>
                            <SelectItem value={PaymentStatus.FAILED}>失敗</SelectItem>
                            <SelectItem value={PaymentStatus.CANCELLED}>已取消</SelectItem>
                        </SelectContent>
                    </Select>
                </div>
            </div>

            <div className="modern-card p-0 overflow-hidden">
                {isLoading ? (
                    <div className="p-8 space-y-3 animate-pulse">
                        {Array.from({ length: 5 }).map((_, i) => (
                            <div key={i} className="h-12 bg-muted rounded" />
                        ))}
                    </div>
                ) : (
                    <PaymentTable
                        payments={filteredPayments}
                        onRefund={setRefundTarget}
                        onCancel={setCancelTarget}
                    />
                )}
            </div>

            {paymentsResponse && paymentsResponse.totalPages > 1 && (
                <div className="flex justify-center items-center gap-4">
                    <Button
                        variant="outline" size="sm"
                        onClick={() => setCurrentPage(Math.max(0, currentPage - 1))}
                        disabled={currentPage === 0}
                    >
                        上一頁
                    </Button>
                    <span className="text-sm text-muted-foreground">
                        第 {currentPage + 1} 頁，共 {paymentsResponse.totalPages} 頁
                    </span>
                    <Button
                        variant="outline" size="sm"
                        onClick={() => setCurrentPage(Math.min(paymentsResponse.totalPages - 1, currentPage + 1))}
                        disabled={currentPage >= paymentsResponse.totalPages - 1}
                    >
                        下一頁
                    </Button>
                </div>
            )}

            <RefundDialog
                payment={refundTarget}
                open={!!refundTarget}
                onOpenChange={(open) => { if (!open) setRefundTarget(null) }}
            />
            <CancelPaymentDialog
                payment={cancelTarget}
                open={!!cancelTarget}
                onOpenChange={(open) => { if (!open) setCancelTarget(null) }}
            />
        </div>
    )
}
