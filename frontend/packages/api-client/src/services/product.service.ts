import type { AxiosInstance } from "axios"
import { apiRequest, buildQueryParams } from "../client"
import type { PageRequest, PageResponse } from "../types/common"
import type { Product } from "../types/product"

export function createProductService(client: AxiosInstance) {
    return {
        list: (params?: PageRequest) =>
            apiRequest<PageResponse<Product>>(client, "GET", `/products${buildQueryParams(params)}`),

        get: (productId: string) =>
            apiRequest<Product>(client, "GET", `/products/${productId}`),

        update: (productId: string, product: Partial<Product>) =>
            apiRequest<Product>(client, "PUT", `/products/${productId}`, product),

        delete: (productId: string) =>
            apiRequest<void>(client, "DELETE", `/products/${productId}`),
    }
}

export type ProductService = ReturnType<typeof createProductService>
