'use client'

import { useAuthStore } from '@/lib/stores/auth-store'
import { useCartStore } from '@/lib/stores/cart-store'
import type { Product } from '@repo/api-client'
import { useAddToCart } from '@repo/api-client'
import { Button, Card, CardContent, toast } from '@repo/ui'
import { ShoppingCart } from 'lucide-react'
import Link from 'next/link'

function formatPrice(amount: number) {
    return `NT$ ${amount.toLocaleString()}`
}

export function ProductCard({ product }: { product: Product }) {
    const customerId = useAuthStore((s) => s.customerId) || 'demo-customer'
    const addItemToStore = useCartStore((s) => s.addItem)
    const addToCart = useAddToCart()

    const handleAddToCart = (e: React.MouseEvent) => {
        e.preventDefault()
        e.stopPropagation()
        addToCart.mutate(
            { customerId, item: { productId: product.id, quantity: 1 } },
            {
                onSuccess: () => {
                    addItemToStore({
                        productId: product.id,
                        productName: product.name,
                        quantity: 1,
                        unitPrice: product.price,
                        totalPrice: product.price,
                    })
                    toast({ title: '已加入購物車', description: product.name })
                },
                onError: () => {
                    toast({ title: '加入失敗', description: '請稍後再試', variant: 'destructive' })
                },
            }
        )
    }

    return (
        <Link href={`/products/${product.id}`} className="group cursor-pointer">
            <Card className="rounded-xl shadow-sm border-stone-100 overflow-hidden hover:shadow-md transition-shadow duration-200 h-full flex flex-col">
                <div className="aspect-square bg-stone-100 flex items-center justify-center">
                    <ShoppingCart className="h-12 w-12 text-stone-300" />
                </div>
                <CardContent className="p-4 flex flex-col flex-1">
                    <p className="text-xs text-muted-foreground mb-1">{product.category}</p>
                    <h3 className="text-sm font-semibold text-foreground group-hover:text-primary transition-colors line-clamp-2 flex-1">
                        {product.name}
                    </h3>
                    <p className="mt-2 text-base font-bold text-primary">
                        {formatPrice(product.price.amount)}
                    </p>
                    <Button
                        size="sm"
                        className="mt-3 w-full cursor-pointer"
                        onClick={handleAddToCart}
                        disabled={!product.inStock || addToCart.isPending}
                    >
                        {product.inStock ? '加入購物車' : '缺貨中'}
                    </Button>
                </CardContent>
            </Card>
        </Link>
    )
}
