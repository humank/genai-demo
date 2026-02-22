'use client'

import { AlertTriangle, RefreshCw } from 'lucide-react'
import { useEffect } from 'react'

export default function GlobalError({
  error,
  reset,
}: {
  error: Error & { digest?: string }
  reset: () => void
}) {
  useEffect(() => {
    console.error('全局錯誤:', error)
  }, [error])

  return (
    <html>
      <body>
        <div className="min-h-screen flex items-center justify-center bg-background">
          <div className="text-center space-y-6 p-8">
            <div className="flex justify-center">
              <AlertTriangle className="h-16 w-16 text-red-500" />
            </div>

            <div className="space-y-2">
              <h1 className="text-2xl font-bold">
                應用發生嚴重錯誤
              </h1>
              <p className="text-gray-600 max-w-md">
                應用遇到了嚴重錯誤。請重新載入頁面或聯繫技術支援。
              </p>
            </div>

            <div className="flex flex-col sm:flex-row gap-4 justify-center">
              <button onClick={reset} className="flex items-center gap-2 bg-primary text-primary-foreground px-4 py-2 rounded-md">
                <RefreshCw className="h-4 w-4" />
                重試
              </button>
              <button
                onClick={() => window.location.href = '/'}
                className="px-4 py-2 rounded-md border"
              >
                返回首頁
              </button>
            </div>
          </div>
        </div>
      </body>
    </html>
  )
}
