'use client'

import React from 'react'
import Link from 'next/link'
import { Card, CardContent, CardHeader } from '@/components/ui/card'
import { Button } from '@/components/ui/button'
import { ArrowRight, LucideIcon } from 'lucide-react'
import { cn } from '@/lib/utils'

interface FeatureCardProps {
  title: string
  description: string
  icon: LucideIcon
  href: string
  color?: 'blue' | 'green' | 'purple' | 'orange' | 'red' | 'indigo'
  stats?: {
    label: string
    value: string | number
  }
  badge?: {
    text: string
    variant: 'new' | 'hot' | 'updated'
  }
  disabled?: boolean
}

const colorVariants = {
  blue: {
    gradient: 'from-blue-500 to-blue-600',
    light: 'from-blue-50 to-blue-100/50',
    icon: 'bg-blue-500/10 text-blue-600 border-blue-500/20 group-hover:bg-blue-500 group-hover:text-white',
    border: 'group-hover:border-blue-500/30'
  },
  green: {
    gradient: 'from-green-500 to-green-600',
    light: 'from-green-50 to-green-100/50',
    icon: 'bg-green-500/10 text-green-600 border-green-500/20 group-hover:bg-green-500 group-hover:text-white',
    border: 'group-hover:border-green-500/30'
  },
  purple: {
    gradient: 'from-purple-500 to-purple-600',
    light: 'from-purple-50 to-purple-100/50',
    icon: 'bg-purple-500/10 text-purple-600 border-purple-500/20 group-hover:bg-purple-500 group-hover:text-white',
    border: 'group-hover:border-purple-500/30'
  },
  orange: {
    gradient: 'from-orange-500 to-orange-600',
    light: 'from-orange-50 to-orange-100/50',
    icon: 'bg-orange-500/10 text-orange-600 border-orange-500/20 group-hover:bg-orange-500 group-hover:text-white',
    border: 'group-hover:border-orange-500/30'
  },
  red: {
    gradient: 'from-red-500 to-red-600',
    light: 'from-red-50 to-red-100/50',
    icon: 'bg-red-500/10 text-red-600 border-red-500/20 group-hover:bg-red-500 group-hover:text-white',
    border: 'group-hover:border-red-500/30'
  },
  indigo: {
    gradient: 'from-indigo-500 to-indigo-600',
    light: 'from-indigo-50 to-indigo-100/50',
    icon: 'bg-indigo-500/10 text-indigo-600 border-indigo-500/20 group-hover:bg-indigo-500 group-hover:text-white',
    border: 'group-hover:border-indigo-500/30'
  }
}

const badgeVariants = {
  new: 'bg-green-500 text-white',
  hot: 'bg-red-500 text-white',
  updated: 'bg-blue-500 text-white'
}

export const FeatureCard: React.FC<FeatureCardProps> = ({
  title,
  description,
  icon: Icon,
  href,
  color = 'blue',
  stats,
  badge,
  disabled = false
}) => {
  const colors = colorVariants[color]

  const CardWrapper = disabled ? 'div' : Link

  return (
    <CardWrapper href={disabled ? undefined : href} className={disabled ? 'cursor-not-allowed' : ''}>
      <Card className={cn(
        "modern-card modern-card-hover group relative overflow-hidden transition-all duration-300",
        colors.border,
        disabled && "opacity-50 cursor-not-allowed"
      )}>
        {/* 背景漸變效果 */}
        <div className={cn(
          "absolute inset-0 opacity-0 group-hover:opacity-5 transition-opacity duration-300 bg-gradient-to-br",
          colors.gradient
        )} />
        
        {/* Badge */}
        {badge && (
          <div className="absolute top-4 right-4 z-10">
            <span className={cn(
              "px-2 py-1 text-xs font-medium rounded-full",
              badgeVariants[badge.variant]
            )}>
              {badge.text}
            </span>
          </div>
        )}

        <CardHeader className="pb-4">
          <div className="flex items-start justify-between">
            <div className={cn(
              "flex items-center justify-center w-12 h-12 rounded-xl border transition-all duration-300",
              colors.icon
            )}>
              <Icon className="h-6 w-6 transition-all duration-300" />
            </div>
            
            <ArrowRight className={cn(
              "h-5 w-5 text-muted-foreground group-hover:text-foreground transition-all duration-300 group-hover:translate-x-1",
              disabled && "hidden"
            )} />
          </div>
        </CardHeader>

        <CardContent className="pt-0">
          <div className="space-y-3">
            <div>
              <h3 className="text-lg font-semibold text-foreground group-hover:text-foreground transition-colors">
                {title}
              </h3>
              <p className="text-sm text-muted-foreground group-hover:text-muted-foreground/80 transition-colors mt-1">
                {description}
              </p>
            </div>
            
            {stats && (
              <div className="pt-2 border-t border-border/50">
                <div className="flex items-center justify-between">
                  <span className="text-xs text-muted-foreground">
                    {stats.label}
                  </span>
                  <span className="text-sm font-medium text-foreground">
                    {typeof stats.value === 'number' ? stats.value.toLocaleString() : stats.value}
                  </span>
                </div>
              </div>
            )}
            
            {disabled && (
              <div className="pt-2">
                <Button variant="outline" size="sm" disabled className="w-full">
                  即將推出
                </Button>
              </div>
            )}
          </div>
        </CardContent>
        
        {/* 裝飾性背景圖標 */}
        <div className="absolute bottom-0 right-0 w-24 h-24 opacity-[0.02] transform translate-x-6 translate-y-6">
          <Icon className="w-full h-full" />
        </div>
      </Card>
    </CardWrapper>
  )
}
