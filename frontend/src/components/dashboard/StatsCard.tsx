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
    type: 'increase' | 'decrease' | 'neutral'
    period: string
  }
  icon: LucideIcon
  color?: 'blue' | 'green' | 'orange' | 'purple' | 'red' | 'indigo'
  trend?: number[]
  loading?: boolean
}

const colorVariants = {
  blue: {
    icon: 'bg-blue-500/10 text-blue-600 border-blue-500/20',
    gradient: 'from-blue-500/20 to-blue-600/5',
    accent: 'text-blue-600'
  },
  green: {
    icon: 'bg-green-500/10 text-green-600 border-green-500/20',
    gradient: 'from-green-500/20 to-green-600/5',
    accent: 'text-green-600'
  },
  orange: {
    icon: 'bg-orange-500/10 text-orange-600 border-orange-500/20',
    gradient: 'from-orange-500/20 to-orange-600/5',
    accent: 'text-orange-600'
  },
  purple: {
    icon: 'bg-purple-500/10 text-purple-600 border-purple-500/20',
    gradient: 'from-purple-500/20 to-purple-600/5',
    accent: 'text-purple-600'
  },
  red: {
    icon: 'bg-red-500/10 text-red-600 border-red-500/20',
    gradient: 'from-red-500/20 to-red-600/5',
    accent: 'text-red-600'
  },
  indigo: {
    icon: 'bg-indigo-500/10 text-indigo-600 border-indigo-500/20',
    gradient: 'from-indigo-500/20 to-indigo-600/5',
    accent: 'text-indigo-600'
  }
}

const changeTypeStyles = {
  increase: 'text-green-600 bg-green-50 border-green-200',
  decrease: 'text-red-600 bg-red-50 border-red-200',
  neutral: 'text-gray-600 bg-gray-50 border-gray-200'
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
  const colors = colorVariants[color]

  if (loading) {
    return (
      <Card className="modern-card animate-pulse">
        <CardContent className="p-6">
          <div className="flex items-center justify-between">
            <div className="space-y-2">
              <div className="h-4 bg-muted rounded w-20"></div>
              <div className="h-8 bg-muted rounded w-16"></div>
              <div className="h-3 bg-muted rounded w-24"></div>
            </div>
            <div className="h-12 w-12 bg-muted rounded-xl"></div>
          </div>
        </CardContent>
      </Card>
    )
  }

  return (
    <Card className={cn(
      "modern-card modern-card-hover relative overflow-hidden group",
      `bg-gradient-to-br ${colors.gradient}`
    )}>
      <CardContent className="p-6">
        <div className="flex items-center justify-between">
          <div className="space-y-2">
            {/* 標題 */}
            <p className="text-sm font-medium text-muted-foreground group-hover:text-foreground transition-colors">
              {title}
            </p>
            
            {/* 數值 */}
            <p className="text-3xl font-bold text-foreground">
              {typeof value === 'number' ? value.toLocaleString() : value}
            </p>
            
            {/* 變化指標 */}
            {change && (
              <div className="flex items-center space-x-2">
                <span className={cn(
                  "inline-flex items-center px-2 py-1 rounded-full text-xs font-medium border",
                  changeTypeStyles[change.type]
                )}>
                  {change.type === 'increase' && '↗'}
                  {change.type === 'decrease' && '↘'}
                  {change.type === 'neutral' && '→'}
                  <span className="ml-1">{change.value}</span>
                </span>
                <span className="text-xs text-muted-foreground">
                  {change.period}
                </span>
              </div>
            )}
          </div>
          
          {/* 圖標 */}
          <div className={cn(
            "flex items-center justify-center w-12 h-12 rounded-xl border transition-all duration-200 group-hover:scale-110",
            colors.icon
          )}>
            <Icon className="h-6 w-6" />
          </div>
        </div>
        
        {/* 趨勢線 (簡化版) */}
        {trend && trend.length > 0 && (
          <div className="mt-4 pt-4 border-t border-border/50">
            <div className="flex items-end space-x-1 h-8">
              {trend.map((value, index) => (
                <div
                  key={index}
                  className={cn(
                    "flex-1 rounded-sm transition-all duration-300 hover:opacity-80",
                    colors.accent.replace('text-', 'bg-').replace('600', '200')
                  )}
                  style={{
                    height: `${Math.max(4, (value / Math.max(...trend)) * 100)}%`
                  }}
                />
              ))}
            </div>
          </div>
        )}
      </CardContent>
      
      {/* 背景裝飾 */}
      <div className="absolute top-0 right-0 w-32 h-32 opacity-5 transform rotate-12 translate-x-8 -translate-y-8">
        <Icon className="w-full h-full" />
      </div>
    </Card>
  )
}
