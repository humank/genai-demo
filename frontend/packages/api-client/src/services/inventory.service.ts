import type { AxiosInstance } from "axios"
import { apiRequest } from "../client"
import type { AdjustInventoryRequest, Inventory } from "../types/inventory"

export function createInventoryService(client: AxiosInstance) {
    return {
        get: (productId: string) =>
            apiRequest<Inventory>(client, "GET", `/inventory/${productId}`),

        adjust: (productId: string, adjustment: AdjustInventoryRequest) =>
            apiRequest<Inventory>(client, "POST", `/inventory/${productId}/adjust`, adjustment),

        check: (productId: string, quantity: number) =>
            apiRequest<boolean>(client, "GET", `/inventory/${productId}/check?quantity=${quantity}`),
    }
}

export type InventoryService = ReturnType<typeof createInventoryService>
