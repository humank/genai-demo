import type { Money } from "./common"

export enum OrderStatus {
    CREATED = "CREATED",
    PENDING = "PENDING",
    CONFIRMED = "CONFIRMED",
    SHIPPED = "SHIPPED",
    DELIVERED = "DELIVERED",
    CANCELLED = "CANCELLED",
}

export interface OrderItem {
    productId: string
    productName: string
    quantity: number
    unitPrice: Money
    totalPrice: Money
}

export interface Order {
    id: string
    customerId: string
    shippingAddress: string
    items: OrderItem[]
    status: OrderStatus
    totalAmount: Money
    effectiveAmount: Money
    createdAt: string
    updatedAt: string
}

export interface CreateOrderRequest {
    customerId: string
    shippingAddress: string
}

export interface AddOrderItemRequest {
    productId: string
    quantity: number
}
