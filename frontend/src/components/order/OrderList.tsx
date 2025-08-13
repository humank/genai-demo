'use client'

import React, { useState } from 'react'
import { OrderCard } from './OrderCard'
import { Button } from '@/components/ui/button'
import { Input } from '@/components/ui/input'
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from '@/components/ui/select'
import { useOrders, useCancelOrder } from '@/hooks/useApi'
import { OrderStatus } from '@/types/domain'
import { Search, Plus, RefreshCw } from 'lucide-react'
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
  const pageSize = 10

  const { data: ordersResponse, isLoading, error, refetch } = useOrders({
    page: currentPage,
    size: pageSize,
  })

  const cancelOrderMutation = useCancelOrder()

  const handleCancelOrder = async (orderId: string) => {
    if (window.confirm('確定要取消這個訂單嗎？')) {
      try {
        await cancelOrderMutation.mutateAsync(orderId)
      } catch (error) {
        console.error('Cancel order failed:', error)
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
      <div className="flex flex-col items-center justify-center p-8 text-center">
        <p className="text-red-500 mb-4">載入訂單時發生錯誤</p>
        <Button onClick={handleRefresh} variant="outline">
          <RefreshCw className="h-4 w-4 mr-2" />
          重試
        </Button>
      </div>
    )
  }

  return (
    <div className="space-y-6">
      {/* 標題和操作區 */}
      <div className="flex flex-col sm:flex-row justify-between items-start sm:items-center gap-4">
        <div>
          <h1 className="text-2xl font-bold">訂單管理</h1>
          <p className="text-muted-foreground">
            管理和追蹤所有訂單
          </p>
        </div>
        
        <div className="flex space-x-2">
          <Button onClick={handleRefresh} variant="outline" size="sm">
            <RefreshCw className="h-4 w-4 mr-2" />
            刷新
          </Button>
          
          {onCreateOrder && (
            <Button onClick={onCreateOrder} size="sm">
              <Plus className="h-4 w-4 mr-2" />
              新增訂單
            </Button>
          )}
        </div>
      </div>

      {/* 搜尋和篩選區 */}
      <div className="flex flex-col sm:flex-row gap-4">
        <div className="relative flex-1">
          <Search className="absolute left-3 top-1/2 transform -translate-y-1/2 h-4 w-4 text-muted-foreground" />
          <Input
            placeholder="搜尋訂單 ID 或配送地址..."
            value={searchTerm}
            onChange={(e) => setSearchTerm(e.target.value)}
            className="pl-10"
          />
        </div>
        
        <Select value={statusFilter} onValueChange={(value) => setStatusFilter(value as OrderStatus | 'ALL')}>
          <SelectTrigger className="w-full sm:w-48">
            <SelectValue placeholder="選擇狀態" />
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

      {/* 訂單列表 */}
      {isLoading ? (
        <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-6">
          {Array.from({ length: 6 }).map((_, index) => (
            <div key={index} className="animate-pulse">
              <div className="bg-muted rounded-lg h-64"></div>
            </div>
          ))}
        </div>
      ) : filteredOrders.length === 0 ? (
        <div className="text-center py-12">
          <p className="text-muted-foreground mb-4">
            {searchTerm || statusFilter !== 'ALL' ? '沒有符合條件的訂單' : '還沒有任何訂單'}
          </p>
          {onCreateOrder && (
            <Button onClick={onCreateOrder}>
              <Plus className="h-4 w-4 mr-2" />
              創建第一個訂單
            </Button>
          )}
        </div>
      ) : (
        <>
          <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-6">
            {filteredOrders.map((order) => (
              <OrderCard
                key={order.id}
                order={order}
                onView={onViewOrder}
                onEdit={onEditOrder}
                onCancel={handleCancelOrder}
              />
            ))}
          </div>

          {/* 分頁 */}
          {ordersResponse && ordersResponse.totalPages > 1 && (
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
                第 {currentPage + 1} 頁，共 {ordersResponse.totalPages} 頁
              </span>
              
              <Button
                variant="outline"
                size="sm"
                onClick={() => setCurrentPage(Math.min(ordersResponse.totalPages - 1, currentPage + 1))}
                disabled={currentPage >= ordersResponse.totalPages - 1}
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
