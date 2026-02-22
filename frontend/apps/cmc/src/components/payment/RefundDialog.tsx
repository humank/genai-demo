'use client'

import { useRefundPayment } from '@repo/api-client/hooks'
import type { Payment } from '@repo/api-client/types'
import {
    Button, Dialog, DialogContent, DialogDescription,
    DialogFooter, DialogHeader, DialogTitle, toast
} from '@repo/ui'

interface RefundDialogProps {
    payment: Payment | null
    open: boolean
    onOpenChange: (open: boolean) => void
}

export function RefundDialog({ payment, open, onOpenChange }: RefundDialogProps) {
    const refundMutation = useRefundPayment()

    const handleRefund = async () => {
        if (!payment) return
        try {
            await refundMutation.mutateAsync(payment.id)
            toast({ title: '退款成功' })
            onOpenChange(false)
        } catch (error: any) {
            toast({ title: `退款失敗：${error?.message || '請稍後再試'}`, variant: 'destructive' })
        }
    }

    if (!payment) return null

    return (
        <Dialog open={open} onOpenChange={onOpenChange}>
            <DialogContent>
                <DialogHeader>
                    <DialogTitle>確認退款</DialogTitle>
                    <DialogDescription>
                        確定要對支付 <span className="font-mono font-semibold">{payment.id}</span> 進行退款嗎？
                        退款金額為 {payment.amount.currency} {payment.amount.amount.toLocaleString()}。
                        此操作無法撤銷。
                    </DialogDescription>
                </DialogHeader>
                <DialogFooter>
                    <Button variant="outline" onClick={() => onOpenChange(false)} disabled={refundMutation.isPending}>
                        取消
                    </Button>
                    <Button onClick={handleRefund} disabled={refundMutation.isPending}>
                        {refundMutation.isPending ? '處理中...' : '確認退款'}
                    </Button>
                </DialogFooter>
            </DialogContent>
        </Dialog>
    )
}
