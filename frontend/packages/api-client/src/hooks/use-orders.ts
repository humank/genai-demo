"use client"

import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query"
import { useApiServices } from "../provider"
import { queryKeys } from "../query-keys"
import type { PageRequest } from "../types/common"
import type { AddOrderItemRequest, CreateOrderRequest } from "../types/order"

export const useOrders = (params?: PageRequest) => {
    const { order } = useApiServices()
    return useQuery({
        queryKey: [...queryKeys.orders, params],
        queryFn: () => order.list(params),
        staleTime: 5 * 60 * 1000,
    })
}

export const useOrder = (orderId: string) => {
    const { order } = useApiServices()
    return useQuery({
        queryKey: queryKeys.order(orderId),
        queryFn: () => order.get(orderId),
        enabled: !!orderId,
    })
}

export const useCreateOrder = () => {
    const { order } = useApiServices()
    const queryClient = useQueryClient()
    return useMutation({
        mutationFn: (request: CreateOrderRequest) => order.create(request),
        onSuccess: () => {
            queryClient.invalidateQueries({ queryKey: queryKeys.orders })
        },
    })
}

export const useAddOrderItem = () => {
    const { order } = useApiServices()
    const queryClient = useQueryClient()
    return useMutation({
        mutationFn: ({ orderId, request }: { orderId: string; request: AddOrderItemRequest }) =>
            order.addItem(orderId, request),
        onSuccess: (_, { orderId }) => {
            queryClient.invalidateQueries({ queryKey: queryKeys.order(orderId) })
            queryClient.invalidateQueries({ queryKey: queryKeys.orders })
        },
    })
}

export const useSubmitOrder = () => {
    const { order } = useApiServices()
    const queryClient = useQueryClient()
    return useMutation({
        mutationFn: (orderId: string) => order.submit(orderId),
        onSuccess: (_, orderId) => {
            queryClient.invalidateQueries({ queryKey: queryKeys.order(orderId) })
            queryClient.invalidateQueries({ queryKey: queryKeys.orders })
        },
    })
}

export const useCancelOrder = () => {
    const { order } = useApiServices()
    const queryClient = useQueryClient()
    return useMutation({
        mutationFn: (orderId: string) => order.cancel(orderId),
        onSuccess: (_, orderId) => {
            queryClient.invalidateQueries({ queryKey: queryKeys.order(orderId) })
            queryClient.invalidateQueries({ queryKey: queryKeys.orders })
        },
    })
}
