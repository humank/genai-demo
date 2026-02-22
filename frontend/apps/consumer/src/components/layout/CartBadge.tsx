'use client'

import { useCartStore } from '@/lib/stores/cart-store'
import { ShoppingCart } from 'lucide-react'
import Link from 'next/link'

export function CartBadge() {
    const itemCount = useCartStore((s) => s.itemCount)

    return (
        <Link
            href="/cart"
            className="relative p-2 text-foreground hover:text-primary transition-colors cursor-pointer"
            aria-label={`購物車，${itemCount} 件商品`}
        >
            <ShoppingCart className="h-5 w-5" />
            {itemCount > 0 && (
                <span className="absolute -top-0.5 -right-0.5 flex h-5 w-5 items-center justify-center rounded-full bg-primary text-[11px] font-medium text-primary-foreground">
                    {itemCount > 99 ? '99+' : itemCount}
                </span>
            )}
        </Link>
    )
}
