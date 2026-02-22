'use client'

import { useCancelPayment } from '@repo/api-client/hooks'
import type { Payment } from '@repo/api-client/types'
import {
    Button, Dialog, DialogContent, DialogDescription,
    DialogFooter, DialogHeader, DialogTitle, toast
} from '@repo/ui'

interface CancelPaymentDialogProps {
    payment: Payment | null
    open: boolean
    onOpenChange: (open: boolean) => void
}

export function CancelPaymentDialog({ payment, open, onOpenChange }: CancelPaymentDialogProps) {
    const cancelMutation = useCancelPayment()

    const handleCancel = async () => {
        if (!payment) return
        try {
            await cancelMutation.mutateAsync(payment.id)
            toast({ title: '支付已取消' })
            onOpenChange(false)
        } catch (error: any) {
            toast({ title: `取消支付失敗：${error?.message || '請稍後再試'}`, variant: 'destructive' })
        }
    }

    if (!payment) return null

    return (
        <Dialog open={open} onOpenChange={onOpenChange}>
            <DialogContent>
                <DialogHeader>
                    <DialogTitle>確認取消支付</DialogTitle>
                    <DialogDescription>
                        確定要取消支付 <span className="font-mono font-semibold">{payment.id}</span> 嗎？
                        此操作無法撤銷。
                    </DialogDescription>
                </DialogHeader>
                <DialogFooter>
                    <Button variant="outline" onClick={() => onOpenChange(false)} disabled={cancelMutation.isPending}>
                        返回
                    </Button>
                    <Button variant="destructive" onClick={handleCancel} disabled={cancelMutation.isPending}>
                        {cancelMutation.isPending ? '處理中...' : '確認取消'}
                    </Button>
                </DialogFooter>
            </DialogContent>
        </Dialog>
    )
}
