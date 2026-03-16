'use client'

import { useEffect, useRef } from 'react'

interface ImageZoomOptions {
  zoomFactor?: number
}

export function useImageZoom<T extends HTMLElement = HTMLDivElement>(
  options: ImageZoomOptions = {}
) {
  const { zoomFactor = 1.15 } = options
  const ref = useRef<T>(null)

  useEffect(() => {
    const el = ref.current
    if (!el || typeof window === 'undefined') return

    const prefersReduced = window.matchMedia('(prefers-reduced-motion: reduce)').matches
    if (prefersReduced) return

    el.style.transition = 'transform 300ms ease'
    el.style.willChange = 'transform'

    const onEnter = () => {
      el.style.transform = `scale(${zoomFactor})`
    }
    const onLeave = () => {
      el.style.transform = 'scale(1)'
    }

    el.addEventListener('mouseenter', onEnter)
    el.addEventListener('mouseleave', onLeave)

    return () => {
      el.removeEventListener('mouseenter', onEnter)
      el.removeEventListener('mouseleave', onLeave)
    }
  }, [zoomFactor])

  return ref
}
