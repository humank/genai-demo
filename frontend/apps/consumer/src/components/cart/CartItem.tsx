'use client'

import { useAuthStore } from '@/lib/stores/auth-store'
import { useCartStore } from '@/lib/stores/cart-store'
import type { CartItem as CartItemType } from '@repo/api-client'
import { useRemoveCartItem, useUpdateCartItem } from '@repo/api-client'
import { Button, toast } from '@repo/ui'
import { Minus, Plus, Trash2 } from 'lucide-react'

function formatPrice(amount: number) {
    return `NT$ ${amount.toLocaleString()}`
}

export function CartItem({ item }: { item: CartItemType }) {
    const customerId = useAuthStore((s) => s.customerId) || 'demo-customer'
    const updateStore = useCartStore((s) => s.updateQuantity)
    const removeStore = useCartStore((s) => s.removeItem)

    const updateItem = useUpdateCartItem()
    const removeItem = useRemoveCartItem()

    const handleUpdate = (newQty: number) => {
        if (newQty < 1) return
        updateItem.mutate(
            { customerId, item: { productId: item.productId, quantity: newQty } },
            {
                onSuccess: () => updateStore(item.productId, newQty),
                onError: () => toast({ title: '更新失敗', variant: 'destructive' }),
            }
        )
    }

    const handleRemove = () => {
        removeItem.mutate(
            { customerId, productId: item.productId },
            {
                onSuccess: () => {
                    removeStore(item.productId)
                    toast({ title: '已移除', description: item.productName })
                },
                onError: () => toast({ title: '移除失敗', variant: 'destructive' }),
            }
        )
    }

    return (
        <div className="flex items-center gap-4 p-4 bg-white rounded-xl border border-stone-100">
            {/* Image placeholder */}
            <div className="h-20 w-20 bg-stone-100 rounded-lg flex-shrink-0 flex items-center justify-center">
                <span className="text-stone-300 text-2xl">📦</span>
            </div>

            {/* Info */}
            <div className="flex-1 min-w-0">
                <h3 className="text-sm font-semibold text-foreground truncate">{item.productName}</h3>
                <p className="text-sm text-muted-foreground mt-0.5">
                    {formatPrice(item.unitPrice.amount)} / 件
                </p>
            </div>

            {/* Quantity */}
            <div className="flex items-center border border-stone-200 rounded-lg">
                <button
                    onClick={() => handleUpdate(item.quantity - 1)}
                    disabled={item.quantity <= 1 || updateItem.isPending}
                    className="p-1.5 hover:bg-stone-50 transition-colors cursor-pointer disabled:opacity-50 rounded-l-lg"
                    aria-label="減少數量"
                >
                    <Minus className="h-3.5 w-3.5" />
                </button>
                <span className="px-3 text-sm font-medium min-w-[2.5rem] text-center">
                    {item.quantity}
                </span>
                <button
                    onClick={() => handleUpdate(item.quantity + 1)}
                    disabled={updateItem.isPending}
                    className="p-1.5 hover:bg-stone-50 transition-colors cursor-pointer disabled:opacity-50 rounded-r-lg"
                    aria-label="增加數量"
                >
                    <Plus className="h-3.5 w-3.5" />
                </button>
            </div>

            {/* Subtotal */}
            <p className="text-sm font-bold text-primary w-24 text-right">
                {formatPrice(item.unitPrice.amount * item.quantity)}
            </p>

            {/* Remove */}
            <Button
                variant="ghost"
                size="sm"
                className="text-muted-foreground hover:text-destructive cursor-pointer"
                onClick={handleRemove}
                disabled={removeItem.isPending}
                aria-label={`移除 ${item.productName}`}
            >
                <Trash2 className="h-4 w-4" />
            </Button>
        </div>
    )
}
