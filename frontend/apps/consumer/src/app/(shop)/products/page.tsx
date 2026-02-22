'use client'

import { CategoryFilter } from '@/components/product/CategoryFilter'
import { ProductGrid } from '@/components/product/ProductGrid'
import { ProductSkeleton } from '@/components/product/ProductSkeleton'
import type { Product } from '@repo/api-client'
import { useConsumerProducts, useProductSearch } from '@repo/api-client'
import { EmptyState, Input } from '@repo/ui'
import { Package, Search } from 'lucide-react'
import { useRouter, useSearchParams } from 'next/navigation'
import { useCallback, useEffect, useMemo, useState } from 'react'

export default function ProductsPage() {
    const searchParams = useSearchParams()
    const router = useRouter()

    const initialQ = searchParams.get('q') || ''
    const initialCat = searchParams.get('category') || ''
    const initialPage = Number(searchParams.get('page') || '0')

    const [search, setSearch] = useState(initialQ)
    const [debouncedSearch, setDebouncedSearch] = useState(initialQ)
    const [category, setCategory] = useState(initialCat)
    const [page, setPage] = useState(initialPage)

    // 300ms debounce
    useEffect(() => {
        const timer = setTimeout(() => setDebouncedSearch(search), 300)
        return () => clearTimeout(timer)
    }, [search])

    // Sync URL params
    const updateUrl = useCallback((q: string, cat: string, p: number) => {
        const params = new URLSearchParams()
        if (q) params.set('q', q)
        if (cat) params.set('category', cat)
        if (p > 0) params.set('page', String(p))
        const qs = params.toString()
        router.replace(`/products${qs ? `?${qs}` : ''}`, { scroll: false })
    }, [router])

    useEffect(() => {
        updateUrl(debouncedSearch, category, page)
    }, [debouncedSearch, category, page, updateUrl])

    const isSearching = debouncedSearch.length > 0
    const pageParams = useMemo(() => ({ page, size: 12 }), [page])

    const allProducts = useConsumerProducts(pageParams)
    const searchResults = useProductSearch(debouncedSearch, pageParams)

    const query = isSearching ? searchResults : allProducts
    const rawProducts: Product[] = (query.data as { content?: Product[] })?.content ?? (query.data as Product[] | undefined) ?? []

    // Client-side category filter
    const products = category
        ? rawProducts.filter((p) => p.category === category)
        : rawProducts

    const totalPages = (query.data as { totalPages?: number })?.totalPages ?? 1

    const handleCategoryChange = (cat: string) => {
        setCategory(cat)
        setPage(0)
    }

    return (
        <div className="container-shop py-8">
            <h1 className="text-2xl font-bold text-foreground mb-6">所有商品</h1>

            {/* Search + Filter */}
            <div className="space-y-4 mb-8">
                <div className="relative max-w-md">
                    <Search className="absolute left-3 top-1/2 -translate-y-1/2 h-4 w-4 text-muted-foreground" />
                    <Input
                        placeholder="搜尋商品..."
                        value={search}
                        onChange={(e) => { setSearch(e.target.value); setPage(0) }}
                        className="pl-10 rounded-lg"
                    />
                </div>
                <CategoryFilter selected={category} onSelect={handleCategoryChange} />
            </div>

            {/* Products */}
            {query.isLoading ? (
                <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 xl:grid-cols-4 gap-6">
                    {Array.from({ length: 8 }).map((_, i) => (
                        <ProductSkeleton key={i} />
                    ))}
                </div>
            ) : products.length === 0 ? (
                <EmptyState
                    icon={Package}
                    title="找不到商品"
                    description={debouncedSearch ? `沒有符合「${debouncedSearch}」的商品` : '目前沒有商品'}
                />
            ) : (
                <>
                    <ProductGrid products={products} />

                    {/* Pagination */}
                    {totalPages > 1 && (
                        <div className="flex justify-center gap-2 mt-8">
                            {Array.from({ length: totalPages }).map((_, i) => (
                                <button
                                    key={i}
                                    onClick={() => setPage(i)}
                                    className={`h-9 w-9 rounded-lg text-sm font-medium transition-colors cursor-pointer ${page === i
                                        ? 'bg-primary text-primary-foreground'
                                        : 'bg-white border border-stone-200 text-foreground hover:bg-stone-50'
                                        }`}
                                >
                                    {i + 1}
                                </button>
                            ))}
                        </div>
                    )}
                </>
            )}
        </div>
    )
}
