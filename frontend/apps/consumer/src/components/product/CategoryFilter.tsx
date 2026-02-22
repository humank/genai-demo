'use client'

import { useCategories } from '@repo/api-client'
import { Button, Skeleton } from '@repo/ui'

interface CategoryFilterProps {
    selected: string
    onSelect: (category: string) => void
}

export function CategoryFilter({ selected, onSelect }: CategoryFilterProps) {
    const { data: categories, isLoading } = useCategories()

    if (isLoading) {
        return (
            <div className="flex gap-2 flex-wrap">
                {Array.from({ length: 5 }).map((_, i) => (
                    <Skeleton key={i} className="h-9 w-20 rounded-lg" />
                ))}
            </div>
        )
    }

    const items = categories ?? []

    return (
        <div className="flex gap-2 flex-wrap">
            <Button
                variant={selected === '' ? 'default' : 'outline'}
                size="sm"
                className="rounded-lg cursor-pointer"
                onClick={() => onSelect('')}
            >
                全部
            </Button>
            {items.map((cat) => (
                <Button
                    key={cat}
                    variant={selected === cat ? 'default' : 'outline'}
                    size="sm"
                    className="rounded-lg cursor-pointer"
                    onClick={() => onSelect(cat)}
                >
                    {cat}
                </Button>
            ))}
        </div>
    )
}
