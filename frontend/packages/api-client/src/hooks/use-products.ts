"use client"

import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query"
import { useApiServices } from "../provider"
import { queryKeys } from "../query-keys"
import type { PageRequest } from "../types/common"
import type { Product } from "../types/product"

export const useProducts = (params?: PageRequest) => {
    const { product } = useApiServices()
    return useQuery({
        queryKey: [...queryKeys.products, params],
        queryFn: () => product.list(params),
        staleTime: 10 * 60 * 1000,
    })
}

export const useProduct = (productId: string) => {
    const { product } = useApiServices()
    return useQuery({
        queryKey: queryKeys.product(productId),
        queryFn: () => product.get(productId),
        enabled: !!productId,
    })
}

export const useUpdateProduct = () => {
    const { product } = useApiServices()
    const queryClient = useQueryClient()
    return useMutation({
        mutationFn: ({ productId, data }: { productId: string; data: Partial<Product> }) =>
            product.update(productId, data),
        onSuccess: (_, { productId }) => {
            queryClient.invalidateQueries({ queryKey: queryKeys.product(productId) })
            queryClient.invalidateQueries({ queryKey: queryKeys.products })
        },
    })
}

export const useDeleteProduct = () => {
    const { product } = useApiServices()
    const queryClient = useQueryClient()
    return useMutation({
        mutationFn: (productId: string) => product.delete(productId),
        onSuccess: () => {
            queryClient.invalidateQueries({ queryKey: queryKeys.products })
        },
    })
}
