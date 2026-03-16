'use client'

import { useCallback, useEffect, useRef } from 'react'

interface TrackedError {
  type: 'runtime' | 'unhandled-rejection' | 'api' | 'user-operation'
  message: string
  timestamp: number
  meta?: Record<string, string | number>
}

const FLUSH_INTERVAL = 30_000

export function useErrorTracking() {
  const queueRef = useRef<TrackedError[]>([])

  const flush = useCallback(() => {
    if (queueRef.current.length === 0) return
    const batch = queueRef.current.splice(0)
    if (process.env.NODE_ENV === 'development') {
      console.log('[ErrorTracking] Flushing errors:', batch)
    }
    // In production, send batch to observability endpoint
  }, [])

  useEffect(() => {
    if (typeof window === 'undefined') return

    const onError = (event: ErrorEvent) => {
      queueRef.current.push({
        type: 'runtime',
        message: event.message || 'Unknown error',
        timestamp: Date.now(),
        meta: {
          filename: event.filename || '',
          lineno: event.lineno ?? 0,
          colno: event.colno ?? 0,
        },
      })
    }

    const onRejection = (event: PromiseRejectionEvent) => {
      queueRef.current.push({
        type: 'unhandled-rejection',
        message: String(event.reason),
        timestamp: Date.now(),
      })
    }

    window.addEventListener('error', onError)
    window.addEventListener('unhandledrejection', onRejection)
    const timer = setInterval(flush, FLUSH_INTERVAL)

    return () => {
      window.removeEventListener('error', onError)
      window.removeEventListener('unhandledrejection', onRejection)
      clearInterval(timer)
      flush()
    }
  }, [flush])

  const trackApiError = useCallback(
    (url: string, method: string, status: number, message: string) => {
      queueRef.current.push({
        type: 'api',
        message,
        timestamp: Date.now(),
        meta: { url, method, status },
      })
    },
    []
  )

  const trackUserOperationError = useCallback(
    (operation: string, errorType: string, message: string) => {
      queueRef.current.push({
        type: 'user-operation',
        message,
        timestamp: Date.now(),
        meta: { operation, errorType },
      })
    },
    []
  )

  return { trackApiError, trackUserOperationError }
}
