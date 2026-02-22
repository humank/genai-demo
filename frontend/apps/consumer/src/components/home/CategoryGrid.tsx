import { Card, CardContent } from '@repo/ui'
import { Grid3X3 } from 'lucide-react'
import Link from 'next/link'

async function getCategories(): Promise<string[]> {
    try {
        const baseURL = process.env.NEXT_PUBLIC_API_URL || 'http://localhost:8080/api'
        const res = await fetch(`${baseURL}/consumer/products/categories`, {
            next: { revalidate: 1800 },
        })
        if (!res.ok) return []
        const json = await res.json()
        return json.data ?? json ?? []
    } catch {
        return []
    }
}

export async function CategoryGrid() {
    const categories = await getCategories()

    if (categories.length === 0) {
        return null
    }

    return (
        <section className="container-shop py-12">
            <h2 className="text-2xl font-bold text-foreground mb-6">商品分類</h2>
            <div className="grid grid-cols-2 sm:grid-cols-3 lg:grid-cols-4 xl:grid-cols-6 gap-4">
                {categories.map((category) => (
                    <Link
                        key={category}
                        href={`/products?category=${encodeURIComponent(category)}`}
                        className="cursor-pointer"
                    >
                        <Card className="rounded-xl shadow-sm border-stone-100 hover:shadow-md hover:border-primary/20 transition-all duration-200 text-center">
                            <CardContent className="p-6 flex flex-col items-center gap-3">
                                <div className="h-10 w-10 rounded-lg bg-primary/10 flex items-center justify-center">
                                    <Grid3X3 className="h-5 w-5 text-primary" />
                                </div>
                                <span className="text-sm font-medium text-foreground">
                                    {category}
                                </span>
                            </CardContent>
                        </Card>
                    </Link>
                ))}
            </div>
        </section>
    )
}
