import type { AxiosInstance } from "axios"
import { apiRequest } from "../client"
import type { AddCartItemRequest, CartItem, UpdateCartItemRequest } from "../types/cart"

export function createCartService(client: AxiosInstance) {
    return {
        getItems: (customerId: string) =>
            apiRequest<CartItem[]>(client, "GET", `/consumer/cart/${customerId}/items`),

        addItem: (customerId: string, request: AddCartItemRequest) =>
            apiRequest<void>(client, "POST", `/consumer/cart/${customerId}/items`, request),

        updateItem: (customerId: string, request: UpdateCartItemRequest) =>
            apiRequest<void>(client, "PUT", `/consumer/cart/${customerId}/items`, request),

        removeItem: (customerId: string, productId: string) =>
            apiRequest<void>(client, "DELETE", `/consumer/cart/${customerId}/items/${productId}`),

        clear: (customerId: string) =>
            apiRequest<void>(client, "DELETE", `/consumer/cart/${customerId}/items`),
    }
}

export type CartService = ReturnType<typeof createCartService>
