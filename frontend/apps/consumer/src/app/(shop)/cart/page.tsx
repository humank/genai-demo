'use client'

import { CartEmpty } from '@/components/cart/CartEmpty'
import { CartItemList } from '@/components/cart/CartItemList'
import { CartSummary } from '@/components/cart/CartSummary'
import { useAuthStore } from '@/lib/stores/auth-store'
import { useCartStore } from '@/lib/stores/cart-store'
import { useCart } from '@repo/api-client'
import { Skeleton } from '@repo/ui'
import { useEffect } from 'react'

export default function CartPage() {
    const customerId = useAuthStore((s) => s.customerId) || 'demo-customer'
    const setItems = useCartStore((s) => s.setItems)
    const storeItems = useCartStore((s) => s.items)

    const { data, isLoading } = useCart(customerId)

    // Sync API data → Zustand store
    useEffect(() => {
        if (data) {
            const items = Array.isArray(data) ? data : []
            setItems(items)
        }
    }, [data, setItems])

    const items = Array.isArray(data) ? data : storeItems

    if (isLoading) {
        return (
            <div className="container-shop py-8">
                <Skeleton className="h-8 w-32 mb-6" />
                <div className="grid grid-cols-1 lg:grid-cols-3 gap-8">
                    <div className="lg:col-span-2 space-y-3">
                        {Array.from({ length: 3 }).map((_, i) => (
                            <Skeleton key={i} className="h-24 rounded-xl" />
                        ))}
                    </div>
                    <Skeleton className="h-48 rounded-xl" />
                </div>
            </div>
        )
    }

    if (items.length === 0) {
        return (
            <div className="container-shop py-8">
                <h1 className="text-2xl font-bold text-foreground mb-6">購物車</h1>
                <CartEmpty />
            </div>
        )
    }

    return (
        <div className="container-shop py-8">
            <h1 className="text-2xl font-bold text-foreground mb-6">購物車</h1>
            <div className="grid grid-cols-1 lg:grid-cols-3 gap-8">
                <div className="lg:col-span-2">
                    <CartItemList items={items} />
                </div>
                <div>
                    <CartSummary items={items} />
                </div>
            </div>
        </div>
    )
}
