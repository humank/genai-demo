"use client"

import { useQuery } from "@tanstack/react-query"
import { useApiServices } from "../provider"
import { queryKeys } from "../query-keys"
import type { PageRequest } from "../types/common"

export const useCustomers = (params?: PageRequest) => {
    const { customer } = useApiServices()
    return useQuery({
        queryKey: [...queryKeys.customers, params],
        queryFn: () => customer.list(params),
        staleTime: 5 * 60 * 1000,
    })
}

export const useCustomer = (customerId: string) => {
    const { customer } = useApiServices()
    return useQuery({
        queryKey: queryKeys.customer(customerId),
        queryFn: () => customer.get(customerId),
        enabled: !!customerId,
    })
}
