'use client'

import { createContext, ReactNode, useContext } from 'react'
import { useErrorTracking } from '../hooks/use-error-tracking'
import { useUserBehavior } from '../hooks/use-user-behavior'
import { useWebVitals } from '../hooks/use-web-vitals'

interface ObservabilityContextValue {
  trackApiError: (url: string, method: string, status: number, message: string) => void
  trackUserOperationError: (operation: string, errorType: string, message: string) => void
  trackProductInteraction: (productId: string, type: string, section: string) => void
  trackSearchBehavior: (query: string, resultsCount: number) => void
  trackConversionStep: (step: string, metadata?: Record<string, unknown>) => void
}

const ObservabilityContext = createContext<ObservabilityContextValue | null>(null)

export function ObservabilityProvider({ children }: { children: ReactNode }) {
  useWebVitals()
  const { trackApiError, trackUserOperationError } = useErrorTracking()
  const { trackProductInteraction, trackSearchBehavior, trackConversionStep } =
    useUserBehavior()

  return (
    <ObservabilityContext.Provider
      value={{
        trackApiError,
        trackUserOperationError,
        trackProductInteraction,
        trackSearchBehavior,
        trackConversionStep,
      }}
    >
      {children}
    </ObservabilityContext.Provider>
  )
}

export function useObservability(): ObservabilityContextValue {
  const ctx = useContext(ObservabilityContext)
  if (!ctx) {
    throw new Error('useObservability must be used within ObservabilityProvider')
  }
  return ctx
}
