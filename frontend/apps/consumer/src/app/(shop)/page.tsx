import { CategoryGrid } from '@/components/home/CategoryGrid'
import { FeaturedProducts } from '@/components/home/FeaturedProducts'
import { HeroBanner } from '@/components/home/HeroBanner'
import { Newsletter } from '@/components/home/Newsletter'
import { PromotionSection } from '@/components/home/PromotionSection'
import { Suspense } from 'react'
import { HomeSkeleton } from './loading'

export default function HomePage() {
    return (
        <>
            <HeroBanner />
            <Suspense fallback={<HomeSkeleton />}>
                <FeaturedProducts />
            </Suspense>
            <Suspense fallback={null}>
                <CategoryGrid />
            </Suspense>
            <Suspense fallback={null}>
                <PromotionSection />
            </Suspense>
            <Newsletter />
        </>
    )
}
