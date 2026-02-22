"use client"

import { Loader2 } from "lucide-react"
import * as React from "react"
import { cn } from "../lib/utils"

interface LoadingProps {
    size?: "sm" | "md" | "lg"
    text?: string
    className?: string
}

const sizeClasses = {
    sm: "h-4 w-4",
    md: "h-6 w-6",
    lg: "h-8 w-8",
}

const Loading: React.FC<LoadingProps> = ({ size = "md", text, className }) => {
    return (
        <div className={cn("flex items-center justify-center space-x-2", className)}>
            <Loader2 className={cn("animate-spin text-primary", sizeClasses[size])} />
            {text && <span className="text-sm text-muted-foreground">{text}</span>}
        </div>
    )
}

const LoadingPage: React.FC<{ text?: string }> = ({ text = "載入中..." }) => {
    return (
        <div className="flex items-center justify-center min-h-[400px]">
            <div className="text-center space-y-4">
                <Loading size="lg" />
                <p className="text-muted-foreground">{text}</p>
            </div>
        </div>
    )
}

const LoadingSkeleton: React.FC<{ className?: string }> = ({ className }) => {
    return <div className={cn("animate-pulse bg-muted rounded", className)} />
}

export { Loading, LoadingPage, LoadingSkeleton }
