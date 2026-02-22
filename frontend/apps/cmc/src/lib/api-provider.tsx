"use client"

import { ApiServicesProvider, createApiClient } from "@repo/api-client"
import React from "react"

const apiClient = createApiClient({
    baseURL: process.env.NEXT_PUBLIC_API_URL || "http://localhost:8080/api",
    onUnauthorized: () => {
        if (typeof window !== "undefined") {
            localStorage.removeItem("auth_token")
            window.location.href = "/login"
        }
    },
    getAuthToken: () => {
        if (typeof window !== "undefined") {
            return localStorage.getItem("auth_token")
        }
        return null
    },
})

interface CmcApiProviderProps {
    children: React.ReactNode
}

export function CmcApiProvider({ children }: CmcApiProviderProps) {
    return (
        <ApiServicesProvider client={apiClient}>
            {children}
        </ApiServicesProvider>
    )
}
