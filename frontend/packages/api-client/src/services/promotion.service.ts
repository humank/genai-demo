import type { AxiosInstance } from "axios"
import { apiRequest } from "../client"
import type { Promotion } from "../types/promotion"

export function createPromotionService(client: AxiosInstance) {
    return {
        list: () =>
            apiRequest<Promotion[]>(client, "GET", "/promotions"),

        getActive: () =>
            apiRequest<Promotion[]>(client, "GET", "/promotions/active"),

        apply: (orderId: string, promotionId: string) =>
            apiRequest<void>(client, "POST", `/orders/${orderId}/promotions/${promotionId}`),
    }
}

export type PromotionService = ReturnType<typeof createPromotionService>
