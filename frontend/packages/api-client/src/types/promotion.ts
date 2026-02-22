import type { Money } from "./common"

export interface Promotion {
    id: string
    name: string
    description: string
    type: "DISCOUNT" | "FLASH_SALE" | "GIFT_WITH_PURCHASE" | "ADD_ON"
    discountAmount?: Money
    discountPercentage?: number
    startTime: string
    endTime: string
    isActive: boolean
    conditions: PromotionCondition[]
}

export interface PromotionCondition {
    type: "MIN_AMOUNT" | "PRODUCT_CATEGORY" | "QUANTITY"
    value: string | number
}
