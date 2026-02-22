"use client"

import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query"
import { useApiServices } from "../provider"
import { queryKeys } from "../query-keys"

export const usePromotions = () => {
    const { promotion } = useApiServices()
    return useQuery({
        queryKey: queryKeys.promotions,
        queryFn: () => promotion.list(),
        staleTime: 5 * 60 * 1000,
    })
}

export const useActivePromotions = () => {
    const { promotion } = useApiServices()
    return useQuery({
        queryKey: queryKeys.activePromotions,
        queryFn: () => promotion.getActive(),
        staleTime: 2 * 60 * 1000,
    })
}

export const useApplyPromotion = () => {
    const { promotion } = useApiServices()
    const queryClient = useQueryClient()
    return useMutation({
        mutationFn: ({ orderId, promotionId }: { orderId: string; promotionId: string }) =>
            promotion.apply(orderId, promotionId),
        onSuccess: (_, { orderId }) => {
            queryClient.invalidateQueries({ queryKey: queryKeys.order(orderId) })
        },
    })
}
