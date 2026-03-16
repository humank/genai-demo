'use client'

import { useCallback, useEffect, useRef } from 'react'

const SCROLL_MILESTONES = [25, 50, 75, 90, 100]

function logBehavior(event: string, data?: Record<string, unknown>) {
  if (process.env.NODE_ENV === 'development') {
    console.log(`[UserBehavior] ${event}`, data ?? '')
  }
}

export function useUserBehavior() {
  const reachedMilestones = useRef(new Set<number>())
  const sessionStart = useRef(Date.now())

  useEffect(() => {
    if (typeof window === 'undefined') return

    // Scroll depth tracking
    const onScroll = () => {
      const scrollTop = window.scrollY
      const docHeight = document.documentElement.scrollHeight - window.innerHeight
      if (docHeight <= 0) return
      const pct = Math.round((scrollTop / docHeight) * 100)

      for (const milestone of SCROLL_MILESTONES) {
        if (pct >= milestone && !reachedMilestones.current.has(milestone)) {
          reachedMilestones.current.add(milestone)
          logBehavior('scroll_depth', { milestone })
        }
      }
    }

    // Visibility tracking
    const onVisibility = () => {
      logBehavior('visibility_change', { hidden: document.hidden })
    }

    window.addEventListener('scroll', onScroll, { passive: true })
    document.addEventListener('visibilitychange', onVisibility)

    return () => {
      window.removeEventListener('scroll', onScroll)
      document.removeEventListener('visibilitychange', onVisibility)
      const duration = Math.round((Date.now() - sessionStart.current) / 1000)
      logBehavior('session_duration', { seconds: duration })
    }
  }, [])

  const trackProductInteraction = useCallback(
    (productId: string, type: string, section: string) => {
      logBehavior('product_interaction', { productId, type, section })
    },
    []
  )

  const trackSearchBehavior = useCallback(
    (query: string, resultsCount: number) => {
      logBehavior('search', { query, resultsCount })
    },
    []
  )

  const trackConversionStep = useCallback(
    (step: string, metadata?: Record<string, unknown>) => {
      logBehavior('conversion_step', { step, ...metadata })
    },
    []
  )

  return { trackProductInteraction, trackSearchBehavior, trackConversionStep }
}
