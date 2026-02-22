import type { AxiosInstance } from "axios"
import { apiRequest, buildQueryParams } from "../client"
import type { PageRequest, PageResponse } from "../types/common"
import type { Product } from "../types/product"

export function createConsumerProductService(client: AxiosInstance) {
    return {
        list: (params?: PageRequest) =>
            apiRequest<PageResponse<Product>>(
                client,
                "GET",
                `/consumer/products${buildQueryParams(params)}`
            ),

        search: (keyword: string, params?: PageRequest) =>
            apiRequest<PageResponse<Product>>(
                client,
                "GET",
                `/consumer/products/search${buildQueryParams({ q: keyword, ...params })}`
            ),

        getCategories: () =>
            apiRequest<string[]>(client, "GET", "/consumer/products/categories"),
    }
}

export type ConsumerProductService = ReturnType<typeof createConsumerProductService>
