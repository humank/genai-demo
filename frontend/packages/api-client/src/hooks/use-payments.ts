"use client"

import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query"
import { useApiServices } from "../provider"
import { queryKeys } from "../query-keys"
import type { PageRequest } from "../types/common"
import type { ProcessPaymentRequest } from "../types/payment"

export const usePayments = (params?: PageRequest) => {
    const { payment } = useApiServices()
    return useQuery({
        queryKey: [...queryKeys.payments, params],
        queryFn: () => payment.list(params),
        staleTime: 5 * 60 * 1000,
    })
}

export const useProcessPayment = () => {
    const { payment } = useApiServices()
    const queryClient = useQueryClient()
    return useMutation({
        mutationFn: (request: ProcessPaymentRequest) => payment.process(request),
        onSuccess: (result) => {
            queryClient.invalidateQueries({ queryKey: queryKeys.orderPayments(result.orderId) })
            queryClient.invalidateQueries({ queryKey: queryKeys.order(result.orderId) })
            queryClient.invalidateQueries({ queryKey: queryKeys.payments })
        },
    })
}

export const useOrderPayments = (orderId: string) => {
    const { payment } = useApiServices()
    return useQuery({
        queryKey: queryKeys.orderPayments(orderId),
        queryFn: () => payment.getByOrder(orderId),
        enabled: !!orderId,
    })
}

export const useRefundPayment = () => {
    const { payment } = useApiServices()
    const queryClient = useQueryClient()
    return useMutation({
        mutationFn: (paymentId: string) => payment.refund(paymentId),
        onSuccess: () => {
            queryClient.invalidateQueries({ queryKey: queryKeys.payments })
        },
    })
}

export const useCancelPayment = () => {
    const { payment } = useApiServices()
    const queryClient = useQueryClient()
    return useMutation({
        mutationFn: (paymentId: string) => payment.cancel(paymentId),
        onSuccess: () => {
            queryClient.invalidateQueries({ queryKey: queryKeys.payments })
        },
    })
}
