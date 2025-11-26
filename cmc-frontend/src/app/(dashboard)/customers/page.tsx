'use client'

import React, { useState } from 'react'

import { CustomerCard } from '@/components/customer/CustomerCard'
import { StatsCard } from '@/components/dashboard/StatsCard'
import { Button } from '@/components/ui/button'
import { Input } from '@/components/ui/input'
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from '@/components/ui/select'
import { EmptyState } from '@/components/ui/empty-state'
import { useCustomers, useStats } from '@/hooks/useApi'
import { Customer } from '@/types/domain'
import {
  Users,
  Plus,
  Search,
  Filter,
  RefreshCw,
  UserPlus,
  Crown,
  TrendingUp,
  Calendar
} from 'lucide-react'

export default function CustomersPage() {
  const [searchTerm, setSearchTerm] = useState('')
  const [membershipFilter, setMembershipFilter] = useState<string>('ALL')
  const [currentPage, setCurrentPage] = useState(0)
  const pageSize = 20

  // 使用 API 獲取客戶數據
  const { data: customersResponse, isLoading, error, refetch } = useCustomers({
    page: currentPage,
    size: pageSize,
  })

  // 獲取統計數據
  const { data: statsData } = useStats()

  const customers = customersResponse?.content || []

  // 統計數據
  const totalCustomers = customersResponse?.totalElements || 0
  const activeCustomers = Math.floor(totalCustomers * 0.8) // 模擬活躍客戶比例
  const vipCustomers = customers.filter(c => ['Gold', 'Platinum', 'Diamond'].includes(c.membershipLevel)).length
  const totalRevenue = statsData?.totalCompletedOrderValue ? Number(statsData.totalCompletedOrderValue) : 0

  // 過濾客戶
  const filteredCustomers = customers.filter(customer => {
    const matchesSearch =
      customer.name.toLowerCase().includes(searchTerm.toLowerCase()) ||
      customer.email.toLowerCase().includes(searchTerm.toLowerCase()) ||
      customer.phone.includes(searchTerm)

    const matchesMembership = membershipFilter === 'ALL' || customer.membershipLevel === membershipFilter

    return matchesSearch && matchesMembership
  })

  // 獲取所有會員等級
  const membershipLevels = Array.from(new Set(customers.map(c => c.membershipLevel)))

  const handleViewCustomer = (customerId: string) => {
    console.log('View customer:', customerId)
  }

  const handleEditCustomer = (customerId: string) => {
    console.log('Edit customer:', customerId)
  }

  const handleMessageCustomer = (customerId: string) => {
    console.log('Message customer:', customerId)
  }

  const handleCreateCustomer = () => {
    console.log('Create new customer')
  }

  const handleRefresh = () => {
    refetch()
  }

  const handleResetFilters = () => {
    setSearchTerm('')
    setMembershipFilter('ALL')
    setCurrentPage(0)
  }

  if (error) {
    return (
      <div className="min-h-screen bg-gradient-to-br from-background via-background to-muted/20">

        <main className="container-modern py-8">
          <div className="flex flex-col items-center justify-center p-8 text-center">
            <p className="text-red-500 mb-4">載入客戶時發生錯誤</p>
            <Button onClick={handleRefresh} variant="outline">
              <RefreshCw className="h-4 w-4 mr-2" />
              重試
            </Button>
          </div>
        </main>
      </div>
    )
  }

  return (
    <div className="min-h-screen bg-gradient-to-br from-background via-background to-muted/20">


      <main className="container-modern py-8 space-y-8">
        {/* 頁面標題 */}
        <div className="flex flex-col sm:flex-row justify-between items-start sm:items-center gap-4">
          <div>
            <h1 className="text-3xl font-bold text-foreground">客戶管理</h1>
            <p className="text-muted-foreground">
              管理客戶資料、會員等級和購買記錄
            </p>
          </div>

          <div className="flex space-x-2">
            <Button variant="outline" size="sm" onClick={handleRefresh}>
              <RefreshCw className="h-4 w-4 mr-2" />
              刷新
            </Button>
            <Button onClick={handleCreateCustomer} className="btn-primary">
              <UserPlus className="h-4 w-4 mr-2" />
              新增客戶
            </Button>
          </div>
        </div>

        {/* 統計卡片 */}
        <div className="grid-stats">
          <StatsCard
            title="客戶總數"
            value={totalCustomers}
            change={{ value: '+5%', type: 'increase', period: '本月' }}
            icon={Users}
            color="blue"
            loading={isLoading}
          />
          <StatsCard
            title="活躍客戶"
            value={activeCustomers}
            change={{
              value: totalCustomers > 0 ? `${Math.round((activeCustomers / totalCustomers) * 100)}%` : '0%',
              type: 'neutral',
              period: '活躍率'
            }}
            icon={TrendingUp}
            color="green"
            loading={isLoading}
          />
          <StatsCard
            title="VIP 客戶"
            value={vipCustomers}
            change={{
              value: totalCustomers > 0 ? `${Math.round((vipCustomers / totalCustomers) * 100)}%` : '0%',
              type: 'neutral',
              period: 'VIP 率'
            }}
            icon={Crown}
            color="purple"
            loading={isLoading}
          />
          <StatsCard
            title="總營收貢獻"
            value={`NT$ ${totalRevenue.toLocaleString()}`}
            change={{ value: '+12%', type: 'increase', period: '本月' }}
            icon={Calendar}
            color="orange"
            loading={isLoading}
          />
        </div>

        {/* 搜尋和篩選 */}
        <div className="modern-card p-6 space-y-4">
          <div className="flex items-center space-x-2 mb-4">
            <Filter className="h-5 w-5 text-primary" />
            <h3 className="font-semibold text-foreground">篩選和搜尋</h3>
          </div>

          <div className="grid grid-cols-1 md:grid-cols-4 gap-4">
            <div className="relative md:col-span-2">
              <Search className="absolute left-3 top-1/2 transform -translate-y-1/2 h-4 w-4 text-muted-foreground" />
              <Input
                placeholder="搜尋客戶姓名、Email 或電話..."
                value={searchTerm}
                onChange={(e) => setSearchTerm(e.target.value)}
                className="pl-10"
              />
            </div>

            <Select value={membershipFilter} onValueChange={setMembershipFilter}>
              <SelectTrigger>
                <SelectValue placeholder="會員等級" />
              </SelectTrigger>
              <SelectContent>
                <SelectItem value="ALL">所有等級</SelectItem>
                {membershipLevels.map(level => (
                  <SelectItem key={level} value={level}>
                    {level}
                  </SelectItem>
                ))}
              </SelectContent>
            </Select>

            <Button variant="outline" className="w-full" onClick={handleResetFilters}>
              <RefreshCw className="h-4 w-4 mr-2" />
              重置篩選
            </Button>
          </div>
        </div>

        {/* 客戶列表 */}
        {isLoading ? (
          <div className="grid-modern">
            {Array.from({ length: 6 }).map((_, index) => (
              <div key={index} className="modern-card p-6 animate-pulse">
                <div className="space-y-4">
                  <div className="flex items-center space-x-3">
                    <div className="w-12 h-12 bg-muted rounded-full"></div>
                    <div className="flex-1 space-y-2">
                      <div className="h-4 bg-muted rounded w-3/4"></div>
                      <div className="h-3 bg-muted rounded w-1/2"></div>
                    </div>
                  </div>
                  <div className="space-y-2">
                    <div className="h-3 bg-muted rounded"></div>
                    <div className="h-3 bg-muted rounded w-2/3"></div>
                  </div>
                </div>
              </div>
            ))}
          </div>
        ) : filteredCustomers.length === 0 ? (
          <EmptyState
            icon={Users}
            title={searchTerm || membershipFilter !== 'ALL'
              ? "沒有符合條件的客戶"
              : "還沒有任何客戶"
            }
            description={searchTerm || membershipFilter !== 'ALL'
              ? "請嘗試調整搜尋條件或篩選器"
              : "開始添加您的第一個客戶來建立客戶資料庫"
            }
            action={{
              label: "新增客戶",
              onClick: handleCreateCustomer
            }}
          />
        ) : (
          <div className="grid-modern">
            {filteredCustomers.map((customer, index) => {
              // 模擬客戶統計數據
              const mockStats = {
                orderCount: Math.floor(Math.random() * 30) + 1,
                totalSpent: Math.floor(Math.random() * 100000) + 5000,
                lastOrderDate: new Date(Date.now() - Math.random() * 30 * 24 * 60 * 60 * 1000).toISOString().split('T')[0]
              }

              return (
                <div
                  key={customer.id}
                  className="animate-scale-in"
                  style={{ animationDelay: `${index * 50}ms` }}
                >
                  <CustomerCard
                    customer={customer}
                    onView={handleViewCustomer}
                    onEdit={handleEditCustomer}
                    onMessage={handleMessageCustomer}
                    orderCount={mockStats.orderCount}
                    totalSpent={mockStats.totalSpent}
                    lastOrderDate={mockStats.lastOrderDate}
                  />
                </div>
              )
            })}
          </div>
        )}

        {/* 分頁 */}
        {customersResponse && customersResponse.totalPages > 1 && (
          <div className="flex justify-center items-center space-x-2">
            <Button
              variant="outline"
              size="sm"
              onClick={() => setCurrentPage(Math.max(0, currentPage - 1))}
              disabled={currentPage === 0}
            >
              上一頁
            </Button>

            <span className="text-sm text-muted-foreground">
              第 {currentPage + 1} 頁，共 {customersResponse.totalPages} 頁
            </span>

            <Button
              variant="outline"
              size="sm"
              onClick={() => setCurrentPage(Math.min(customersResponse.totalPages - 1, currentPage + 1))}
              disabled={currentPage >= customersResponse.totalPages - 1}
            >
              下一頁
            </Button>
          </div>
        )}

        {/* 分頁信息 */}
        {filteredCustomers.length > 0 && (
          <div className="flex justify-center items-center space-x-4 text-sm text-muted-foreground">
            <span>顯示 {filteredCustomers.length} 位客戶，共 {totalCustomers} 位</span>
            {searchTerm && (
              <span>• 搜尋: "{searchTerm}"</span>
            )}
            {membershipFilter !== 'ALL' && (
              <span>• 等級: {membershipFilter}</span>
            )}
          </div>
        )}
      </main>
    </div>
  )
}
