'use client'

import React, { useState } from 'react'
import { OrderCard } from './OrderCard'
import { Button } from '@/components/ui/button'
import { Input } from '@/components/ui/input'
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from '@/components/ui/select'
import { useOrders, useCancelOrder } from '@/hooks/useApi'
import { OrderStatus } from '@/types/domain'
import { Search, Plus, RefreshCw, Filter, Grid3X3, List } from 'lucide-react'
import { toast } from 'react-hot-toast'

interface OrderListProps {
  onCreateOrder?: () => void
  onViewOrder?: (orderId: string) => void
  onEditOrder?: (orderId: string) => void
}

export const OrderList: React.FC<OrderListProps> = ({
  onCreateOrder,
  onViewOrder,
  onEditOrder,
}) => {
  const [searchTerm, setSearchTerm] = useState('')
  const [statusFilter, setStatusFilter] = useState<OrderStatus | 'ALL'>('ALL')
  const [currentPage, setCurrentPage] = useState(0)
  const [viewMode, setViewMode] = useState<'grid' | 'list'>('grid')
  const pageSize = 12

  const { data: ordersResponse, isLoading, error, refetch } = useOrders({
    page: currentPage,
    size: pageSize,
  })

  const cancelOrderMutation = useCancelOrder()

  const handleCancelOrder = async (orderId: string) => {
    if (window.confirm('確定要取消這個訂單嗎？')) {
      try {
        await cancelOrderMutation.mutateAsync(orderId)
        toast.success('訂單已取消')
      } catch (error: any) {
        console.error('Cancel order failed:', error)
        toast.error(`取消訂單失敗: ${error.message}`)
      }
    }
  }

  const handleRefresh = () => {
    refetch()
    toast.success('訂單列表已刷新')
  }

  // 過濾訂單
  const filteredOrders = ordersResponse?.content?.filter((order) => {
    const matchesSearch = 
      order.id.toLowerCase().includes(searchTerm.toLowerCase()) ||
      order.shippingAddress.toLowerCase().includes(searchTerm.toLowerCase())
    
    const matchesStatus = statusFilter === 'ALL' || order.status === statusFilter
    
    return matchesSearch && matchesStatus
  }) || []

  if (error) {
    return (
      <div className="flex flex-col items-center justify-center p-12 text-center">
        <div className="w-16 h-16 bg-red-100 rounded-full flex items-center justify-center mb-4">
          <RefreshCw className="h-8 w-8 text-red-500" />
        </div>
        <h3 className="text-lg font-semibold text-foreground mb-2">載入失敗</h3>
        <p className="text-muted-foreground mb-6">載入訂單時發生錯誤，請重試</p>
        <Button onClick={handleRefresh} variant="outline" className="min-w-32">
          <RefreshCw className="h-4 w-4 mr-2" />
          重新載入
        </Button>
      </div>
    )
  }

  return (
    <div className="space-y-6">
      {/* 操作工具欄 */}
      <div className="flex flex-col lg:flex-row gap-4 items-start lg:items-center justify-between">
        {/* 搜尋和篩選 */}
        <div className="flex flex-col sm:flex-row gap-3 flex-1 max-w-2xl">
          <div className="relative flex-1">
            <Search className="absolute left-3 top-1/2 transform -translate-y-1/2 h-4 w-4 text-muted-foreground" />
            <Input
              placeholder="搜尋訂單 ID 或配送地址..."
              value={searchTerm}
              onChange={(e) => setSearchTerm(e.target.value)}
              className="pl-10 h-10"
            />
          </div>
          
          <Select value={statusFilter} onValueChange={(value) => setStatusFilter(value as OrderStatus | 'ALL')}>
            <SelectTrigger className="w-full sm:w-48 h-10">
              <Filter className="h-4 w-4 mr-2" />
              <SelectValue placeholder="篩選狀態" />
            </SelectTrigger>
            <SelectContent>
              <SelectItem value="ALL">所有狀態</SelectItem>
              <SelectItem value={OrderStatus.CREATED}>已創建</SelectItem>
              <SelectItem value={OrderStatus.PENDING}>待處理</SelectItem>
              <SelectItem value={OrderStatus.CONFIRMED}>已確認</SelectItem>
              <SelectItem value={OrderStatus.SHIPPED}>已出貨</SelectItem>
              <SelectItem value={OrderStatus.DELIVERED}>已送達</SelectItem>
              <SelectItem value={OrderStatus.CANCELLED}>已取消</SelectItem>
            </SelectContent>
          </Select>
        </div>

        {/* 操作按鈕 */}
        <div className="flex items-center gap-2">
          {/* 視圖切換 */}
          <div className="flex items-center border rounded-lg p-1">
            <Button
              variant={viewMode === 'grid' ? 'default' : 'ghost'}
              size="sm"
              onClick={() => setViewMode('grid')}
              className="h-8 px-3"
            >
              <Grid3X3 className="h-4 w-4" />
            </Button>
            <Button
              variant={viewMode === 'list' ? 'default' : 'ghost'}
              size="sm"
              onClick={() => setViewMode('list')}
              className="h-8 px-3"
            >
              <List className="h-4 w-4" />
            </Button>
          </div>

          <Button onClick={handleRefresh} variant="outline" size="sm" className="h-10">
            <RefreshCw className="h-4 w-4 mr-2" />
            刷新
          </Button>
          
          {onCreateOrder && (
            <Button onClick={onCreateOrder} size="sm" className="h-10">
              <Plus className="h-4 w-4 mr-2" />
              新增訂單
            </Button>
          )}
        </div>
      </div>

      {/* 結果統計 */}
      {!isLoading && (
        <div className="flex items-center justify-between text-sm text-muted-foreground">
          <span>
            顯示 {filteredOrders.length} 個訂單
            {searchTerm && ` · 搜尋「${searchTerm}」`}
            {statusFilter !== 'ALL' && ` · 狀態「${statusFilter}」`}
          </span>
          {ordersResponse && ordersResponse.totalPages > 1 && (
            <span>
              第 {currentPage + 1} 頁，共 {ordersResponse.totalPages} 頁
            </span>
          )}
        </div>
      )}

      {/* 訂單列表 */}
      {isLoading ? (
        <div className={`grid gap-6 ${viewMode === 'grid' ? 'grid-cols-1 md:grid-cols-2 lg:grid-cols-3' : 'grid-cols-1'}`}>
          {Array.from({ length: 6 }).map((_, index) => (
            <div key={index} className="animate-pulse">
              <div className="modern-card p-6">
                <div className="flex items-center justify-between mb-4">
                  <div className="h-6 bg-muted rounded w-32"></div>
                  <div className="h-6 bg-muted rounded-full w-20"></div>
                </div>
                <div className="space-y-3">
                  <div className="h-4 bg-muted rounded w-full"></div>
                  <div className="h-4 bg-muted rounded w-3/4"></div>
                  <div className="h-4 bg-muted rounded w-1/2"></div>
                </div>
                <div className="flex gap-2 mt-6">
                  <div className="h-9 bg-muted rounded flex-1"></div>
                  <div className="h-9 bg-muted rounded flex-1"></div>
                </div>
              </div>
            </div>
          ))}
        </div>
      ) : filteredOrders.length === 0 ? (
        <div className="text-center py-16">
          <div className="w-20 h-20 bg-muted/50 rounded-full flex items-center justify-center mx-auto mb-6">
            <Search className="h-10 w-10 text-muted-foreground" />
          </div>
          <h3 className="text-lg font-semibold text-foreground mb-2">
            {searchTerm || statusFilter !== 'ALL' ? '沒有符合條件的訂單' : '還沒有任何訂單'}
          </h3>
          <p className="text-muted-foreground mb-6">
            {searchTerm || statusFilter !== 'ALL' 
              ? '嘗試調整搜尋條件或篩選器' 
              : '創建您的第一個訂單開始使用系統'
            }
          </p>
          {onCreateOrder && (
            <Button onClick={onCreateOrder} className="min-w-40">
              <Plus className="h-4 w-4 mr-2" />
              創建第一個訂單
            </Button>
          )}
        </div>
      ) : (
        <>
          <div className={`grid gap-6 ${viewMode === 'grid' ? 'grid-cols-1 md:grid-cols-2 lg:grid-cols-3' : 'grid-cols-1'}`}>
            {filteredOrders.map((order, index) => (
              <div 
                key={order.id} 
                className="animate-scale-in" 
                style={{ animationDelay: `${index * 50}ms` }}
              >
                <OrderCard
                  order={order}
                  onView={onViewOrder}
                  onEdit={onEditOrder}
                  onCancel={handleCancelOrder}
                  compact={viewMode === 'list'}
                />
              </div>
            ))}
          </div>

          {/* 分頁 */}
          {ordersResponse && ordersResponse.totalPages > 1 && (
            <div className="flex justify-center items-center space-x-4 pt-8">
              <Button
                variant="outline"
                size="sm"
                onClick={() => setCurrentPage(Math.max(0, currentPage - 1))}
                disabled={currentPage === 0}
                className="min-w-20"
              >
                上一頁
              </Button>
              
              <div className="flex items-center space-x-2">
                {Array.from({ length: Math.min(5, ordersResponse.totalPages) }, (_, i) => {
                  const pageNum = currentPage < 3 ? i : currentPage - 2 + i
                  if (pageNum >= ordersResponse.totalPages) return null
                  
                  return (
                    <Button
                      key={pageNum}
                      variant={pageNum === currentPage ? 'default' : 'outline'}
                      size="sm"
                      onClick={() => setCurrentPage(pageNum)}
                      className="w-10 h-10"
                    >
                      {pageNum + 1}
                    </Button>
                  )
                })}
              </div>
              
              <Button
                variant="outline"
                size="sm"
                onClick={() => setCurrentPage(Math.min(ordersResponse.totalPages - 1, currentPage + 1))}
                disabled={currentPage >= ordersResponse.totalPages - 1}
                className="min-w-20"
              >
                下一頁
              </Button>
            </div>
          )}
        </>
      )}
    </div>
  )
}
