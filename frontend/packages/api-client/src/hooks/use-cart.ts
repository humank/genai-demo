"use client"

import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query"
import { useApiServices } from "../provider"
import { queryKeys } from "../query-keys"
import type { AddCartItemRequest, UpdateCartItemRequest } from "../types/cart"

export const useCart = (customerId: string) => {
    const { cart } = useApiServices()
    return useQuery({
        queryKey: queryKeys.cart(customerId),
        queryFn: () => cart.getItems(customerId),
        enabled: !!customerId,
        staleTime: 2 * 60 * 1000,
    })
}

export const useAddToCart = () => {
    const { cart } = useApiServices()
    const queryClient = useQueryClient()
    return useMutation({
        mutationFn: ({ customerId, item }: { customerId: string; item: AddCartItemRequest }) =>
            cart.addItem(customerId, item),
        onSuccess: (_, { customerId }) => {
            queryClient.invalidateQueries({ queryKey: queryKeys.cart(customerId) })
        },
    })
}

export const useUpdateCartItem = () => {
    const { cart } = useApiServices()
    const queryClient = useQueryClient()
    return useMutation({
        mutationFn: ({ customerId, item }: { customerId: string; item: UpdateCartItemRequest }) =>
            cart.updateItem(customerId, item),
        onSuccess: (_, { customerId }) => {
            queryClient.invalidateQueries({ queryKey: queryKeys.cart(customerId) })
        },
    })
}

export const useRemoveCartItem = () => {
    const { cart } = useApiServices()
    const queryClient = useQueryClient()
    return useMutation({
        mutationFn: ({ customerId, productId }: { customerId: string; productId: string }) =>
            cart.removeItem(customerId, productId),
        onSuccess: (_, { customerId }) => {
            queryClient.invalidateQueries({ queryKey: queryKeys.cart(customerId) })
        },
    })
}

export const useClearCart = () => {
    const { cart } = useApiServices()
    const queryClient = useQueryClient()
    return useMutation({
        mutationFn: (customerId: string) => cart.clear(customerId),
        onSuccess: (_, customerId) => {
            queryClient.invalidateQueries({ queryKey: queryKeys.cart(customerId) })
        },
    })
}
