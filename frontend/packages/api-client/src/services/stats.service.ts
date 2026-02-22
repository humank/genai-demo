import type { AxiosInstance } from "axios"
import { apiRequest } from "../client"

export interface StatsOverview {
    totalOrders: number
    totalRevenue: number
    totalCustomers: number
    totalInventory: number
    [key: string]: unknown
}

export interface OrderStatusStats {
    status: string
    count: number
}

export interface PaymentMethodStats {
    method: string
    count: number
    totalAmount: number
}

export function createStatsService(client: AxiosInstance) {
    return {
        getStats: () =>
            apiRequest<StatsOverview>(client, "GET", "/stats"),

        getOrderStatusStats: () =>
            apiRequest<OrderStatusStats[]>(client, "GET", "/stats/order-status"),

        getPaymentMethodStats: () =>
            apiRequest<PaymentMethodStats[]>(client, "GET", "/stats/payment-methods"),
    }
}

export type StatsService = ReturnType<typeof createStatsService>
