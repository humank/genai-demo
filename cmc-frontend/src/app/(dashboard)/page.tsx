'use client'

import { ActivityTimeline } from '@/components/dashboard/ActivityTimeline'
import { FeatureCard } from '@/components/dashboard/FeatureCard'
import { StatsCard } from '@/components/dashboard/StatsCard'
import { useActivities, useStats } from '@/hooks/useApi'
import {
  Activity,
  AlertTriangle,
  CreditCard,
  DollarSign,
  Package,
  ShoppingCart,
  TrendingUp,
  Truck,
  Users
} from 'lucide-react'
import React from 'react'

export default function HomePage() {
  const { data: statsData, isLoading: statsLoading, error: statsError } = useStats()
  const { data: activities, isLoading: activitiesLoading, error: activitiesError } = useActivities({ limit: 10 })

  const stats = React.useMemo(() => {
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
        color: 'blue' as const
      },
      {
        title: '完成訂單價值',
        value: statsData.totalCompletedOrderValue
          ? `NT$ ${Number(statsData.totalCompletedOrderValue).toLocaleString()}`
          : 'NT$ 0',
        change: { value: '+8%', type: 'increase' as const, period: '比昨天' },
        icon: DollarSign,
        color: 'green' as const
      },
      {
        title: '獨特客戶',
        value: statsData.uniqueCustomers || 0,
        change: { value: '+5%', type: 'increase' as const, period: '比上週' },
        icon: Users,
        color: 'purple' as const
      },
      {
        title: '庫存總量',
        value: statsData.totalAvailableInventory || 0,
        change: { value: '正常', type: 'neutral' as const, period: '庫存狀態' },
        icon: Package,
        color: 'orange' as const
      }
    ]
  }, [statsData])

  const features = [
    {
      title: '訂單管理',
      description: '創建、查看和管理所有訂單，追蹤訂單狀態和處理流程',
      icon: ShoppingCart,
      href: '/orders',
      color: 'blue' as const,
      stats: { label: '總訂單', value: statsData?.totalOrders || 0 }
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
      stats: { label: '獨特客戶', value: statsData?.uniqueCustomers || 0 }
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
      stats: { label: '活動進行中', value: 5 }
    }
  ]

  return (
    <div className="space-y-8">
      {/* 標題區塊 */}
      <div className="text-center space-y-4">
        <h1 className="text-4xl font-bold text-gray-900 dark:text-white">
          商務管理中心
        </h1>
        <p className="text-lg text-gray-600 dark:text-gray-400 max-w-2xl mx-auto">
          專業的電子商務管理平台，為您提供完整的訂單、商品、客戶和庫存管理解決方案
        </p>
      </div>

      {/* 統計卡片 */}
      <div>
        <h2 className="text-xl font-semibold text-gray-900 dark:text-white mb-6 flex items-center">
          <Activity className="h-5 w-5 mr-2 text-blue-600" />
          實時統計
        </h2>
        <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-4">
          {stats.map((stat, index) => (
            <div key={stat.title}>
              <StatsCard {...stat} loading={statsLoading} />
            </div>
          ))}
        </div>
      </div>

      {/* 功能模組 */}
      <div>
        <h2 className="text-xl font-semibold text-gray-900 dark:text-white mb-6 flex items-center">
          <Package className="h-5 w-5 mr-2 text-blue-600" />
          功能模組
        </h2>
        <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 gap-6">
          {features.map((feature, index) => (
            <div key={feature.title}>
              <FeatureCard {...feature} />
            </div>
          ))}
        </div>
      </div>

      {/* 最近活動 */}
      <div>
        <ActivityTimeline
          activities={activities || []}
          loading={activitiesLoading}
        />
      </div>

      {/* 快速操作區 */}
      <div className="bg-gray-50 dark:bg-gray-800/50 rounded-lg p-8 text-center space-y-4">
        <h3 className="text-lg font-semibold text-gray-900 dark:text-white">
          準備開始使用？
        </h3>
        <p className="text-gray-600 dark:text-gray-400">
          探索我們的功能模組，或查看詳細的系統文檔和 API 指南
        </p>
        <div className="flex flex-col sm:flex-row gap-4 justify-center items-center">
          <button className="bg-blue-600 text-white px-6 py-2 rounded-md hover:bg-blue-700 transition-colors flex items-center">
            <ShoppingCart className="h-4 w-4 mr-2" />
            創建第一個訂單
          </button>
          <button className="bg-white dark:bg-gray-800 text-gray-700 dark:text-gray-300 px-6 py-2 rounded-md border border-gray-300 dark:border-gray-700 hover:bg-gray-50 dark:hover:bg-gray-700 transition-colors flex items-center">
            <Package className="h-4 w-4 mr-2" />
            查看 API 文檔
          </button>
        </div>
      </div>
    </div>
  )
}