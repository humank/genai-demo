'use client'

import React from 'react'
import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card'
import { Button } from '@/components/ui/button'
import { Bell, MoreHorizontal } from 'lucide-react'
import { cn } from '@/lib/utils'

interface ActivityItem {
  id: string
  type: 'order' | 'payment' | 'inventory' | 'customer' | 'system'
  title: string
  description?: string
  timestamp: string
  status: 'success' | 'warning' | 'error' | 'info'
  metadata?: {
    orderId?: string
    customerId?: string
    amount?: number
  }
}

interface ActivityTimelineProps {
  activities?: ActivityItem[]
  showAll?: boolean
  onViewAll?: () => void
  loading?: boolean
}

const statusConfig = {
  success: 'bg-green-500',
  warning: 'bg-yellow-500',
  error: 'bg-red-500',
  info: 'bg-blue-500'
}

export const ActivityTimeline: React.FC<ActivityTimelineProps> = ({
  activities = [],
  showAll = false,
  onViewAll,
  loading = false
}) => {
  const displayActivities = showAll ? activities : activities.slice(0, 5)

  if (loading) {
    return (
      <Card className="bg-white border border-gray-200 rounded-lg shadow-sm">
        <CardHeader>
          <CardTitle className="flex items-center">
            <div className="h-5 w-5 bg-gray-200 rounded mr-2 animate-pulse"></div>
            <div className="h-6 bg-gray-200 rounded w-24 animate-pulse"></div>
          </CardTitle>
        </CardHeader>
        <CardContent>
          <div className="space-y-4">
            {Array.from({ length: 4 }).map((_, index) => (
              <div key={index} className="flex items-start space-x-4">
                <div className="w-3 h-3 bg-gray-200 rounded-full mt-2 animate-pulse"></div>
                <div className="flex-1 space-y-2">
                  <div className="h-4 bg-gray-200 rounded w-3/4 animate-pulse"></div>
                  <div className="h-3 bg-gray-200 rounded w-1/2 animate-pulse"></div>
                </div>
              </div>
            ))}
          </div>
        </CardContent>
      </Card>
    )
  }

  return (
    <Card className="bg-white border border-gray-200 rounded-lg shadow-sm">
      <CardHeader className="pb-4">
        <div className="flex items-center justify-between">
          <CardTitle className="flex items-center text-lg">
            <Bell className="h-5 w-5 mr-2 text-blue-600" />
            最近活動
          </CardTitle>
          <Button variant="ghost" size="sm">
            <MoreHorizontal className="h-4 w-4" />
          </Button>
        </div>
      </CardHeader>
      
      <CardContent className="pt-0">
        <div className="space-y-4">
          {displayActivities.map((activity, index) => {
            const isLast = index === displayActivities.length - 1
            
            return (
              <div key={activity.id} className="relative">
                <div className="flex items-start space-x-4">
                  <div className="relative flex-shrink-0">
                    <div className={cn(
                      "w-3 h-3 rounded-full",
                      statusConfig[activity.status]
                    )} />
                    {!isLast && (
                      <div className="absolute top-3 left-1/2 w-0.5 h-8 bg-gray-200 transform -translate-x-1/2" />
                    )}
                  </div>
                  
                  <div className="flex-1 min-w-0 pb-4">
                    <div className="flex items-start justify-between">
                      <div className="flex-1">
                        <p className="text-sm font-medium text-gray-900">
                          {activity.title}
                        </p>
                        
                        {activity.description && (
                          <p className="text-sm text-gray-600 mt-1">
                            {activity.description}
                          </p>
                        )}
                        
                        {activity.metadata && (
                          <div className="flex items-center space-x-4 mt-2 text-xs text-gray-500">
                            {activity.metadata.orderId && (
                              <span>訂單: {activity.metadata.orderId}</span>
                            )}
                            {activity.metadata.customerId && (
                              <span>客戶: {activity.metadata.customerId}</span>
                            )}
                            {activity.metadata.amount && (
                              <span>金額: NT$ {activity.metadata.amount.toLocaleString()}</span>
                            )}
                          </div>
                        )}
                      </div>
                      
                      <span className="text-xs text-gray-500 whitespace-nowrap ml-4">
                        {activity.timestamp}
                      </span>
                    </div>
                  </div>
                </div>
              </div>
            )
          })}
        </div>
        
        {!showAll && activities.length > 5 && onViewAll && (
          <div className="pt-4 border-t border-gray-200">
            <Button 
              variant="outline" 
              size="sm" 
              onClick={onViewAll}
              className="w-full"
            >
              查看全部活動 ({activities.length})
            </Button>
          </div>
        )}
        
        {displayActivities.length === 0 && (
          <div className="text-center py-8">
            <Bell className="h-12 w-12 text-gray-300 mx-auto mb-4" />
            <p className="text-sm text-gray-500">暫無活動記錄</p>
          </div>
        )}
      </CardContent>
    </Card>
  )
}