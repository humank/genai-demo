'use client'

import React from 'react'
import Link from 'next/link'
import { Card, CardContent, CardHeader } from '@/components/ui/card'
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
  blue: 'text-blue-600',
  green: 'text-green-600',
  purple: 'text-purple-600',
  orange: 'text-orange-600',
  red: 'text-red-600',
  indigo: 'text-indigo-600'
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
  const CardWrapper = ({ children, className }: { children: React.ReactNode; className?: string }) => {
    if (disabled) {
      return <div className={cn("cursor-not-allowed", className)}>{children}</div>
    }
    return <Link href={href} className={className}>{children}</Link>
  }

  return (
    <CardWrapper>
      <Card className={cn(
        "bg-white border border-gray-200 rounded-lg shadow-sm hover:shadow-md transition-all duration-200 group relative",
        disabled && "opacity-50 cursor-not-allowed"
      )}>
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
              "flex items-center justify-center w-12 h-12 rounded-lg bg-gray-100",
              colorVariants[color]
            )}>
              <Icon className="h-6 w-6" />
            </div>
            
            {!disabled && (
              <ArrowRight className="h-5 w-5 text-gray-400 group-hover:text-gray-600 transition-colors group-hover:translate-x-1" />
            )}
          </div>
        </CardHeader>

        <CardContent className="pt-0">
          <div className="space-y-3">
            <div>
              <h3 className="text-lg font-semibold text-gray-900">
                {title}
              </h3>
              <p className="text-sm text-gray-600 mt-1">
                {description}
              </p>
            </div>
            
            {stats && (
              <div className="pt-2 border-t border-gray-200">
                <div className="flex items-center justify-between">
                  <span className="text-xs text-gray-500">
                    {stats.label}
                  </span>
                  <span className="text-sm font-medium text-gray-900">
                    {typeof stats.value === 'number' ? stats.value.toLocaleString() : stats.value}
                  </span>
                </div>
              </div>
            )}
          </div>
        </CardContent>
      </Card>
    </CardWrapper>
  )
}