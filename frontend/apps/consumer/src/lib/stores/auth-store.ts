import { create } from 'zustand'

interface AuthStore {
    customerId: string | null
    isAuthenticated: boolean
    setCustomerId: (id: string) => void
    logout: () => void
}

export const useAuthStore = create<AuthStore>((set) => ({
    customerId: typeof window !== 'undefined'
        ? localStorage.getItem('consumer_customer_id')
        : null,
    isAuthenticated: typeof window !== 'undefined'
        ? !!localStorage.getItem('consumer_customer_id')
        : false,
    setCustomerId: (id) => {
        if (typeof window !== 'undefined') {
            localStorage.setItem('consumer_customer_id', id)
        }
        set({ customerId: id, isAuthenticated: true })
    },
    logout: () => {
        if (typeof window !== 'undefined') {
            localStorage.removeItem('consumer_customer_id')
        }
        set({ customerId: null, isAuthenticated: false })
    },
}))
