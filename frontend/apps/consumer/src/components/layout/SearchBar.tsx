'use client'

import { Input } from '@repo/ui'
import { Search } from 'lucide-react'
import { useRouter } from 'next/navigation'
import { useCallback, useEffect, useRef, useState } from 'react'

export function SearchBar({ className }: { className?: string }) {
    const router = useRouter()
    const [value, setValue] = useState('')
    const timerRef = useRef<ReturnType<typeof setTimeout> | null>(null)

    const handleSearch = useCallback((keyword: string) => {
        if (keyword.trim()) {
            router.push(`/products?q=${encodeURIComponent(keyword.trim())}`)
        }
    }, [router])

    useEffect(() => {
        return () => {
            if (timerRef.current) clearTimeout(timerRef.current)
        }
    }, [])

    const onChange = (e: React.ChangeEvent<HTMLInputElement>) => {
        const v = e.target.value
        setValue(v)
        if (timerRef.current) clearTimeout(timerRef.current)
        timerRef.current = setTimeout(() => handleSearch(v), 300)
    }

    const onKeyDown = (e: React.KeyboardEvent<HTMLInputElement>) => {
        if (e.key === 'Enter') {
            if (timerRef.current) clearTimeout(timerRef.current)
            handleSearch(value)
        }
    }

    return (
        <div className={`relative ${className ?? ''}`}>
            <Search className="absolute left-3 top-1/2 -translate-y-1/2 h-4 w-4 text-muted-foreground" />
            <Input
                type="search"
                placeholder="搜尋商品..."
                value={value}
                onChange={onChange}
                onKeyDown={onKeyDown}
                className="pl-9 rounded-xl bg-stone-50 border-stone-200 focus:bg-white"
                aria-label="搜尋商品"
            />
        </div>
    )
}
