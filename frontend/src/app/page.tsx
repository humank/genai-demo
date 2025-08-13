'use client'

import React from 'react'
import { Navbar } from '@/components/layout/Navbar'
import { StatsCard } from '@/components/dashboard/StatsCard'
import { FeatureCard } from '@/components/dashboard/FeatureCard'
import { ActivityTimeline } from '@/components/dashboard/ActivityTimeline'
import { 
  ShoppingCart, 
  Package, 
  Users, 
  CreditCard, 
  Truck, 
  TrendingUp,
  DollarSign,
  AlertTriangle,
  Activity,
  Sparkles
} from 'lucide-react'

export default function HomePage() {
  // 模擬統計數據
  const stats = [
    {
      title: '今日訂單',
      value: 24,
      change: { value: '+12%', type: 'increase' as const, period: '比昨天' },
      icon: ShoppingCart,
      color: 'blue' as const,
      trend: [12, 19, 15, 22, 18, 24, 20]
    },
    {
      title: '今日營收',
      value: 'NT$ 45,231',
      change: { value: '+8%', type: 'increase' as const, period: '比昨天' },
      icon: DollarSign,
      color: 'green' as const,
      trend: [30000, 35000, 32000, 42000, 38000, 45231, 40000]
    },
    {
      title: '活躍客戶',
      value: 1234,
      change: { value: '+5%', type: 'increase' as const, period: '比上週' },
      icon: Users,
      color: 'purple' as const,
      trend: [1100, 1150, 1200, 1180, 1220, 1234, 1210]
    },
    {
      title: '庫存警告',
      value: 3,
      change: { value: '-2', type: 'decrease' as const, period: '比昨天' },
      icon: AlertTriangle,
      color: 'orange' as const,
      trend: [5, 4, 6, 3, 4, 3, 2]
    }
  ]

  // 功能模組配置
  const features = [
    {
      title: '訂單管理',
      description: '創建、查看和管理所有訂單，追蹤訂單狀態和處理流程',
      icon: ShoppingCart,
      href: '/orders',
      color: 'blue' as const,
      stats: { label: '今日新增', value: 24 },
      badge: { text: 'HOT', variant: 'hot' as const }
    },
    {
      title: '商品管理',
      description: '管理商品資訊、分類、庫存和定價策略',
      icon: Package,
      href: '/products',
      color: 'green' as const,
      stats: { label: '商品總數', value: 1567 }
    },
    {
      title: '客戶管理',
      description: '管理客戶資料、會員等級和購買記錄分析',
      icon: Users,
      href: '/customers',
      color: 'purple' as const,
      stats: { label: '活躍客戶', value: 1234 },
      badge: { text: 'NEW', variant: 'new' as const }
    },
    {
      title: '支付管理',
      description: '處理支付請求、查看支付狀態和財務報表',
      icon: CreditCard,
      href: '/payments',
      color: 'orange' as const,
      stats: { label: '今日收款', value: 'NT$ 45,231' }
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
            歡迎使用 GenAI Demo
          </h1>
          
          <p className="subheading-modern mx-auto">
            基於 DDD 和六角形架構的現代化電商平台，提供完整的訂單、庫存、支付和客戶管理功能。
            體驗最新的企業級架構設計和用戶界面。
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
                <StatsCard {...stat} />
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
          <ActivityTimeline />
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
