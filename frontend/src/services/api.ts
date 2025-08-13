import axios, { AxiosInstance, AxiosResponse } from 'axios'
import { 
  Order, 
  Product, 
  Customer, 
  Payment, 
  Inventory, 
  Promotion,
  CreateOrderRequest,
  AddOrderItemRequest,
  ProcessPaymentRequest,
  ApiResponse,
  PageRequest,
  PageResponse
} from '@/types/domain'

class ApiClient {
  private client: AxiosInstance

  constructor() {
    this.client = axios.create({
      baseURL: process.env.NEXT_PUBLIC_API_URL || 'http://localhost:8080/api',
      timeout: 10000,
      headers: {
        'Content-Type': 'application/json',
      },
    })

    // 請求攔截器
    this.client.interceptors.request.use(
      (config) => {
        // 可以在這裡添加認證 token
        const token = localStorage.getItem('auth_token')
        if (token) {
          config.headers.Authorization = `Bearer ${token}`
        }
        return config
      },
      (error) => Promise.reject(error)
    )

    // 響應攔截器
    this.client.interceptors.response.use(
      (response) => response,
      (error) => {
        if (error.response?.status === 401) {
          // 處理未授權錯誤
          localStorage.removeItem('auth_token')
          window.location.href = '/login'
        }
        return Promise.reject(error)
      }
    )
  }

  private async request<T>(
    method: 'GET' | 'POST' | 'PUT' | 'DELETE',
    url: string,
    data?: any
  ): Promise<T> {
    try {
      const response: AxiosResponse<ApiResponse<T>> = await this.client.request({
        method,
        url,
        data,
      })
      
      if (response.data.success) {
        return response.data.data as T
      } else {
        throw new Error(response.data.message || 'API request failed')
      }
    } catch (error) {
      console.error(`API ${method} ${url} failed:`, error)
      throw error
    }
  }

  // 訂單相關 API
  async createOrder(request: CreateOrderRequest): Promise<Order> {
    return this.request<Order>('POST', '/orders', request)
  }

  async getOrder(orderId: string): Promise<Order> {
    return this.request<Order>('GET', `/orders/${orderId}`)
  }

  async getOrders(params?: PageRequest): Promise<PageResponse<Order>> {
    const queryParams = new URLSearchParams()
    if (params) {
      Object.entries(params).forEach(([key, value]) => {
        if (value !== undefined) {
          queryParams.append(key, value.toString())
        }
      })
    }
    return this.request<PageResponse<Order>>('GET', `/orders?${queryParams}`)
  }

  async addOrderItem(orderId: string, request: AddOrderItemRequest): Promise<void> {
    return this.request<void>('POST', `/orders/${orderId}/items`, request)
  }

  async submitOrder(orderId: string): Promise<void> {
    return this.request<void>('POST', `/orders/${orderId}/submit`)
  }

  async cancelOrder(orderId: string): Promise<void> {
    return this.request<void>('POST', `/orders/${orderId}/cancel`)
  }

  // 產品相關 API
  async getProducts(params?: PageRequest): Promise<PageResponse<Product>> {
    const queryParams = new URLSearchParams()
    if (params) {
      Object.entries(params).forEach(([key, value]) => {
        if (value !== undefined) {
          queryParams.append(key, value.toString())
        }
      })
    }
    return this.request<PageResponse<Product>>('GET', `/products?${queryParams}`)
  }

  async getProduct(productId: string): Promise<Product> {
    return this.request<Product>('GET', `/products/${productId}`)
  }

  // 客戶相關 API
  async getCustomer(customerId: string): Promise<Customer> {
    return this.request<Customer>('GET', `/customers/${customerId}`)
  }

  async getCustomers(params?: PageRequest): Promise<PageResponse<Customer>> {
    const queryParams = new URLSearchParams()
    if (params) {
      Object.entries(params).forEach(([key, value]) => {
        if (value !== undefined) {
          queryParams.append(key, value.toString())
        }
      })
    }
    return this.request<PageResponse<Customer>>('GET', `/customers?${queryParams}`)
  }

  // 支付相關 API
  async processPayment(request: ProcessPaymentRequest): Promise<Payment> {
    return this.request<Payment>('POST', '/payments', request)
  }

  async getPayment(paymentId: string): Promise<Payment> {
    return this.request<Payment>('GET', `/payments/${paymentId}`)
  }

  async getOrderPayments(orderId: string): Promise<Payment[]> {
    return this.request<Payment[]>('GET', `/orders/${orderId}/payments`)
  }

  // 庫存相關 API
  async getInventory(productId: string): Promise<Inventory> {
    return this.request<Inventory>('GET', `/inventory/${productId}`)
  }

  async checkInventory(productId: string, quantity: number): Promise<boolean> {
    return this.request<boolean>('GET', `/inventory/${productId}/check?quantity=${quantity}`)
  }

  // 促銷相關 API
  async getPromotions(): Promise<Promotion[]> {
    return this.request<Promotion[]>('GET', '/promotions')
  }

  async getActivePromotions(): Promise<Promotion[]> {
    return this.request<Promotion[]>('GET', '/promotions/active')
  }

  async applyPromotion(orderId: string, promotionId: string): Promise<void> {
    return this.request<void>('POST', `/orders/${orderId}/promotions/${promotionId}`)
  }
}

// 單例模式
export const apiClient = new ApiClient()

// 導出具體的 API 服務
export const orderService = {
  create: (request: CreateOrderRequest) => apiClient.createOrder(request),
  get: (id: string) => apiClient.getOrder(id),
  list: (params?: PageRequest) => apiClient.getOrders(params),
  addItem: (orderId: string, request: AddOrderItemRequest) => 
    apiClient.addOrderItem(orderId, request),
  submit: (id: string) => apiClient.submitOrder(id),
  cancel: (id: string) => apiClient.cancelOrder(id),
}

export const productService = {
  list: (params?: PageRequest) => apiClient.getProducts(params),
  get: (id: string) => apiClient.getProduct(id),
}

export const customerService = {
  get: (id: string) => apiClient.getCustomer(id),
  list: (params?: PageRequest) => apiClient.getCustomers(params),
}

export const paymentService = {
  process: (request: ProcessPaymentRequest) => apiClient.processPayment(request),
  get: (id: string) => apiClient.getPayment(id),
  getByOrder: (orderId: string) => apiClient.getOrderPayments(orderId),
}

export const inventoryService = {
  get: (productId: string) => apiClient.getInventory(productId),
  check: (productId: string, quantity: number) => 
    apiClient.checkInventory(productId, quantity),
}

export const promotionService = {
  list: () => apiClient.getPromotions(),
  getActive: () => apiClient.getActivePromotions(),
  apply: (orderId: string, promotionId: string) => 
    apiClient.applyPromotion(orderId, promotionId),
}
