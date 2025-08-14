import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query'
import {
  orderService,
  productService,
  customerService,
  paymentService,
  inventoryService,
  promotionService,
  statsService,
  activityService,
} from '@/services/api'
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
  PageRequest,
} from '@/types/domain'

// Query Keys
export const queryKeys = {
  orders: ['orders'] as const,
  order: (id: string) => ['orders', id] as const,
  products: ['products'] as const,
  product: (id: string) => ['products', id] as const,
  customers: ['customers'] as const,
  customer: (id: string) => ['customers', id] as const,
  payments: ['payments'] as const,
  payment: (id: string) => ['payments', id] as const,
  orderPayments: (orderId: string) => ['payments', 'order', orderId] as const,
  inventory: (productId: string) => ['inventory', productId] as const,
  promotions: ['promotions'] as const,
  activePromotions: ['promotions', 'active'] as const,
  stats: ['stats'] as const,
  orderStatusStats: ['stats', 'order-status'] as const,
  paymentMethodStats: ['stats', 'payment-methods'] as const,
  activities: ['activities'] as const,
}

// 訂單相關 hooks
export const useOrders = (params?: PageRequest) => {
  return useQuery({
    queryKey: [...queryKeys.orders, params],
    queryFn: () => orderService.list(params),
    staleTime: 5 * 60 * 1000, // 5 minutes
  })
}

export const useOrder = (orderId: string) => {
  return useQuery({
    queryKey: queryKeys.order(orderId),
    queryFn: () => orderService.get(orderId),
    enabled: !!orderId,
  })
}

export const useCreateOrder = () => {
  const queryClient = useQueryClient()
  
  return useMutation({
    mutationFn: (request: CreateOrderRequest) => orderService.create(request),
    onSuccess: (newOrder) => {
      queryClient.invalidateQueries({ queryKey: queryKeys.orders })
    },
    onError: (error: Error) => {
      throw error
    },
  })
}

export const useAddOrderItem = () => {
  const queryClient = useQueryClient()
  
  return useMutation({
    mutationFn: ({ orderId, request }: { orderId: string; request: AddOrderItemRequest }) =>
      orderService.addItem(orderId, request),
    onSuccess: (_, { orderId }) => {
      queryClient.invalidateQueries({ queryKey: queryKeys.order(orderId) })
      queryClient.invalidateQueries({ queryKey: queryKeys.orders })
    },
    onError: (error: Error) => {
      throw error
    },
  })
}

export const useSubmitOrder = () => {
  const queryClient = useQueryClient()
  
  return useMutation({
    mutationFn: (orderId: string) => orderService.submit(orderId),
    onSuccess: (_, orderId) => {
      queryClient.invalidateQueries({ queryKey: queryKeys.order(orderId) })
      queryClient.invalidateQueries({ queryKey: queryKeys.orders })
    },
    onError: (error: Error) => {
      throw error
    },
  })
}

export const useCancelOrder = () => {
  const queryClient = useQueryClient()
  
  return useMutation({
    mutationFn: (orderId: string) => orderService.cancel(orderId),
    onSuccess: (_, orderId) => {
      queryClient.invalidateQueries({ queryKey: queryKeys.order(orderId) })
      queryClient.invalidateQueries({ queryKey: queryKeys.orders })
      // 移除這裡的 toast，讓組件自己處理
    },
    onError: (error: Error) => {
      // 移除這裡的 toast，讓組件自己處理
      throw error
    },
  })
}

// 產品相關 hooks
export const useProducts = (params?: PageRequest) => {
  return useQuery({
    queryKey: [...queryKeys.products, params],
    queryFn: () => productService.list(params),
    staleTime: 10 * 60 * 1000, // 10 minutes
  })
}

export const useProduct = (productId: string) => {
  return useQuery({
    queryKey: queryKeys.product(productId),
    queryFn: () => productService.get(productId),
    enabled: !!productId,
  })
}

// 客戶相關 hooks
export const useCustomers = (params?: PageRequest) => {
  return useQuery({
    queryKey: [...queryKeys.customers, params],
    queryFn: () => customerService.list(params),
    staleTime: 5 * 60 * 1000,
  })
}

export const useCustomer = (customerId: string) => {
  return useQuery({
    queryKey: queryKeys.customer(customerId),
    queryFn: () => customerService.get(customerId),
    enabled: !!customerId,
  })
}

// 支付相關 hooks
export const useProcessPayment = () => {
  const queryClient = useQueryClient()
  
  return useMutation({
    mutationFn: (request: ProcessPaymentRequest) => paymentService.process(request),
    onSuccess: (payment) => {
      queryClient.invalidateQueries({ 
        queryKey: queryKeys.orderPayments(payment.orderId) 
      })
      queryClient.invalidateQueries({ 
        queryKey: queryKeys.order(payment.orderId) 
      })
    },
    onError: (error: Error) => {
      throw error
    },
  })
}

export const useOrderPayments = (orderId: string) => {
  return useQuery({
    queryKey: queryKeys.orderPayments(orderId),
    queryFn: () => paymentService.getByOrder(orderId),
    enabled: !!orderId,
  })
}

// 庫存相關 hooks
export const useInventory = (productId: string) => {
  return useQuery({
    queryKey: queryKeys.inventory(productId),
    queryFn: () => inventoryService.get(productId),
    enabled: !!productId,
    staleTime: 2 * 60 * 1000, // 2 minutes
  })
}

export const useCheckInventory = () => {
  return useMutation({
    mutationFn: ({ productId, quantity }: { productId: string; quantity: number }) =>
      inventoryService.check(productId, quantity),
  })
}

// 促銷相關 hooks
export const usePromotions = () => {
  return useQuery({
    queryKey: queryKeys.promotions,
    queryFn: () => promotionService.list(),
    staleTime: 5 * 60 * 1000,
  })
}

export const useActivePromotions = () => {
  return useQuery({
    queryKey: queryKeys.activePromotions,
    queryFn: () => promotionService.getActive(),
    staleTime: 2 * 60 * 1000,
  })
}

export const useApplyPromotion = () => {
  const queryClient = useQueryClient()
  
  return useMutation({
    mutationFn: ({ orderId, promotionId }: { orderId: string; promotionId: string }) =>
      promotionService.apply(orderId, promotionId),
    onSuccess: (_, { orderId }) => {
      queryClient.invalidateQueries({ queryKey: queryKeys.order(orderId) })
    },
    onError: (error: Error) => {
      throw error
    },
  })
}

// 統計相關 hooks
export const useStats = () => {
  return useQuery({
    queryKey: queryKeys.stats,
    queryFn: () => statsService.getStats(),
    staleTime: 5 * 60 * 1000, // 5 minutes
  })
}

export const useOrderStatusStats = () => {
  return useQuery({
    queryKey: queryKeys.orderStatusStats,
    queryFn: () => statsService.getOrderStatusStats(),
    staleTime: 5 * 60 * 1000,
  })
}

export const usePaymentMethodStats = () => {
  return useQuery({
    queryKey: queryKeys.paymentMethodStats,
    queryFn: () => statsService.getPaymentMethodStats(),
    staleTime: 5 * 60 * 1000,
  })
}

// 活動記錄相關 hooks
export const useActivities = (params?: { limit?: number }) => {
  return useQuery({
    queryKey: [...queryKeys.activities, params],
    queryFn: () => activityService.list(params),
    staleTime: 2 * 60 * 1000, // 2 minutes
  })
}
