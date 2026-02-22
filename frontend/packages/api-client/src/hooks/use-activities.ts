"use client"

import { useQuery } from "@tanstack/react-query"
import { useApiServices } from "../provider"
import { queryKeys } from "../query-keys"

export const useActivities = (params?: { limit?: number }) => {
    const { activity } = useApiServices()
    return useQuery({
        queryKey: [...queryKeys.activities, params],
        queryFn: () => activity.list(params),
        staleTime: 2 * 60 * 1000,
    })
}
