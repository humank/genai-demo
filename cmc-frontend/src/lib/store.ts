import { create } from 'zustand'
import { devtools, persist } from 'zustand/middleware'
import { Order, Customer, Product } from '@/types/domain'

// 訂單狀態管理
interface OrderState {
  currentOrder: Order | null
  orders: Order[]
  setCurrentOrder: (order: Order | null) => void
  addOrder: (order: Order) => void
  updateOrder: (orderId: string, updates: Partial<Order>) => void
  removeOrder: (orderId: string) => void
  clearOrders: () => void
}

export const useOrderStore = create<OrderState>()(
  devtools(
    persist(
      (set, get) => ({
        currentOrder: null,
        orders: [],
        
        setCurrentOrder: (order) => 
          set({ currentOrder: order }, false, 'setCurrentOrder'),
        
        addOrder: (order) => 
          set(
            (state) => ({ orders: [...state.orders, order] }),
            false,
            'addOrder'
          ),
        
        updateOrder: (orderId, updates) =>
          set(
            (state) => ({
              orders: state.orders.map((order) =>
                order.id === orderId ? { ...order, ...updates } : order
              ),
              currentOrder:
                state.currentOrder?.id === orderId
                  ? { ...state.currentOrder, ...updates }
                  : state.currentOrder,
            }),
            false,
            'updateOrder'
          ),
        
        removeOrder: (orderId) =>
          set(
            (state) => ({
              orders: state.orders.filter((order) => order.id !== orderId),
              currentOrder:
                state.currentOrder?.id === orderId ? null : state.currentOrder,
            }),
            false,
            'removeOrder'
          ),
        
        clearOrders: () => 
          set({ orders: [], currentOrder: null }, false, 'clearOrders'),
      }),
      {
        name: 'order-store',
        partialize: (state) => ({ orders: state.orders }), // 只持久化 orders
      }
    ),
    { name: 'OrderStore' }
  )
)

// 購物車狀態管理
interface CartItem {
  product: Product
  quantity: number
}

interface CartState {
  items: CartItem[]
  addItem: (product: Product, quantity?: number) => void
  removeItem: (productId: string) => void
  updateQuantity: (productId: string, quantity: number) => void
  clearCart: () => void
  getTotalAmount: () => number
  getTotalItems: () => number
}

export const useCartStore = create<CartState>()(
  devtools(
    persist(
      (set, get) => ({
        items: [],
        
        addItem: (product, quantity = 1) =>
          set(
            (state) => {
              const existingItem = state.items.find(
                (item) => item.product.id === product.id
              )
              
              if (existingItem) {
                return {
                  items: state.items.map((item) =>
                    item.product.id === product.id
                      ? { ...item, quantity: item.quantity + quantity }
                      : item
                  ),
                }
              } else {
                return {
                  items: [...state.items, { product, quantity }],
                }
              }
            },
            false,
            'addItem'
          ),
        
        removeItem: (productId) =>
          set(
            (state) => ({
              items: state.items.filter((item) => item.product.id !== productId),
            }),
            false,
            'removeItem'
          ),
        
        updateQuantity: (productId, quantity) =>
          set(
            (state) => ({
              items: quantity <= 0 
                ? state.items.filter((item) => item.product.id !== productId)
                : state.items.map((item) =>
                    item.product.id === productId
                      ? { ...item, quantity }
                      : item
                  ),
            }),
            false,
            'updateQuantity'
          ),
        
        clearCart: () => set({ items: [] }, false, 'clearCart'),
        
        getTotalAmount: () => {
          const { items } = get()
          return items.reduce(
            (total, item) => total + item.product.price.amount * item.quantity,
            0
          )
        },
        
        getTotalItems: () => {
          const { items } = get()
          return items.reduce((total, item) => total + item.quantity, 0)
        },
      }),
      {
        name: 'cart-store',
      }
    ),
    { name: 'CartStore' }
  )
)

// 用戶狀態管理
interface UserState {
  currentUser: Customer | null
  isAuthenticated: boolean
  setCurrentUser: (user: Customer | null) => void
  logout: () => void
}

export const useUserStore = create<UserState>()(
  devtools(
    persist(
      (set) => ({
        currentUser: null,
        isAuthenticated: false,
        
        setCurrentUser: (user) =>
          set(
            { currentUser: user, isAuthenticated: !!user },
            false,
            'setCurrentUser'
          ),
        
        logout: () =>
          set(
            { currentUser: null, isAuthenticated: false },
            false,
            'logout'
          ),
      }),
      {
        name: 'user-store',
      }
    ),
    { name: 'UserStore' }
  )
)

// UI 狀態管理
interface UIState {
  sidebarOpen: boolean
  theme: 'light' | 'dark'
  loading: boolean
  setSidebarOpen: (open: boolean) => void
  setTheme: (theme: 'light' | 'dark') => void
  setLoading: (loading: boolean) => void
}

export const useUIStore = create<UIState>()(
  devtools(
    (set) => ({
      sidebarOpen: false,
      theme: 'light',
      loading: false,
      
      setSidebarOpen: (open) => 
        set({ sidebarOpen: open }, false, 'setSidebarOpen'),
      
      setTheme: (theme) => 
        set({ theme }, false, 'setTheme'),
      
      setLoading: (loading) => 
        set({ loading }, false, 'setLoading'),
    }),
    { name: 'UIStore' }
  )
)
