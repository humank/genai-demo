"use client"

import { useQuery } from "@tanstack/react-query"
import { useApiServices } from "../provider"
import { queryKeys } from "../query-keys"
import type { PageRequest } from "../types/common"

export const useConsumerProducts = (params?: PageRequest) => {
    const { consumerProduct } = useApiServices()
    return useQuery({
        queryKey: [...queryKeys.consumerProducts, params],
        queryFn: () => consumerProduct.list(params),
        staleTime: 10 * 60 * 1000,
    })
}

export const useProductSearch = (keyword: string, params?: PageRequest) => {
    const { consumerProduct } = useApiServices()
    return useQuery({
        queryKey: [...queryKeys.productSearch(keyword), params],
        queryFn: () => consumerProduct.search(keyword, params),
        enabled: keyword.length > 0,
        staleTime: 5 * 60 * 1000,
    })
}

export const useCategories = () => {
    const { consumerProduct } = useApiServices()
    return useQuery({
        queryKey: queryKeys.categories,
        queryFn: () => consumerProduct.getCategories(),
        staleTime: 30 * 60 * 1000,
    })
}
