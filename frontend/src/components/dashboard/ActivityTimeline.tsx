'use client'

import React from 'react'
import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card'
import { Button } from '@/components/ui/button'
import { Bell, ExternalLink, MoreHorizontal } from 'lucide-react'
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
  activities: ActivityItem[]
  showAll?: boolean
  onViewAll?: () => void
  loading?: boolean
}

const activityTypeConfig = {
  order: {
    color: 'bg-blue-500',
    lightColor: 'bg-blue-50 border-blue-200',
    textColor: 'text-blue-700'
  },
  payment: {
    color: 'bg-green-500',
    lightColor: 'bg-green-50 border-green-200',
    textColor: 'text-green-700'
  },
  inventory: {
    color: 'bg-orange-500',
    lightColor: 'bg-orange-50 border-orange-200',
    textColor: 'text-orange-700'
  },
  customer: {
    color: 'bg-purple-500',
    lightColor: 'bg-purple-50 border-purple-200',
    textColor: 'text-purple-700'
  },
  system: {
    color: 'bg-gray-500',
    lightColor: 'bg-gray-50 border-gray-200',
    textColor: 'text-gray-700'
  }
}

const statusConfig = {
  success: 'bg-green-500',
  warning: 'bg-yellow-500',
  error: 'bg-red-500',
  info: 'bg-blue-500'
}

// 模擬數據
const mockActivities: ActivityItem[] = [
  {
    id: '1',
    type: 'order',
    title: '新訂單已創建',
    description: '客戶張小明創建了新訂單',
    timestamp: '2 分鐘前',
    status: 'success',
    metadata: { orderId: 'ORD-001', customerId: 'CUST-123' }
  },
  {
    id: '2',
    type: 'payment',
    title: '支付處理完成',
    description: '訂單 #ORD-001 支付成功',
    timestamp: '5 分鐘前',
    status: 'success',
    metadata: { orderId: 'ORD-001', amount: 1299 }
  },
  {
    id: '3',
    type: 'inventory',
    title: '庫存警告',
    description: '商品 "iPhone 15" 庫存不足',
    timestamp: '10 分鐘前',
    status: 'warning'
  },
  {
    id: '4',
    type: 'customer',
    title: '新客戶註冊',
    description: '李小華完成註冊',
    timestamp: '15 分鐘前',
    status: 'info',
    metadata: { customerId: 'CUST-124' }
  },
  {
    id: '5',
    type: 'system',
    title: '系統維護完成',
    description: '定期系統維護已完成',
    timestamp: '30 分鐘前',
    status: 'success'
  }
]

export const ActivityTimeline: React.FC<ActivityTimelineProps> = ({
  activities = mockActivities,
  showAll = false,
  onViewAll,
  loading = false
}) => {
  const displayActivities = showAll ? activities : activities.slice(0, 5)

  if (loading) {
    return (
      <Card className="modern-card">
        <CardHeader>
          <CardTitle className="flex items-center animate-pulse">
            <div className="h-5 w-5 bg-muted rounded mr-2"></div>
            <div className="h-6 bg-muted rounded w-24"></div>
          </CardTitle>
        </CardHeader>
        <CardContent>
          <div className="space-y-4">
            {Array.from({ length: 4 }).map((_, index) => (
              <div key={index} className="flex items-start space-x-4 animate-pulse">
                <div className="w-3 h-3 bg-muted rounded-full mt-2"></div>
                <div className="flex-1 space-y-2">
                  <div className="h-4 bg-muted rounded w-3/4"></div>
                  <div className="h-3 bg-muted rounded w-1/2"></div>
                </div>
              </div>
            ))}
          </div>
        </CardContent>
      </Card>
    )
  }

  return (
    <Card className="modern-card">
      <CardHeader className="pb-4">
        <div className="flex items-center justify-between">
          <CardTitle className="flex items-center text-lg">
            <Bell className="h-5 w-5 mr-2 text-primary" />
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
            const typeConfig = activityTypeConfig[activity.type]
            const isLast = index === displayActivities.length - 1
            
            return (
              <div key={activity.id} className="relative group">
                {/* 時間線 */}
                <div className="flex items-start space-x-4">
                  {/* 狀態點 */}
                  <div className="relative flex-shrink-0">
                    <div className={cn(
                      "w-3 h-3 rounded-full border-2 border-background transition-all duration-200 group-hover:scale-125",
                      statusConfig[activity.status]
                    )} />
                    {!isLast && (
                      <div className="absolute top-3 left-1/2 w-0.5 h-8 bg-border transform -translate-x-1/2" />
                    )}
                  </div>
                  
                  {/* 內容 */}
                  <div className="flex-1 min-w-0 pb-4">
                    <div className="flex items-start justify-between">
                      <div className="flex-1">
                        <div className="flex items-center space-x-2">
                          <p className="text-sm font-medium text-foreground group-hover:text-primary transition-colors">
                            {activity.title}
                          </p>
                          <span className={cn(
                            "inline-flex items-center px-2 py-0.5 rounded-full text-xs font-medium border",
                            typeConfig.lightColor,
                            typeConfig.textColor
                          )}>
                            {activity.type}
                          </span>
                        </div>
                        
                        {activity.description && (
                          <p className="text-sm text-muted-foreground mt-1">
                            {activity.description}
                          </p>
                        )}
                        
                        {activity.metadata && (
                          <div className="flex items-center space-x-4 mt-2 text-xs text-muted-foreground">
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
                      
                      <div className="flex items-center space-x-2 ml-4">
                        <span className="text-xs text-muted-foreground whitespace-nowrap">
                          {activity.timestamp}
                        </span>
                        <Button variant="ghost" size="sm" className="opacity-0 group-hover:opacity-100 transition-opacity">
                          <ExternalLink className="h-3 w-3" />
                        </Button>
                      </div>
                    </div>
                  </div>
                </div>
              </div>
            )
          })}
        </div>
        
        {!showAll && activities.length > 5 && onViewAll && (
          <div className="pt-4 border-t border-border/50">
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
            <Bell className="h-12 w-12 text-muted-foreground/30 mx-auto mb-4" />
            <p className="text-sm text-muted-foreground">暫無活動記錄</p>
          </div>
        )}
      </CardContent>
    </Card>
  )
}
