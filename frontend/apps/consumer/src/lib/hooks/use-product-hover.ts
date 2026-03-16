'use client'

import { useEffect, useRef } from 'react'

interface ProductHoverOptions {
  effect?: 'lift' | 'glow'
  intensity?: 'subtle' | 'normal'
}

const STYLES = {
  lift: {
    subtle: { transform: 'translateY(-2px)', boxShadow: '0 4px 12px rgba(0,0,0,0.08)' },
    normal: { transform: 'translateY(-4px)', boxShadow: '0 8px 24px rgba(0,0,0,0.12)' },
  },
  glow: {
    subtle: { transform: 'none', boxShadow: '0 0 12px rgba(59,130,246,0.2)' },
    normal: { transform: 'none', boxShadow: '0 0 20px rgba(59,130,246,0.35)' },
  },
} as const

export function useProductHover<T extends HTMLElement = HTMLDivElement>(
  options: ProductHoverOptions = {}
) {
  const { effect = 'lift', intensity = 'normal' } = options
  const ref = useRef<T>(null)

  useEffect(() => {
    const el = ref.current
    if (!el || typeof window === 'undefined') return

    el.style.transition = 'transform 200ms ease, box-shadow 200ms ease'
    el.style.cursor = 'pointer'

    const hoverStyle = STYLES[effect][intensity]

    const onEnter = () => {
      el.style.transform = hoverStyle.transform
      el.style.boxShadow = hoverStyle.boxShadow
    }
    const onLeave = () => {
      el.style.transform = 'none'
      el.style.boxShadow = 'none'
    }

    el.addEventListener('mouseenter', onEnter)
    el.addEventListener('mouseleave', onLeave)

    return () => {
      el.removeEventListener('mouseenter', onEnter)
      el.removeEventListener('mouseleave', onLeave)
    }
  }, [effect, intensity])

  return ref
}
