export const queryKeys = {
    // CMC keys
    orders: ["orders"] as const,
    order: (id: string) => ["orders", id] as const,
    products: ["products"] as const,
    product: (id: string) => ["products", id] as const,
    customers: ["customers"] as const,
    customer: (id: string) => ["customers", id] as const,
    payments: ["payments"] as const,
    payment: (id: string) => ["payments", id] as const,
    orderPayments: (orderId: string) => ["payments", "order", orderId] as const,
    inventory: (productId: string) => ["inventory", productId] as const,
    promotions: ["promotions"] as const,
    activePromotions: ["promotions", "active"] as const,
    stats: ["stats"] as const,
    orderStatusStats: ["stats", "order-status"] as const,
    paymentMethodStats: ["stats", "payment-methods"] as const,
    activities: ["activities"] as const,

    // Consumer keys
    cart: (customerId: string) => ["cart", customerId] as const,
    consumerProducts: ["consumer-products"] as const,
    productSearch: (keyword: string) => ["consumer-products", "search", keyword] as const,
    categories: ["consumer-products", "categories"] as const,
}
