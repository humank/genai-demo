'use client'

import React from 'react'
import { Card, CardContent } from '@/components/ui/card'
import { cn } from '@/lib/utils'
import { LucideIcon } from 'lucide-react'

interface StatsCardProps {
  title: string
  value: string | number
  change?: {
    value: string
    type: 'increase' | 'decrease' | 'neutral' | 'warning'
    period: string
  }
  icon: LucideIcon
  color?: 'blue' | 'green' | 'orange' | 'purple' | 'red' | 'indigo'
  trend?: number[]
  loading?: boolean
}

const colorVariants = {
  blue: 'text-blue-600',
  green: 'text-green-600',
  orange: 'text-orange-600',
  purple: 'text-purple-600',
  red: 'text-red-600',
  indigo: 'text-indigo-600'
}

const changeTypeStyles = {
  increase: 'text-green-600',
  decrease: 'text-red-600',
  neutral: 'text-gray-600',
  warning: 'text-orange-600'
}

export const StatsCard: React.FC<StatsCardProps> = ({
  title,
  value,
  change,
  icon: Icon,
  color = 'blue',
  trend,
  loading = false
}) => {
  if (loading) {
    return (
      <Card className="bg-white border border-gray-200 rounded-lg shadow-sm">
        <CardContent className="p-6">
          <div className="animate-pulse">
            <div className="flex items-center justify-between">
              <div className="space-y-2">
                <div className="h-4 bg-gray-200 rounded w-20"></div>
                <div className="h-8 bg-gray-200 rounded w-16"></div>
                <div className="h-3 bg-gray-200 rounded w-24"></div>
              </div>
              <div className="h-12 w-12 bg-gray-200 rounded-lg"></div>
            </div>
          </div>
        </CardContent>
      </Card>
    )
  }

  return (
    <Card className="bg-white border border-gray-200 rounded-lg shadow-sm hover:shadow-md transition-shadow">
      <CardContent className="p-6">
        <div className="flex items-center justify-between">
          <div className="space-y-2">
            <p className="text-sm font-medium text-gray-600">
              {title}
            </p>
            
            <p className="text-3xl font-bold text-gray-900">
              {typeof value === 'number' ? value.toLocaleString() : value}
            </p>
            
            {change && (
              <div className="flex items-center space-x-2">
                <span className={cn(
                  "text-xs font-medium",
                  changeTypeStyles[change.type]
                )}>
                  {change.type === 'increase' && '↗ '}
                  {change.type === 'decrease' && '↘ '}
                  {change.type === 'neutral' && '→ '}
                  {change.value}
                </span>
                <span className="text-xs text-gray-500">
                  {change.period}
                </span>
              </div>
            )}
          </div>
          
          <div className={cn(
            "flex items-center justify-center w-12 h-12 rounded-lg bg-gray-100",
            colorVariants[color]
          )}>
            <Icon className="h-6 w-6" />
          </div>
        </div>
      </CardContent>
    </Card>
  )
}