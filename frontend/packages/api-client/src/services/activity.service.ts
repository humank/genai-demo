import type { AxiosInstance } from "axios"
import { apiRequest, buildQueryParams } from "../client"

export interface Activity {
    id?: string
    title: string
    timestamp: string
    [key: string]: unknown
}

export function createActivityService(client: AxiosInstance) {
    return {
        list: (params?: { limit?: number }) =>
            apiRequest<Activity[]>(client, "GET", `/activities${buildQueryParams(params)}`),
    }
}

export type ActivityService = ReturnType<typeof createActivityService>
