'use client'

import React from 'react'
import { Navbar } from '@/components/layout/Navbar'
import { OrderList } from '@/components/order/OrderList'
import { StatsCard } from '@/components/dashboard/StatsCard'
import { useRouter } from 'next/navigation'
import { useStats, useOrderStatusStats } from '@/hooks/useApi'
import { 
  ShoppingCart, 
  Package, 
  Clock, 
  CheckCircle,
  XCircle,
  TrendingUp,
  Activity,
  Sparkles
} from 'lucide-react'

export default function OrdersPage() {
  const router = useRouter()
  const { data: statsData, isLoading: statsLoading } = useStats()
  const { data: orderStatusData, isLoading: statusLoading } = useOrderStatusStats()

  const handleCreateOrder = () => {
    router.push('/orders/new')
  }

  const handleViewOrder = (orderId: string) => {
    router.push(`/orders/${orderId}`)
  }

  const handleEditOrder = (orderId: string) => {
    router.push(`/orders/${orderId}/edit`)
  }

  // 計算訂單統計數據
  const orderStats = React.useMemo(() => {
    if (!statsData && !orderStatusData) {
      return [
        {
          title: '總訂單數',
          value: 0,
          change: { value: '載入中...', type: 'neutral' as const, period: '' },
          icon: ShoppingCart,
          color: 'blue' as const,
          loading: true
        },
        {
          title: '待處理',
          value: 0,
          change: { value: '載入中...', type: 'neutral' as const, period: '' },
          icon: Clock,
          color: 'orange' as const,
          loading: true
        },
        {
          title: '已完成',
          value: 0,
          change: { value: '載入中...', type: 'neutral' as const, period: '' },
          icon: CheckCircle,
          color: 'green' as const,
          loading: true
        },
        {
          title: '已取消',
          value: 0,
          change: { value: '載入中...', type: 'neutral' as const, period: '' },
          icon: XCircle,
          color: 'red' as const,
          loading: true
        }
      ]
    }

    const totalOrders = statsData?.totalOrders || 0
    const completedOrders = orderStatusData?.COMPLETED || 0
    const pendingOrders = (orderStatusData?.PENDING || 0) + (orderStatusData?.CREATED || 0)
    const cancelledOrders = orderStatusData?.CANCELLED || 0

    return [
      {
        title: '總訂單數',
        value: totalOrders,
        change: { value: '+12%', type: 'increase' as const, period: '比昨天' },
        icon: ShoppingCart,
        color: 'blue' as const,
        trend: [8, 12, 10, 15, 13, 18, totalOrders]
      },
      {
        title: '待處理',
        value: pendingOrders,
        change: { value: '+3', type: 'increase' as const, period: '今日新增' },
        icon: Clock,
        color: 'orange' as const,
        trend: [2, 4, 3, 6, 4, 7, pendingOrders]
      },
      {
        title: '已完成',
        value: completedOrders,
        change: { value: '+8%', type: 'increase' as const, period: '比昨天' },
        icon: CheckCircle,
        color: 'green' as const,
        trend: [5, 8, 7, 10, 9, 12, completedOrders]
      },
      {
        title: '已取消',
        value: cancelledOrders,
        change: { value: '-2%', type: 'decrease' as const, period: '比昨天' },
        icon: XCircle,
        color: 'red' as const,
        trend: [3, 2, 4, 1, 2, 1, cancelledOrders]
      }
    ]
  }, [statsData, orderStatusData])

  return (
    <div className="min-h-screen bg-gradient-to-br from-background via-background to-muted/20">
      {/* 導航欄 */}
      <Navbar />

      {/* 主要內容 */}
      <main className="container-modern py-8 space-y-8">
        {/* 頁面標題區塊 */}
        <div className="text-center space-y-4 animate-fade-in">
          <div className="inline-flex items-center space-x-2 bg-blue-500/10 text-blue-600 px-4 py-2 rounded-full text-sm font-medium border border-blue-500/20">
            <ShoppingCart className="h-4 w-4" />
            <span>訂單管理中心</span>
          </div>
          
          <h1 className="heading-modern text-gradient">
            訂單管理
          </h1>
          
          <p className="subheading-modern mx-auto">
            全面管理您的訂單流程，從創建到完成的每個環節都在您的掌控之中。
            實時追蹤訂單狀態，優化客戶體驗。
          </p>
        </div>

        {/* 訂單統計卡片 */}
        <div className="animate-slide-up">
          <h2 className="text-xl font-semibold text-foreground mb-6 flex items-center">
            <Activity className="h-5 w-5 mr-2 text-primary" />
            訂單統計
          </h2>
          <div className="grid-stats">
            {orderStats.map((stat, index) => (
              <div key={stat.title} className="animate-scale-in" style={{ animationDelay: `${index * 100}ms` }}>
                <StatsCard {...stat} loading={statsLoading || statusLoading} />
              </div>
            ))}
          </div>
        </div>

        {/* 訂單列表 */}
        <div className="animate-slide-up" style={{ animationDelay: '400ms' }}>
          <div className="modern-card p-0 overflow-hidden">
            <div className="p-6 border-b border-border/50 bg-gradient-to-r from-muted/30 to-muted/10">
              <h2 className="text-xl font-semibold text-foreground mb-2 flex items-center">
                <Package className="h-5 w-5 mr-2 text-primary" />
                訂單列表
              </h2>
              <p className="text-muted-foreground text-sm">
                管理和追蹤所有訂單，支持搜索、篩選和批量操作
              </p>
            </div>
            
            <div className="p-6">
              <OrderList
                onCreateOrder={handleCreateOrder}
                onViewOrder={handleViewOrder}
                onEditOrder={handleEditOrder}
              />
            </div>
          </div>
        </div>

        {/* 快速操作區 */}
        <div className="animate-slide-up" style={{ animationDelay: '600ms' }}>
          <div className="modern-card p-8 text-center space-y-4 bg-gradient-to-r from-blue-500/5 via-purple-500/5 to-blue-500/5">
            <div className="inline-flex items-center space-x-2 text-blue-600 mb-2">
              <Sparkles className="h-5 w-5" />
              <span className="font-medium">快速操作</span>
            </div>
            
            <h3 className="text-lg font-semibold text-foreground">
              需要處理訂單？
            </h3>
            <p className="text-muted-foreground">
              快速創建新訂單，或查看待處理的訂單列表
            </p>
            
            <div className="flex flex-col sm:flex-row gap-4 justify-center items-center">
              <button 
                onClick={handleCreateOrder}
                className="btn-primary"
              >
                <ShoppingCart className="h-4 w-4 mr-2" />
                創建新訂單
              </button>
              <button className="btn-secondary">
                <TrendingUp className="h-4 w-4 mr-2" />
                查看訂單報表
              </button>
            </div>
          </div>
        </div>
      </main>
    </div>
  )
}
