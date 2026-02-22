import type { Money } from "./common"

export enum PaymentStatus {
    PENDING = "PENDING",
    PROCESSING = "PROCESSING",
    COMPLETED = "COMPLETED",
    FAILED = "FAILED",
    CANCELLED = "CANCELLED",
}

export interface Payment {
    id: string
    orderId: string
    amount: Money
    method: string
    status: PaymentStatus
    createdAt: string
    processedAt?: string
}

export interface ProcessPaymentRequest {
    orderId: string
    amount: Money
    method: string
}
