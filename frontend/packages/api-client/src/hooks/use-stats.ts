"use client"

import { useQuery } from "@tanstack/react-query"
import { useApiServices } from "../provider"
import { queryKeys } from "../query-keys"

export const useStats = () => {
    const { stats } = useApiServices()
    return useQuery({
        queryKey: queryKeys.stats,
        queryFn: () => stats.getStats(),
        staleTime: 5 * 60 * 1000,
    })
}

export const useOrderStatusStats = () => {
    const { stats } = useApiServices()
    return useQuery({
        queryKey: queryKeys.orderStatusStats,
        queryFn: () => stats.getOrderStatusStats(),
        staleTime: 5 * 60 * 1000,
    })
}

export const usePaymentMethodStats = () => {
    const { stats } = useApiServices()
    return useQuery({
        queryKey: queryKeys.paymentMethodStats,
        queryFn: () => stats.getPaymentMethodStats(),
        staleTime: 5 * 60 * 1000,
    })
}
