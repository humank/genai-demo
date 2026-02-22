export interface Inventory {
    productId: string
    availableQuantity: number
    reservedQuantity: number
    totalQuantity: number
}

export interface AdjustInventoryRequest {
    quantity: number
    reason: string
    type: string
}
