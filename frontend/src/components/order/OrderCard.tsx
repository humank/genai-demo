'use client'

import React from 'react'
import { Card, CardContent, CardFooter, CardHeader, CardTitle } from '@/components/ui/card'
import { Button } from '@/components/ui/button'
import { Badge } from '@/components/ui/badge'
import { Order, OrderStatus } from '@/types/domain'
import { 
  formatMoney, 
  formatDate, 
  getOrderStatusText, 
  getOrderStatusColor 
} from '@/lib/utils'
import { 
  Package, 
  Calendar, 
  MapPin, 
  CreditCard, 
  Eye, 
  Edit, 
  X 
} from 'lucide-react'

interface OrderCardProps {
  order: Order
  onView?: (orderId: string) => void
  onEdit?: (orderId: string) => void
  onCancel?: (orderId: string) => void
  showActions?: boolean
}

export const OrderCard: React.FC<OrderCardProps> = ({
  order,
  onView,
  onEdit,
  onCancel,
  showActions = true,
}) => {
  const canEdit = order.status === OrderStatus.CREATED
  const canCancel = [OrderStatus.CREATED, OrderStatus.PENDING].includes(order.status)

  return (
    <Card className="w-full hover:shadow-md transition-shadow">
      <CardHeader className="pb-3">
        <div className="flex items-center justify-between">
          <CardTitle className="text-lg font-semibold">
            訂單 #{order.id.slice(-8)}
          </CardTitle>
          <Badge className={getOrderStatusColor(order.status)}>
            {getOrderStatusText(order.status)}
          </Badge>
        </div>
      </CardHeader>

      <CardContent className="space-y-4">
        {/* 基本信息 */}
        <div className="grid grid-cols-1 md:grid-cols-2 gap-4 text-sm">
          <div className="flex items-center space-x-2">
            <Calendar className="h-4 w-4 text-muted-foreground" />
            <span className="text-muted-foreground">創建時間:</span>
            <span>{formatDate(order.createdAt)}</span>
          </div>
          
          <div className="flex items-center space-x-2">
            <MapPin className="h-4 w-4 text-muted-foreground" />
            <span className="text-muted-foreground">配送地址:</span>
            <span className="truncate">{order.shippingAddress}</span>
          </div>
        </div>

        {/* 訂單項目 */}
        <div className="space-y-2">
          <div className="flex items-center space-x-2">
            <Package className="h-4 w-4 text-muted-foreground" />
            <span className="text-sm font-medium">商品清單 ({order.items.length} 項)</span>
          </div>
          
          <div className="bg-muted/50 rounded-md p-3 space-y-2">
            {order.items.slice(0, 3).map((item, index) => (
              <div key={index} className="flex justify-between items-center text-sm">
                <div className="flex-1">
                  <span className="font-medium">{item.productName}</span>
                  <span className="text-muted-foreground ml-2">x{item.quantity}</span>
                </div>
                <span className="font-medium">{formatMoney(item.totalPrice)}</span>
              </div>
            ))}
            
            {order.items.length > 3 && (
              <div className="text-sm text-muted-foreground text-center pt-1">
                還有 {order.items.length - 3} 項商品...
              </div>
            )}
          </div>
        </div>

        {/* 金額信息 */}
        <div className="space-y-2 pt-2 border-t">
          <div className="flex justify-between items-center">
            <span className="text-sm text-muted-foreground">原始金額:</span>
            <span className="text-sm">{formatMoney(order.totalAmount)}</span>
          </div>
          
          {order.effectiveAmount.amount !== order.totalAmount.amount && (
            <div className="flex justify-between items-center">
              <span className="text-sm text-muted-foreground">折扣後:</span>
              <span className="text-sm text-green-600 font-medium">
                {formatMoney(order.effectiveAmount)}
              </span>
            </div>
          )}
          
          <div className="flex justify-between items-center text-lg font-semibold">
            <span>實付金額:</span>
            <div className="flex items-center space-x-2">
              <CreditCard className="h-4 w-4" />
              <span>{formatMoney(order.effectiveAmount)}</span>
            </div>
          </div>
        </div>
      </CardContent>

      {showActions && (
        <CardFooter className="pt-3 border-t">
          <div className="flex space-x-2 w-full">
            {onView && (
              <Button
                variant="outline"
                size="sm"
                onClick={() => onView(order.id)}
                className="flex-1"
              >
                <Eye className="h-4 w-4 mr-2" />
                查看詳情
              </Button>
            )}
            
            {onEdit && canEdit && (
              <Button
                variant="outline"
                size="sm"
                onClick={() => onEdit(order.id)}
                className="flex-1"
              >
                <Edit className="h-4 w-4 mr-2" />
                編輯
              </Button>
            )}
            
            {onCancel && canCancel && (
              <Button
                variant="destructive"
                size="sm"
                onClick={() => onCancel(order.id)}
                className="flex-1"
              >
                <X className="h-4 w-4 mr-2" />
                取消
              </Button>
            )}
          </div>
        </CardFooter>
      )}
    </Card>
  )
}
