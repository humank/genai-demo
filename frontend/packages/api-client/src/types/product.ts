import type { Money } from "./common"

export interface Product {
    id: string
    name: string
    description: string
    price: Money
    category: string
    inStock: boolean
    stockQuantity: number
}
