'use client'

import { useQueryClient } from '@tanstack/react-query'
import { createContext, useContext, useEffect, useState } from 'react'

interface AutoRefreshContextValue {
    enabled: boolean
    interval: number
    toggle: () => void
}

const AutoRefreshContext = createContext<AutoRefreshContextValue>({
    enabled: true,
    interval: 60_000,
    toggle: () => { },
})

export const useAutoRefresh = () => useContext(AutoRefreshContext)

export function AutoRefreshProvider({
    children,
    interval = 60_000,
}: {
    children: React.ReactNode
    interval?: number
}) {
    const [enabled, setEnabled] = useState(true)
    const queryClient = useQueryClient()

    useEffect(() => {
        if (!enabled) return
        const timer = setInterval(() => {
            queryClient.invalidateQueries({ queryKey: ['stats'] })
        }, interval)
        return () => clearInterval(timer)
    }, [enabled, interval, queryClient])

    return (
        <AutoRefreshContext.Provider value={{ enabled, interval, toggle: () => setEnabled((v) => !v) }}>
            {children}
        </AutoRefreshContext.Provider>
    )
}
