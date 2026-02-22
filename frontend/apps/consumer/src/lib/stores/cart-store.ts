import type { CartItem } from '@repo/api-client'
import { create } from 'zustand'

interface CartStore {
    items: CartItem[]
    itemCount: number
    setItems: (items: CartItem[]) => void
    addItem: (item: CartItem) => void
    updateQuantity: (productId: string, quantity: number) => void
    removeItem: (productId: string) => void
    clear: () => void
}

export const useCartStore = create<CartStore>((set) => ({
    items: [],
    itemCount: 0,
    setItems: (items) => set({
        items,
        itemCount: items.reduce((sum, item) => sum + item.quantity, 0),
    }),
    addItem: (item) => set((state) => {
        const existing = state.items.find((i) => i.productId === item.productId)
        const newItems = existing
            ? state.items.map((i) =>
                i.productId === item.productId
                    ? { ...i, quantity: i.quantity + item.quantity }
                    : i
            )
            : [...state.items, item]
        return {
            items: newItems,
            itemCount: newItems.reduce((sum, i) => sum + i.quantity, 0),
        }
    }),
    updateQuantity: (productId, quantity) => set((state) => {
        const newItems = state.items.map((i) =>
            i.productId === productId ? { ...i, quantity } : i
        )
        return {
            items: newItems,
            itemCount: newItems.reduce((sum, i) => sum + i.quantity, 0),
        }
    }),
    removeItem: (productId) => set((state) => {
        const newItems = state.items.filter((i) => i.productId !== productId)
        return {
            items: newItems,
            itemCount: newItems.reduce((sum, i) => sum + i.quantity, 0),
        }
    }),
    clear: () => set({ items: [], itemCount: 0 }),
}))
