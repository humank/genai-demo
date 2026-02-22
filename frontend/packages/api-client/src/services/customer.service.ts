import type { AxiosInstance } from "axios"
import { apiRequest, buildQueryParams } from "../client"
import type { PageRequest, PageResponse } from "../types/common"
import type { Customer } from "../types/customer"

export function createCustomerService(client: AxiosInstance) {
    return {
        get: (customerId: string) =>
            apiRequest<Customer>(client, "GET", `/customers/${customerId}`),

        list: (params?: PageRequest) =>
            apiRequest<PageResponse<Customer>>(client, "GET", `/customers${buildQueryParams(params)}`),
    }
}

export type CustomerService = ReturnType<typeof createCustomerService>
