'use client'

import { ActivityTimeline } from '@/components/dashboard/ActivityTimeline'
import { FeatureCard } from '@/components/dashboard/FeatureCard'
import { StatsCard } from '@/components/dashboard/StatsCard'
import { Navbar } from '@/components/layout/Navbar'
import { useActivities, useStats } from '@/hooks/useApi'
import {
  Activity,
  AlertTriangle,
  CreditCard,
  DollarSign,
  Package,
  ShoppingCart,
  Sparkles,
  TrendingUp,
  Truck,
  Users
} from 'lucide-react'
import React from 'react'

export default function HomePage() {
  // 使用 API 獲取統計數據
  const { data: statsData, isLoading: statsLoading, error: statsError } = useStats()
  const { data: activities, isLoading: activitiesLoading, error: activitiesError } = useActivities({ limit: 10 })

  // 調試日誌
  React.useEffect(() => {
    console.log('HomePage 渲染:', { statsData, statsLoading, statsError, activities, activitiesLoading, activitiesError })
  }, [statsData, statsLoading, statsError, activities, activitiesLoading, activitiesError])

  // 從 API 數據計算統計
  const stats = React.useMemo(() => {
    console.log('計算統計數據:', statsData)
    if (!statsData) {
      return [
        {
          title: '今日訂單',
          value: 0,
          change: { value: '載入中...', type: 'neutral' as const, period: '' },
          icon: ShoppingCart,
          color: 'blue' as const,
          loading: true
        },
        {
          title: '今日營收',
          value: 'NT$ 0',
          change: { value: '載入中...', type: 'neutral' as const, period: '' },
          icon: DollarSign,
          color: 'green' as const,
          loading: true
        },
        {
          title: '活躍客戶',
          value: 0,
          change: { value: '載入中...', type: 'neutral' as const, period: '' },
          icon: Users,
          color: 'purple' as const,
          loading: true
        },
        {
          title: '庫存警告',
          value: 0,
          change: { value: '載入中...', type: 'neutral' as const, period: '' },
          icon: AlertTriangle,
          color: 'orange' as const,
          loading: true
        }
      ]
    }

    return [
      {
        title: '總訂單數',
        value: statsData.totalOrders || 0,
        change: { value: '+12%', type: 'increase' as const, period: '比昨天' },
        icon: ShoppingCart,
        color: 'blue' as const,
        trend: [12, 19, 15, 22, 18, 24, 20]
      },
      {
        title: '完成訂單價值',
        value: statsData.totalCompletedOrderValue
          ? `NT$ ${Number(statsData.totalCompletedOrderValue).toLocaleString()}`
          : 'NT$ 0',
        change: { value: '+8%', type: 'increase' as const, period: '比昨天' },
        icon: DollarSign,
        color: 'green' as const,
        trend: [30000, 35000, 32000, 42000, 38000, 45231, 40000]
      },
      {
        title: '獨特客戶',
        value: statsData.uniqueCustomers || 0,
        change: { value: '+5%', type: 'increase' as const, period: '比上週' },
        icon: Users,
        color: 'purple' as const,
        trend: [1100, 1150, 1200, 1180, 1220, 1234, 1210]
      },
      {
        title: '庫存總量',
        value: statsData.totalAvailableInventory || 0,
        change: { value: '正常', type: 'neutral' as const, period: '庫存狀態' },
        icon: Package,
        color: 'orange' as const,
        trend: [5, 4, 6, 3, 4, 3, 2]
      }
    ]
  }, [statsData])

  // 功能模組配置
  const features = [
    {
      title: '訂單管理',
      description: '創建、查看和管理所有訂單，追蹤訂單狀態和處理流程',
      icon: ShoppingCart,
      href: '/orders',
      color: 'blue' as const,
      stats: { label: '總訂單', value: statsData?.totalOrders || 0 },
      badge: { text: 'HOT', variant: 'hot' as const }
    },
    {
      title: '商品管理',
      description: '管理商品資訊、分類、庫存和定價策略',
      icon: Package,
      href: '/products',
      color: 'green' as const,
      stats: { label: '庫存項目', value: statsData?.totalInventories || 0 }
    },
    {
      title: '客戶管理',
      description: '管理客戶資料、會員等級和購買記錄分析',
      icon: Users,
      href: '/customers',
      color: 'purple' as const,
      stats: { label: '獨特客戶', value: statsData?.uniqueCustomers || 0 },
      badge: { text: 'NEW', variant: 'new' as const }
    },
    {
      title: '支付管理',
      description: '處理支付請求、查看支付狀態和財務報表',
      icon: CreditCard,
      href: '/payments',
      color: 'orange' as const,
      stats: { label: '支付記錄', value: statsData?.totalPayments || 0 }
    },
    {
      title: '物流配送',
      description: '管理配送安排、追蹤配送狀態和物流優化',
      icon: Truck,
      href: '/delivery',
      color: 'indigo' as const,
      stats: { label: '配送中', value: 18 }
    },
    {
      title: '促銷活動',
      description: '創建和管理各種促銷活動、優惠券和行銷策略',
      icon: TrendingUp,
      href: '/promotions',
      color: 'red' as const,
      stats: { label: '活動進行中', value: 5 },
      badge: { text: 'UPDATED', variant: 'updated' as const }
    }
  ]

  return (
    <div className="min-h-screen bg-gradient-to-br from-background via-background to-muted/20">
      {/* 導航欄 */}
      <Navbar />

      {/* 主要內容 */}
      <main className="container-modern py-8 space-y-8">
        {/* 歡迎區塊 */}
        <div className="text-center space-y-4 animate-fade-in">
          <div className="inline-flex items-center space-x-2 bg-primary/10 text-primary px-4 py-2 rounded-full text-sm font-medium border border-primary/20">
            <Sparkles className="h-4 w-4" />
            <span>現代化電商管理平台</span>
          </div>

          <h1 className="heading-modern text-gradient">
            歡迎使用商務管理中心
          </h1>

          <p className="subheading-modern mx-auto">
            專業的電子商務管理平台，為您提供完整的訂單、商品、客戶和庫存管理解決方案。
            體驗現代化的企業級管理系統，提升您的商務運營效率。
          </p>
        </div>

        {/* 統計卡片 */}
        <div className="animate-slide-up">
          <h2 className="text-xl font-semibold text-foreground mb-6 flex items-center">
            <Activity className="h-5 w-5 mr-2 text-primary" />
            實時統計
          </h2>
          <div className="grid-stats">
            {stats.map((stat, index) => (
              <div key={stat.title} className="animate-scale-in" style={{ animationDelay: `${index * 100}ms` }}>
                <StatsCard {...stat} loading={statsLoading} />
              </div>
            ))}
          </div>
        </div>

        {/* 功能模組 */}
        <div className="animate-slide-up" style={{ animationDelay: '200ms' }}>
          <h2 className="text-xl font-semibold text-foreground mb-6 flex items-center">
            <Package className="h-5 w-5 mr-2 text-primary" />
            功能模組
          </h2>
          <div className="grid-modern">
            {features.map((feature, index) => (
              <div key={feature.title} className="animate-scale-in" style={{ animationDelay: `${(index + 4) * 100}ms` }}>
                <FeatureCard {...feature} />
              </div>
            ))}
          </div>
        </div>

        {/* 最近活動 */}
        <div className="animate-slide-up" style={{ animationDelay: '400ms' }}>
          <ActivityTimeline
            activities={activities || []}
            loading={activitiesLoading}
          />
        </div>

        {/* 快速操作區 */}
        <div className="animate-slide-up" style={{ animationDelay: '500ms' }}>
          <div className="modern-card p-8 text-center space-y-4 bg-gradient-to-r from-primary/5 via-accent/5 to-primary/5">
            <h3 className="text-lg font-semibold text-foreground">
              準備開始使用？
            </h3>
            <p className="text-muted-foreground">
              探索我們的功能模組，或查看詳細的系統文檔和 API 指南
            </p>
            <div className="flex flex-col sm:flex-row gap-4 justify-center items-center">
              <button className="btn-primary">
                <ShoppingCart className="h-4 w-4 mr-2" />
                創建第一個訂單
              </button>
              <button className="btn-secondary">
                <Package className="h-4 w-4 mr-2" />
                查看 API 文檔
              </button>
            </div>
          </div>
        </div>
      </main>
    </div>
  )
}
