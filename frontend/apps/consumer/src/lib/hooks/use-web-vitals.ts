'use client'

import { useEffect } from 'react'

interface MetricThresholds {
  good: number
  poor: number
}

const THRESHOLDS: Record<string, MetricThresholds> = {
  LCP: { good: 2500, poor: 4000 },
  FID: { good: 100, poor: 300 },
  CLS: { good: 0.1, poor: 0.25 },
  TTFB: { good: 800, poor: 1800 },
  FCP: { good: 1800, poor: 3000 },
  INP: { good: 200, poor: 500 },
}

function getRating(name: string, value: number): 'good' | 'needs-improvement' | 'poor' {
  const t = THRESHOLDS[name]
  if (!t) return 'needs-improvement'
  if (value <= t.good) return 'good'
  if (value >= t.poor) return 'poor'
  return 'needs-improvement'
}

function logMetric(name: string, value: number) {
  if (process.env.NODE_ENV === 'development') {
    const rating = getRating(name, value)
    console.log(`[Web Vitals] ${name}: ${value.toFixed(2)} (${rating})`)
  }
}

export function useWebVitals(): void {
  useEffect(() => {
    if (typeof window === 'undefined' || !('PerformanceObserver' in window)) return

    const observers: PerformanceObserver[] = []

    try {
      // LCP
      const lcpObserver = new PerformanceObserver((list) => {
        const entries = list.getEntries()
        const last = entries[entries.length - 1]
        if (last) logMetric('LCP', last.startTime)
      })
      lcpObserver.observe({ type: 'largest-contentful-paint', buffered: true })
      observers.push(lcpObserver)
    } catch { /* unsupported */ }

    try {
      // FID
      const fidObserver = new PerformanceObserver((list) => {
        for (const entry of list.getEntries()) {
          const e = entry as PerformanceEventTiming
          logMetric('FID', e.processingStart - e.startTime)
        }
      })
      fidObserver.observe({ type: 'first-input', buffered: true })
      observers.push(fidObserver)
    } catch { /* unsupported */ }

    try {
      // CLS
      let clsValue = 0
      const clsObserver = new PerformanceObserver((list) => {
        for (const entry of list.getEntries()) {
          const e = entry as PerformanceEntry & { hadRecentInput?: boolean; value?: number }
          if (!e.hadRecentInput) {
            clsValue += e.value ?? 0
            logMetric('CLS', clsValue)
          }
        }
      })
      clsObserver.observe({ type: 'layout-shift', buffered: true })
      observers.push(clsObserver)
    } catch { /* unsupported */ }

    try {
      // FCP
      const fcpObserver = new PerformanceObserver((list) => {
        for (const entry of list.getEntries()) {
          if (entry.name === 'first-contentful-paint') {
            logMetric('FCP', entry.startTime)
          }
        }
      })
      fcpObserver.observe({ type: 'paint', buffered: true })
      observers.push(fcpObserver)
    } catch { /* unsupported */ }

    try {
      // INP
      const inpObserver = new PerformanceObserver((list) => {
        for (const entry of list.getEntries()) {
          const e = entry as PerformanceEventTiming
          const duration = e.duration
          if (duration > 0) logMetric('INP', duration)
        }
      })
      inpObserver.observe({ type: 'event', buffered: true })
      observers.push(inpObserver)
    } catch { /* unsupported */ }

    // TTFB
    try {
      const nav = performance.getEntriesByType('navigation')[0] as PerformanceNavigationTiming | undefined
      if (nav) logMetric('TTFB', nav.responseStart - nav.requestStart)
    } catch { /* unsupported */ }

    return () => {
      observers.forEach((o) => o.disconnect())
    }
  }, [])
}
