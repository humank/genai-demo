import axios, { type AxiosInstance, type AxiosResponse } from "axios"

export interface ApiClientConfig {
    baseURL: string
    onUnauthorized?: () => void
    getAuthToken?: () => string | null
}

export function createApiClient(config: ApiClientConfig): AxiosInstance {
    const client = axios.create({
        baseURL: config.baseURL,
        timeout: 10000,
        headers: { "Content-Type": "application/json" },
    })

    // Request interceptor: inject auth token
    client.interceptors.request.use(
        (reqConfig) => {
            const token = config.getAuthToken?.()
            if (token) {
                reqConfig.headers.Authorization = `Bearer ${token}`
            }
            return reqConfig
        },
        (error) => Promise.reject(error)
    )

    // Response interceptor: handle 401
    client.interceptors.response.use(
        (response) => response,
        (error) => {
            if (error.response?.status === 401) {
                config.onUnauthorized?.()
            }
            return Promise.reject(error)
        }
    )

    return client
}

/**
 * Extract data from API response, handling both { success, data } and { status, data } formats.
 */
export async function apiRequest<T>(
    client: AxiosInstance,
    method: "GET" | "POST" | "PUT" | "DELETE",
    url: string,
    data?: unknown
): Promise<T> {
    const response: AxiosResponse = await client.request({ method, url, data })
    const responseData = response.data

    const isSuccess = responseData.success === true || responseData.status === "success"

    if (isSuccess) {
        return (responseData.data ?? responseData) as T
    }

    const errorMessage =
        responseData.message || responseData.errors?.[0] || "API request failed"
    throw new Error(errorMessage)
}

/** Build URL query string from params object */
// eslint-disable-next-line @typescript-eslint/no-explicit-any
export function buildQueryParams(params?: Record<string, any>): string {
    if (!params) return ""
    const qs = new URLSearchParams()
    for (const [key, value] of Object.entries(params)) {
        if (value !== undefined && value !== null) {
            qs.append(key, String(value))
        }
    }
    const str = qs.toString()
    return str ? `?${str}` : ""
}
