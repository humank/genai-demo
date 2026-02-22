'use client'

import { useAuthStore } from '@/lib/stores/auth-store'
import { useCartStore } from '@/lib/stores/cart-store'
import type { Product } from '@repo/api-client'
import { useAddToCart, useConsumerProducts } from '@repo/api-client'
import { Button, Skeleton, toast } from '@repo/ui'
import { ArrowLeft, Minus, Package, Plus, ShoppingCart } from 'lucide-react'
import Link from 'next/link'
import { useState } from 'react'

function formatPrice(amount: number) {
    return `NT$ ${amount.toLocaleString()}`
}

export function ProductDetail({ productId }: { productId: string }) {
    const [quantity, setQuantity] = useState(1)
    const customerId = useAuthStore((s) => s.customerId) || 'demo-customer'
    const addItemToStore = useCartStore((s) => s.addItem)
    const addToCart = useAddToCart()

    // Fetch product from the list (consumer API doesn't have a single-product endpoint)
    const { data, isLoading } = useConsumerProducts({ page: 0, size: 100 })
    const products: Product[] = (data as { content?: Product[] })?.content ?? (data as Product[] | undefined) ?? []
    const product = products.find((p) => p.id === productId)

    if (isLoading) {
        return (
            <div className="container-shop py-8">
                <Skeleton className="h-5 w-24 mb-6" />
                <div className="grid grid-cols-1 lg:grid-cols-2 gap-8">
                    <Skeleton className="aspect-square rounded-xl" />
                    <div className="space-y-4">
                        <Skeleton className="h-8 w-3/4" />
                        <Skeleton className="h-5 w-24" />
                        <Skeleton className="h-20 w-full" />
                        <Skeleton className="h-10 w-40" />
                    </div>
                </div>
            </div>
        )
    }

    if (!product) {
        return (
            <div className="container-shop py-16 text-center">
                <Package className="h-16 w-16 text-stone-300 mx-auto mb-4" />
                <h2 className="text-xl font-semibold text-foreground mb-2">找不到商品</h2>
                <p className="text-muted-foreground mb-6">此商品可能已下架或不存在</p>
                <Link href="/products">
                    <Button variant="outline" className="cursor-pointer">
                        <ArrowLeft className="h-4 w-4 mr-2" />
                        返回商品列表
                    </Button>
                </Link>
            </div>
        )
    }

    const handleAddToCart = () => {
        addToCart.mutate(
            { customerId, item: { productId: product.id, quantity } },
            {
                onSuccess: () => {
                    addItemToStore({
                        productId: product.id,
                        productName: product.name,
                        quantity,
                        unitPrice: product.price,
                        totalPrice: { amount: product.price.amount * quantity, currency: product.price.currency },
                    })
                    toast({ title: '已加入購物車', description: `${product.name} x ${quantity}` })
                    setQuantity(1)
                },
                onError: () => {
                    toast({ title: '加入失敗', description: '請稍後再試', variant: 'destructive' })
                },
            }
        )
    }

    return (
        <div className="container-shop py-8">
            <Link
                href="/products"
                className="inline-flex items-center text-sm text-muted-foreground hover:text-primary transition-colors mb-6 cursor-pointer"
            >
                <ArrowLeft className="h-4 w-4 mr-1" />
                返回商品列表
            </Link>

            <div className="grid grid-cols-1 lg:grid-cols-2 gap-8">
                {/* Image placeholder */}
                <div className="aspect-square bg-stone-100 rounded-xl flex items-center justify-center">
                    <ShoppingCart className="h-24 w-24 text-stone-300" />
                </div>

                {/* Info */}
                <div className="space-y-6">
                    <div>
                        <p className="text-sm text-muted-foreground mb-1">{product.category}</p>
                        <h1 className="text-2xl font-bold text-foreground">{product.name}</h1>
                    </div>

                    <p className="text-3xl font-bold text-primary">
                        {formatPrice(product.price.amount)}
                    </p>

                    {/* Stock status */}
                    <div className="flex items-center gap-2">
                        <span
                            className={`inline-block h-2 w-2 rounded-full ${product.inStock ? 'bg-green-500' : 'bg-red-500'}`}
                        />
                        <span className={`text-sm font-medium ${product.inStock ? 'text-green-600' : 'text-red-600'}`}>
                            {product.inStock ? '有貨' : '缺貨'}
                        </span>
                        {product.inStock && product.stockQuantity > 0 && (
                            <span className="text-sm text-muted-foreground">
                                （剩餘 {product.stockQuantity} 件）
                            </span>
                        )}
                    </div>

                    {product.description && (
                        <p className="text-muted-foreground leading-relaxed">{product.description}</p>
                    )}

                    {/* Quantity + Add to cart */}
                    <div className="flex items-center gap-4">
                        <div className="flex items-center border border-stone-200 rounded-lg">
                            <button
                                onClick={() => setQuantity((q) => Math.max(1, q - 1))}
                                className="p-2 hover:bg-stone-50 transition-colors cursor-pointer rounded-l-lg"
                                aria-label="減少數量"
                            >
                                <Minus className="h-4 w-4" />
                            </button>
                            <span className="px-4 py-2 text-sm font-medium min-w-[3rem] text-center">
                                {quantity}
                            </span>
                            <button
                                onClick={() => setQuantity((q) => Math.min(product.stockQuantity || 99, q + 1))}
                                className="p-2 hover:bg-stone-50 transition-colors cursor-pointer rounded-r-lg"
                                aria-label="增加數量"
                            >
                                <Plus className="h-4 w-4" />
                            </button>
                        </div>

                        <Button
                            size="lg"
                            className="cursor-pointer"
                            onClick={handleAddToCart}
                            disabled={!product.inStock || addToCart.isPending}
                        >
                            <ShoppingCart className="h-4 w-4 mr-2" />
                            {addToCart.isPending ? '處理中...' : '加入購物車'}
                        </Button>
                    </div>
                </div>
            </div>
        </div>
    )
}
