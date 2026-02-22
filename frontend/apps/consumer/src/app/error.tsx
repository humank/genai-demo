'use client'

import { Button } from '@repo/ui'
import { AlertTriangle, Home, RefreshCw } from 'lucide-react'
import { useEffect } from 'react'

export default function Error({
    error,
    reset,
}: {
    error: Error & { digest?: string }
    reset: () => void
}) {
    useEffect(() => {
        console.error('應用錯誤:', error)
    }, [error])

    return (
        <div className="min-h-screen flex items-center justify-center bg-background">
            <div className="text-center space-y-6 p-8 max-w-md">
                <div className="flex justify-center">
                    <AlertTriangle className="h-16 w-16 text-red-500" aria-hidden="true" />
                </div>

                <div className="space-y-2">
                    <h1 className="text-2xl font-bold text-foreground">
                        出現了一些問題
                    </h1>
                    <p className="text-muted-foreground">
                        頁面載入時發生錯誤，請嘗試重新載入。如果問題持續存在，請稍後再試。
                    </p>
                </div>

                <div className="flex flex-col sm:flex-row gap-3 justify-center">
                    <Button onClick={reset} className="cursor-pointer flex items-center gap-2">
                        <RefreshCw className="h-4 w-4" aria-hidden="true" />
                        重試
                    </Button>
                    <Button
                        variant="outline"
                        className="cursor-pointer flex items-center gap-2"
                        onClick={() => (window.location.href = '/')}
                    >
                        <Home className="h-4 w-4" aria-hidden="true" />
                        返回首頁
                    </Button>
                </div>

                {process.env.NODE_ENV === 'development' && (
                    <details className="mt-8 text-left">
                        <summary className="cursor-pointer text-sm text-muted-foreground">
                            錯誤詳情（開發模式）
                        </summary>
                        <pre className="mt-2 p-4 bg-muted rounded text-xs overflow-auto">
                            {error.message}
                            {error.stack && `\n\n${error.stack}`}
                        </pre>
                    </details>
                )}
            </div>
        </div>
    )
}
