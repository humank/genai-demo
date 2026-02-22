'use client'

import { useAuthStore } from '@/lib/stores/auth-store'
import { useCartStore } from '@/lib/stores/cart-store'
import { zodResolver } from '@hookform/resolvers/zod'
import { useAddOrderItem, useClearCart, useCreateOrder, useSubmitOrder } from '@repo/api-client'
import { Button, Input, Label, Textarea, toast } from '@repo/ui'
import { useState } from 'react'
import { useForm } from 'react-hook-form'
import { z } from 'zod'

const checkoutSchema = z.object({
    recipientName: z.string().min(1, '請輸入收件人姓名'),
    phone: z.string().min(1, '請輸入聯絡電話'),
    address: z.string().min(1, '請輸入收件地址'),
    note: z.string().optional(),
})

type CheckoutFormData = z.infer<typeof checkoutSchema>

interface CheckoutFormProps {
    onSuccess: (orderId: string) => void
}

export function CheckoutForm({ onSuccess }: CheckoutFormProps) {
    const [isSubmitting, setIsSubmitting] = useState(false)
    const [error, setError] = useState<string | null>(null)

    const customerId = useAuthStore((s) => s.customerId) || 'demo-customer'
    const items = useCartStore((s) => s.items)
    const clearCart = useCartStore((s) => s.clear)

    const createOrder = useCreateOrder()
    const addOrderItem = useAddOrderItem()
    const submitOrder = useSubmitOrder()
    const clearCartApi = useClearCart()

    const {
        register,
        handleSubmit,
        formState: { errors },
    } = useForm<CheckoutFormData>({
        resolver: zodResolver(checkoutSchema),
    })

    const onSubmit = async (data: CheckoutFormData) => {
        if (items.length === 0) {
            setError('購物車是空的')
            return
        }

        setIsSubmitting(true)
        setError(null)

        try {
            const shippingAddress = `${data.recipientName} ${data.phone}\n${data.address}${data.note ? `\n備註：${data.note}` : ''}`

            // Step 1: Create order
            const order = await createOrder.mutateAsync({
                customerId,
                shippingAddress,
            })

            const orderId = (order as { id?: string })?.id
            if (!orderId) throw new Error('建立訂單失敗')

            // Step 2: Add items
            for (const item of items) {
                await addOrderItem.mutateAsync({
                    orderId,
                    request: { productId: item.productId, quantity: item.quantity },
                })
            }

            // Step 3: Submit order
            await submitOrder.mutateAsync(orderId)

            // Step 4: Clear cart
            clearCartApi.mutate(customerId)
            clearCart()

            toast({ title: '訂單已送出' })
            onSuccess(orderId)
        } catch (err) {
            const message = err instanceof Error ? err.message : '結帳失敗，請稍後再試'
            setError(message)
            toast({ title: '結帳失敗', description: message, variant: 'destructive' })
        } finally {
            setIsSubmitting(false)
        }
    }

    return (
        <form onSubmit={handleSubmit(onSubmit)} className="space-y-5">
            <div className="bg-white rounded-xl border border-stone-100 p-6 space-y-4">
                <h2 className="text-lg font-semibold text-foreground">收件資訊</h2>

                <div className="space-y-1.5">
                    <Label htmlFor="recipientName">收件人姓名</Label>
                    <Input
                        id="recipientName"
                        placeholder="請輸入姓名"
                        {...register('recipientName')}
                        className="rounded-lg"
                    />
                    {errors.recipientName && (
                        <p className="text-sm text-destructive">{errors.recipientName.message}</p>
                    )}
                </div>

                <div className="space-y-1.5">
                    <Label htmlFor="phone">聯絡電話</Label>
                    <Input
                        id="phone"
                        placeholder="請輸入電話號碼"
                        {...register('phone')}
                        className="rounded-lg"
                    />
                    {errors.phone && (
                        <p className="text-sm text-destructive">{errors.phone.message}</p>
                    )}
                </div>

                <div className="space-y-1.5">
                    <Label htmlFor="address">收件地址</Label>
                    <Input
                        id="address"
                        placeholder="請輸入完整地址"
                        {...register('address')}
                        className="rounded-lg"
                    />
                    {errors.address && (
                        <p className="text-sm text-destructive">{errors.address.message}</p>
                    )}
                </div>

                <div className="space-y-1.5">
                    <Label htmlFor="note">備註（選填）</Label>
                    <Textarea
                        id="note"
                        placeholder="特殊需求或備註"
                        {...register('note')}
                        className="rounded-lg"
                        rows={3}
                    />
                </div>
            </div>

            {error && (
                <div className="bg-red-50 border border-red-200 rounded-xl p-4 text-sm text-red-700">
                    {error}
                    <button
                        type="submit"
                        className="ml-2 underline font-medium cursor-pointer"
                    >
                        重試
                    </button>
                </div>
            )}

            <Button
                type="submit"
                size="lg"
                className="w-full cursor-pointer"
                disabled={isSubmitting || items.length === 0}
            >
                {isSubmitting ? '處理中...' : '確認送出訂單'}
            </Button>
        </form>
    )
}
