import type { AxiosInstance } from "axios"
import { apiRequest, buildQueryParams } from "../client"
import type { PageRequest, PageResponse } from "../types/common"
import type { Payment, ProcessPaymentRequest } from "../types/payment"

export function createPaymentService(client: AxiosInstance) {
    return {
        process: (request: ProcessPaymentRequest) =>
            apiRequest<Payment>(client, "POST", "/payments", request),

        get: (paymentId: string) =>
            apiRequest<Payment>(client, "GET", `/payments/${paymentId}`),

        list: (params?: PageRequest) =>
            apiRequest<PageResponse<Payment>>(client, "GET", `/payments${buildQueryParams(params)}`),

        getByOrder: (orderId: string) =>
            apiRequest<Payment[]>(client, "GET", `/orders/${orderId}/payments`),

        refund: (paymentId: string) =>
            apiRequest<Payment>(client, "POST", `/payments/${paymentId}/refund`),

        cancel: (paymentId: string) =>
            apiRequest<Payment>(client, "POST", `/payments/${paymentId}/cancel`),
    }
}

export type PaymentService = ReturnType<typeof createPaymentService>
