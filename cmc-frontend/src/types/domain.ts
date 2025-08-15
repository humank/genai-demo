// 對應後端領域模型的 TypeScript 類型定義

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

export enum OrderStatus {
  CREATED = 'CREATED',
  PENDING = 'PENDING',
  CONFIRMED = 'CONFIRMED',
  SHIPPED = 'SHIPPED',
  DELIVERED = 'DELIVERED',
  CANCELLED = 'CANCELLED'
}

export enum PaymentStatus {
  PENDING = 'PENDING',
  PROCESSING = 'PROCESSING',
  COMPLETED = 'COMPLETED',
  FAILED = 'FAILED',
  CANCELLED = 'CANCELLED'
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

export interface Product {
  id: string
  name: string
  description: string
  price: Money
  category: string
  inStock: boolean
  stockQuantity: number
}

export interface Customer {
  id: string
  name: string
  email: string
  phone: string
  address: string
  membershipLevel: string
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

export interface Inventory {
  productId: string
  availableQuantity: number
  reservedQuantity: number
  totalQuantity: number
}

export interface Promotion {
  id: string
  name: string
  description: string
  type: 'DISCOUNT' | 'FLASH_SALE' | 'GIFT_WITH_PURCHASE' | 'ADD_ON'
  discountAmount?: Money
  discountPercentage?: number
  startTime: string
  endTime: string
  isActive: boolean
  conditions: PromotionCondition[]
}

export interface PromotionCondition {
  type: 'MIN_AMOUNT' | 'PRODUCT_CATEGORY' | 'QUANTITY'
  value: string | number
}

// API 請求/響應類型
export interface CreateOrderRequest {
  customerId: string
  shippingAddress: string
}

export interface AddOrderItemRequest {
  productId: string
  quantity: number
}

export interface ProcessPaymentRequest {
  orderId: string
  amount: Money
  method: string
}

export interface ApiResponse<T> {
  success: boolean
  data?: T
  message?: string
  errors?: string[]
}

// 分頁相關
export interface PageRequest {
  page: number
  size: number
  sort?: string
  direction?: 'ASC' | 'DESC'
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
