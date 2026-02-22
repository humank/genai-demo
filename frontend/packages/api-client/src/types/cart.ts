import type { Money } from "./common"

export interface CartItem {
    productId: string
    productName: string
    quantity: number
    unitPrice: Money
    totalPrice: Money
}

export interface AddCartItemRequest {
    productId: string
    quantity: number
}

export interface UpdateCartItemRequest {
    productId: string
    quantity: number
}
