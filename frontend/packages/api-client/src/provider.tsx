"use client"

import type { AxiosInstance } from "axios"
import * as React from "react"
import { createActivityService, type ActivityService } from "./services/activity.service"
import { createCartService, type CartService } from "./services/cart.service"
import { createConsumerProductService, type ConsumerProductService } from "./services/consumer-product.service"
import { createCustomerService, type CustomerService } from "./services/customer.service"
import { createInventoryService, type InventoryService } from "./services/inventory.service"
import { createOrderService, type OrderService } from "./services/order.service"
import { createPaymentService, type PaymentService } from "./services/payment.service"
import { createProductService, type ProductService } from "./services/product.service"
import { createPromotionService, type PromotionService } from "./services/promotion.service"
import { createStatsService, type StatsService } from "./services/stats.service"

export interface ApiServices {
    order: OrderService
    product: ProductService
    customer: CustomerService
    payment: PaymentService
    inventory: InventoryService
    promotion: PromotionService
    stats: StatsService
    cart: CartService
    consumerProduct: ConsumerProductService
    activity: ActivityService
}

const ApiServicesContext = React.createContext<ApiServices | null>(null)

export function useApiServices(): ApiServices {
    const ctx = React.useContext(ApiServicesContext)
    if (!ctx) {
        throw new Error("useApiServices must be used within an ApiServicesProvider")
    }
    return ctx
}

interface ApiServicesProviderProps {
    client: AxiosInstance
    children: React.ReactNode
}

export function ApiServicesProvider({ client, children }: ApiServicesProviderProps) {
    const services = React.useMemo<ApiServices>(
        () => ({
            order: createOrderService(client),
            product: createProductService(client),
            customer: createCustomerService(client),
            payment: createPaymentService(client),
            inventory: createInventoryService(client),
            promotion: createPromotionService(client),
            stats: createStatsService(client),
            cart: createCartService(client),
            consumerProduct: createConsumerProductService(client),
            activity: createActivityService(client),
        }),
        [client]
    )

    return (
        <ApiServicesContext.Provider value={services}>
            {children}
        </ApiServicesContext.Provider>
    )
}
