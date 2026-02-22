import type { AxiosInstance } from "axios"
import { apiRequest, buildQueryParams } from "../client"
import type { PageRequest, PageResponse } from "../types/common"
import type { AddOrderItemRequest, CreateOrderRequest, Order } from "../types/order"

export function createOrderService(client: AxiosInstance) {
    return {
        create: (request: CreateOrderRequest) =>
            apiRequest<Order>(client, "POST", "/orders", request),

        get: (orderId: string) =>
            apiRequest<Order>(client, "GET", `/orders/${orderId}`),

        list: (params?: PageRequest) =>
            apiRequest<PageResponse<Order>>(client, "GET", `/orders${buildQueryParams(params)}`),

        addItem: (orderId: string, request: AddOrderItemRequest) =>
            apiRequest<void>(client, "POST", `/orders/${orderId}/items`, request),

        submit: (orderId: string) =>
            apiRequest<void>(client, "POST", `/orders/${orderId}/submit`),

        cancel: (orderId: string) =>
            apiRequest<void>(client, "POST", `/orders/${orderId}/cancel`),
    }
}

export type OrderService = ReturnType<typeof createOrderService>
