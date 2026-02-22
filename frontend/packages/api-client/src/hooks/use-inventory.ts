"use client"

import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query"
import { useApiServices } from "../provider"
import { queryKeys } from "../query-keys"
import type { AdjustInventoryRequest } from "../types/inventory"

export const useInventory = (productId: string) => {
    const { inventory } = useApiServices()
    return useQuery({
        queryKey: queryKeys.inventory(productId),
        queryFn: () => inventory.get(productId),
        enabled: !!productId,
        staleTime: 2 * 60 * 1000,
    })
}

export const useAdjustInventory = () => {
    const { inventory } = useApiServices()
    const queryClient = useQueryClient()
    return useMutation({
        mutationFn: ({ productId, adjustment }: { productId: string; adjustment: AdjustInventoryRequest }) =>
            inventory.adjust(productId, adjustment),
        onSuccess: (_, { productId }) => {
            queryClient.invalidateQueries({ queryKey: queryKeys.inventory(productId) })
            queryClient.invalidateQueries({ queryKey: queryKeys.products })
        },
    })
}

export const useCheckInventory = () => {
    const { inventory } = useApiServices()
    return useMutation({
        mutationFn: ({ productId, quantity }: { productId: string; quantity: number }) =>
            inventory.check(productId, quantity),
    })
}
