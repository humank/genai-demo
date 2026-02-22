import { Button } from '@repo/ui'
import Link from 'next/link'

export function HeroBanner() {
    return (
        <section className="relative overflow-hidden bg-gradient-to-br from-indigo-600 via-indigo-500 to-indigo-700 text-white">
            <div className="container-shop py-16 sm:py-20 lg:py-28">
                <div className="max-w-2xl">
                    <h1 className="text-3xl sm:text-4xl lg:text-5xl font-bold tracking-tight leading-tight">
                        探索精選好物
                        <br />
                        <span className="text-amber-300">享受購物樂趣</span>
                    </h1>
                    <p className="mt-4 text-base sm:text-lg text-indigo-100 max-w-lg">
                        嚴選優質商品，提供最佳購物體驗。立即瀏覽我們的精選商品系列。
                    </p>
                    <div className="mt-8 flex flex-wrap gap-4">
                        <Button asChild className="rounded-xl px-6 py-3 bg-amber-500 text-stone-900 hover:bg-amber-400 font-semibold cursor-pointer">
                            <Link href="/products">立即選購</Link>
                        </Button>
                        <Button asChild variant="outline" className="rounded-xl px-6 py-3 border-white/30 text-white hover:bg-white/10 cursor-pointer">
                            <Link href="/products">瀏覽分類</Link>
                        </Button>
                    </div>
                </div>
            </div>
            {/* Decorative circles */}
            <div className="absolute -top-24 -right-24 h-72 w-72 rounded-full bg-white/5" aria-hidden="true" />
            <div className="absolute -bottom-16 -left-16 h-56 w-56 rounded-full bg-white/5" aria-hidden="true" />
        </section>
    )
}
