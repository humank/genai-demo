export interface Money {
    amount: number
    currency: string
}

export interface OrderId {
    value: string
}

export interface CustomerId {
    value: string
}

export interface ProductId {
    value: string
}

export interface ApiResponse<T> {
    success: boolean
    data?: T
    message?: string
    errors?: string[]
}

export interface PageRequest {
    page: number
    size: number
    sort?: string
    direction?: "ASC" | "DESC"
}

export interface PageResponse<T> {
    content: T[]
    totalElements: number
    totalPages: number
    size: number
    number: number
    first: boolean
    last: boolean
}
