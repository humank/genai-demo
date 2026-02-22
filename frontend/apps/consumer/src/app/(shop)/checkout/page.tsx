'use client'

import { CheckoutForm } from '@/components/order/CheckoutForm'
import { OrderConfirmation } from '@/components/order/OrderConfirmation'
import { OrderSummary } from '@/components/order/OrderSummary'
import { useCartStore } from '@/lib/stores/cart-store'
import { Button } from '@repo/ui'
import { ArrowLeft, ShoppingCart } from 'lucide-react'
import Link from 'next/link'
import { useState } from 'react'

export default function CheckoutPage() {
    const [confirmedOrderId, setConfirmedOrderId] = useState<string | null>(null)
    const items = useCartStore((s) => s.items)

    if (confirmedOrderId) {
        return <OrderConfirmation orderId={confirmedOrderId} />
    }

    if (items.length === 0) {
        return (
            <div className="container-shop py-16 text-center">
                <ShoppingCart className="h-16 w-16 text-stone-300 mx-auto mb-4" />
                <h2 className="text-xl font-semibold text-foreground mb-2">購物車是空的</h2>
                <p className="text-muted-foreground mb-6">請先加入商品再結帳</p>
                <Link href="/products">
                    <Button className="cursor-pointer">前往購物</Button>
                </Link>
            </div>
        )
    }

    return (
        <div className="container-shop py-8">
            <Link
                href="/cart"
                className="inline-flex items-center text-sm text-muted-foreground hover:text-primary transition-colors mb-6 cursor-pointer"
            >
                <ArrowLeft className="h-4 w-4 mr-1" />
                返回購物車
            </Link>

            <h1 className="text-2xl font-bold text-foreground mb-6">結帳</h1>

            <div className="grid grid-cols-1 lg:grid-cols-3 gap-8">
                <div className="lg:col-span-2">
                    <CheckoutForm onSuccess={setConfirmedOrderId} />
                </div>
                <div>
                    <OrderSummary />
                </div>
            </div>
        </div>
    )
}
