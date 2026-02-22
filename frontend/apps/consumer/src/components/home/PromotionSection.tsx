import { Badge, Card, CardContent } from '@repo/ui'
import { Sparkles } from 'lucide-react'

interface Promotion {
    id: string
    name: string
    description: string
    type: string
    discountPercentage?: number
    isActive: boolean
}

async function getActivePromotions(): Promise<Promotion[]> {
    try {
        const baseURL = process.env.NEXT_PUBLIC_API_URL || 'http://localhost:8080/api'
        const res = await fetch(`${baseURL}/promotions/active`, {
            next: { revalidate: 120 },
        })
        if (!res.ok) return []
        const json = await res.json()
        return json.data ?? json ?? []
    } catch {
        return []
    }
}

export async function PromotionSection() {
    const promotions = await getActivePromotions()

    if (promotions.length === 0) {
        return null
    }

    return (
        <section className="container-shop py-12">
            <h2 className="text-2xl font-bold text-foreground mb-6">優惠活動</h2>
            <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 gap-6">
                {promotions.map((promo) => (
                    <Card
                        key={promo.id}
                        className="rounded-xl shadow-sm border-amber-100 bg-gradient-to-br from-amber-50 to-white overflow-hidden"
                    >
                        <CardContent className="p-6">
                            <div className="flex items-center gap-2 mb-3">
                                <Sparkles className="h-5 w-5 text-amber-500" />
                                <Badge className="bg-amber-100 text-amber-700 hover:bg-amber-100 border-0">
                                    {promo.type === 'DISCOUNT' && '折扣'}
                                    {promo.type === 'FLASH_SALE' && '限時特賣'}
                                    {promo.type === 'GIFT_WITH_PURCHASE' && '滿額贈'}
                                    {promo.type === 'ADD_ON' && '加購優惠'}
                                    {!['DISCOUNT', 'FLASH_SALE', 'GIFT_WITH_PURCHASE', 'ADD_ON'].includes(promo.type) && '優惠'}
                                </Badge>
                            </div>
                            <h3 className="text-lg font-semibold text-foreground mb-1">
                                {promo.name}
                            </h3>
                            <p className="text-sm text-muted-foreground line-clamp-2">
                                {promo.description}
                            </p>
                            {promo.discountPercentage && (
                                <p className="mt-3 text-2xl font-bold text-amber-600">
                                    {promo.discountPercentage}% OFF
                                </p>
                            )}
                        </CardContent>
                    </Card>
                ))}
            </div>
        </section>
    )
}
