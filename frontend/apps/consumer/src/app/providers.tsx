'use client'

import { ApiServicesProvider, createApiClient } from '@repo/api-client'
import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import React from 'react'

const apiClient = createApiClient({
    baseURL: process.env.NEXT_PUBLIC_API_URL || 'http://localhost:8080/api',
    onUnauthorized: () => {
        if (typeof window !== 'undefined') {
            localStorage.removeItem('consumer_customer_id')
        }
    },
})

const queryClient = new QueryClient({
    defaultOptions: {
        queries: {
            staleTime: 5 * 60 * 1000,
            gcTime: 10 * 60 * 1000,
            retry: (failureCount, error: unknown) => {
                const err = error as { response?: { status?: number } }
                if (err?.response?.status && err.response.status >= 400 && err.response.status < 500) {
                    return false
                }
                return failureCount < 3
            },
            refetchOnWindowFocus: false,
        },
        mutations: {
            retry: 1,
        },
    },
})

export function Providers({ children }: { children: React.ReactNode }) {
    return (
        <QueryClientProvider client={queryClient}>
            <ApiServicesProvider client={apiClient}>
                {children}
            </ApiServicesProvider>
        </QueryClientProvider>
    )
}
