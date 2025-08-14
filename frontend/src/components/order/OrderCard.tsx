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
  X,
  ShoppingBag,
  Clock
} from 'lucide-react'

interface OrderCardProps {
  order: Order
  onView?: (orderId: string) => void
  onEdit?: (orderId: string) => void
  onCancel?: (orderId: string) => void
  showActions?: boolean
  compact?: boolean
}

export const OrderCard: React.FC<OrderCardProps> = ({
  order,
  onView,
  onEdit,
  onCancel,
  showActions = true,
  compact = false,
}) => {
  const canEdit = order?.status === OrderStatus.CREATED
  const canCancel = order?.status && [OrderStatus.CREATED, OrderStatus.PENDING].includes(order.status)

  if (compact) {
    return (
      <Card className="w-full hover:shadow-md transition-all duration-200 border-l-4 border-l-primary/20 hover:border-l-primary/60">
        <CardContent className="p-6">
          <div className="flex items-center justify-between">
            <div className="flex-1 space-y-3">
              {/* 標題行 */}
              <div className="flex items-center justify-between">
                <div className="flex items-center space-x-3">
                  <div className="w-10 h-10 bg-primary/10 rounded-lg flex items-center justify-center">
                    <ShoppingBag className="h-5 w-5 text-primary" />
                  </div>
                  <div>
                    <h3 className="font-semibold text-foreground">
                      訂單 #{order.id?.slice(-8) || 'N/A'}
                    </h3>
                    <p className="text-sm text-muted-foreground flex items-center">
                      <Clock className="h-3 w-3 mr-1" />
                      {formatDate(order?.createdAt, 'MM/dd HH:mm')}
                    </p>
                  </div>
                </div>
                <Badge className={getOrderStatusColor(order?.status)}>
                  {getOrderStatusText(order?.status)}
                </Badge>
              </div>

              {/* 詳情行 */}
              <div className="grid grid-cols-1 md:grid-cols-3 gap-4 text-sm">
                <div className="flex items-center space-x-2">
                  <MapPin className="h-4 w-4 text-muted-foreground flex-shrink-0" />
                  <span className="text-muted-foreground">地址:</span>
                  <span className="truncate">{order?.shippingAddress || 'N/A'}</span>
                </div>
                
                <div className="flex items-center space-x-2">
                  <Package className="h-4 w-4 text-muted-foreground flex-shrink-0" />
                  <span className="text-muted-foreground">商品:</span>
                  <span>{order.items?.length || 0} 項</span>
                </div>

                <div className="flex items-center space-x-2">
                  <CreditCard className="h-4 w-4 text-muted-foreground flex-shrink-0" />
                  <span className="text-muted-foreground">金額:</span>
                  <span className="font-medium">{formatMoney(order?.effectiveAmount)}</span>
                </div>
              </div>
            </div>

            {/* 操作按鈕 */}
            {showActions && (
              <div className="flex space-x-2 ml-6">
                {onView && (
                  <Button
                    variant="outline"
                    size="sm"
                    onClick={() => onView(order?.id)}
                  >
                    <Eye className="h-4 w-4" />
                  </Button>
                )}
                
                {onEdit && canEdit && (
                  <Button
                    variant="outline"
                    size="sm"
                    onClick={() => onEdit(order?.id)}
                  >
                    <Edit className="h-4 w-4" />
                  </Button>
                )}
                
                {onCancel && canCancel && (
                  <Button
                    variant="destructive"
                    size="sm"
                    onClick={() => onCancel(order?.id)}
                  >
                    <X className="h-4 w-4" />
                  </Button>
                )}
              </div>
            )}
          </div>
        </CardContent>
      </Card>
    )
  }

  return (
    <Card className="w-full hover:shadow-lg transition-all duration-300 group modern-card-hover">
      <CardHeader className="pb-3">
        <div className="flex items-center justify-between">
          <div className="flex items-center space-x-3">
            <div className="w-12 h-12 bg-gradient-to-br from-primary/20 to-primary/10 rounded-xl flex items-center justify-center group-hover:from-primary/30 group-hover:to-primary/20 transition-all duration-300">
              <ShoppingBag className="h-6 w-6 text-primary" />
            </div>
            <div>
              <CardTitle className="text-lg font-semibold">
                訂單 #{order.id?.slice(-8) || 'N/A'}
              </CardTitle>
              <p className="text-sm text-muted-foreground flex items-center mt-1">
                <Clock className="h-3 w-3 mr-1" />
                {formatDate(order?.createdAt)}
              </p>
            </div>
          </div>
          <Badge className={getOrderStatusColor(order?.status)}>
            {getOrderStatusText(order?.status)}
          </Badge>
        </div>
      </CardHeader>

      <CardContent className="space-y-4">
        {/* 基本信息 */}
        <div className="space-y-3">
          <div className="flex items-start space-x-2">
            <MapPin className="h-4 w-4 text-muted-foreground mt-0.5 flex-shrink-0" />
            <div className="flex-1">
              <span className="text-sm text-muted-foreground">配送地址:</span>
              <p className="text-sm font-medium">{order?.shippingAddress || 'N/A'}</p>
            </div>
          </div>
        </div>

        {/* 訂單項目 */}
        <div className="space-y-2">
          <div className="flex items-center space-x-2">
            <Package className="h-4 w-4 text-muted-foreground" />
            <span className="text-sm font-medium">商品清單 ({order.items?.length || 0} 項)</span>
          </div>
          
          <div className="bg-muted/30 rounded-lg p-3 space-y-2">
            {(order?.items || []).slice(0, 3).map((item, index) => (
              <div key={item?.id || `${item?.productName}-${index}` || `item-${index}`} className="flex justify-between items-center text-sm">
                <div className="flex-1">
                  <span className="font-medium">{item.productName}</span>
                  <span className="text-muted-foreground ml-2">x{item.quantity}</span>
                </div>
                <span className="font-medium">{formatMoney(item?.totalPrice)}</span>
              </div>
            )) || (
              <div className="text-sm text-muted-foreground text-center">
                暫無商品信息
              </div>
            )}
            
            {(order.items?.length || 0) > 3 && (
              <div className="text-sm text-muted-foreground text-center pt-1 border-t border-border/50">
                還有 {(order.items?.length || 0) - 3} 項商品...
              </div>
            )}
          </div>
        </div>

        {/* 金額信息 */}
        <div className="space-y-2 pt-2 border-t border-border/50">
          <div className="flex justify-between items-center">
            <span className="text-sm text-muted-foreground">原始金額:</span>
            <span className="text-sm">{formatMoney(order?.totalAmount)}</span>
          </div>
          
          {order.effectiveAmount?.amount !== order.totalAmount?.amount && (
            <div className="flex justify-between items-center">
              <span className="text-sm text-muted-foreground">折扣後:</span>
              <span className="text-sm text-green-600 font-medium">
                {formatMoney(order?.effectiveAmount)}
              </span>
            </div>
          )}
          
          <div className="flex justify-between items-center text-lg font-semibold pt-2 border-t border-border/30">
            <span>實付金額:</span>
            <div className="flex items-center space-x-2">
              <CreditCard className="h-4 w-4" />
              <span className="text-primary">{formatMoney(order?.effectiveAmount)}</span>
            </div>
          </div>
        </div>
      </CardContent>

      {showActions && (
        <CardFooter className="pt-3 border-t border-border/50 bg-muted/20">
          <div className="flex space-x-2 w-full">
            {onView && (
              <Button
                variant="outline"
                size="sm"
                onClick={() => onView(order.id)}
                className="flex-1 hover:bg-primary/10 hover:text-primary hover:border-primary/30"
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
                className="flex-1 hover:bg-blue-50 hover:text-blue-600 hover:border-blue-300"
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
