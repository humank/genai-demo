import { Card, CardContent } from '@repo/ui'
import Link from 'next/link'

interface FeaturedProduct {
    id: string
    name: string
    price: { amount: number; currency: string }
    category: string
}

async function getFeaturedProducts(): Promise<FeaturedProduct[]> {
    try {
        const baseURL = process.env.NEXT_PUBLIC_API_URL || 'http://localhost:8080/api'
        const res = await fetch(`${baseURL}/consumer/products?size=8`, {
            next: { revalidate: 600 },
        })
        if (!res.ok) return []
        const json = await res.json()
        const data = json.data ?? json
        return data.content ?? data ?? []
    } catch {
        return []
    }
}

function formatPrice(price: { amount: number; currency: string }) {
    return new Intl.NumberFormat('zh-TW', {
        style: 'currency',
        currency: price.currency || 'TWD',
        minimumFractionDigits: 0,
    }).format(price.amount)
}

export async function FeaturedProducts() {
    const products = await getFeaturedProducts()

    if (products.length === 0) {
        return null
    }

    return (
        <section className="container-shop py-12">
            <div className="flex items-center justify-between mb-6">
                <h2 className="text-2xl font-bold text-foreground">精選商品</h2>
                <Link
                    href="/products"
                    className="text-sm font-medium text-primary hover:underline cursor-pointer"
                >
                    查看全部
                </Link>
            </div>
            <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 xl:grid-cols-4 gap-6">
                {products.map((product) => (
                    <Link key={product.id} href={`/products/${product.id}`} className="group cursor-pointer">
                        <Card className="rounded-xl shadow-sm border-stone-100 overflow-hidden hover:shadow-md transition-shadow duration-200">
                            <div className="aspect-square bg-stone-100 flex items-center justify-center">
                                <span className="text-4xl text-stone-300" aria-hidden="true">📦</span>
                            </div>
                            <CardContent className="p-4">
                                <p className="text-xs text-muted-foreground mb-1">{product.category}</p>
                                <h3 className="text-sm font-semibold text-foreground group-hover:text-primary transition-colors line-clamp-2">
                                    {product.name}
                                </h3>
                                <p className="mt-2 text-base font-bold text-primary">
                                    {formatPrice(product.price)}
                                </p>
                            </CardContent>
                        </Card>
                    </Link>
                ))}
            </div>
        </section>
    )
}
