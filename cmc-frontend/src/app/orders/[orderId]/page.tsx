'use client'

import React from 'react'
import { useParams, useRouter } from 'next/navigation'
import { ArrowLeft, Package, MapPin, Clock, CreditCard, User, Phone, Mail } from 'lucide-react'
import { Button } from '@/components/ui/button'
import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card'
import { Badge } from '@/components/ui/badge'
import { Navbar } from '@/components/layout/Navbar'
import { useOrder } from '@/hooks/useApi'
import { formatMoney, formatDate, getOrderStatusColor, getOrderStatusText } from '@/lib/utils'

export default function OrderDetailPage() {
  const params = useParams()
  const router = useRouter()
  const orderId = params.orderId as string

  const { data: order, isLoading, error } = useOrder(orderId)

  if (isLoading) {
    return (
      <div className="min-h-screen bg-background">
        <Navbar />
        <main className="container-modern py-8">
          <div className="animate-pulse space-y-6">
            <div className="h-8 bg-muted rounded w-64"></div>
            <div className="grid gap-6">
              <div className="h-64 bg-muted rounded"></div>
              <div className="h-48 bg-muted rounded"></div>
            </div>
          </div>
        </main>
      </div>
    )
  }

  if (error || !order) {
    return (
      <div className="min-h-screen bg-background">
        <Navbar />
        <main className="container-modern py-8">
          <div className="text-center space-y-4">
            <h1 className="text-2xl font-bold text-foreground">訂單不存在</h1>
            <p className="text-muted-foreground">找不到指定的訂單，可能已被刪除或您沒有權限查看。</p>
            <Button onClick={() => router.back()}>
              <ArrowLeft className="h-4 w-4 mr-2" />
              返回
            </Button>
          </div>
        </main>
      </div>
    )
  }

  return (
    <div className="min-h-screen bg-background">
      <Navbar />
      
      <main className="container-modern py-8 space-y-6">
        {/* 頁面標題 */}
        <div className="flex items-center justify-between">
          <div className="flex items-center space-x-4">
            <Button 
              variant="outline" 
              size="sm"
              onClick={() => router.back()}
            >
              <ArrowLeft className="h-4 w-4 mr-2" />
              返回
            </Button>
            <div>
              <h1 className="text-2xl font-bold text-foreground">
                訂單詳情 #{order.id?.slice(-8) || 'N/A'}
              </h1>
              <p className="text-muted-foreground">
                創建時間: {formatDate(order.createdAt)}
              </p>
            </div>
          </div>
          <Badge className={getOrderStatusColor(order.status)}>
            {getOrderStatusText(order.status)}
          </Badge>
        </div>

        <div className="grid grid-cols-1 lg:grid-cols-3 gap-6">
          {/* 左側：訂單信息 */}
          <div className="lg:col-span-2 space-y-6">
            {/* 商品清單 */}
            <Card>
              <CardHeader>
                <CardTitle className="flex items-center">
                  <Package className="h-5 w-5 mr-2" />
                  商品清單 ({order.items?.length || 0} 項)
                </CardTitle>
              </CardHeader>
              <CardContent>
                <div className="space-y-4">
                  {(order.items || []).map((item, index) => (
                    <div key={item?.id || `item-${index}`} className="flex justify-between items-center p-4 bg-muted/30 rounded-lg">
                      <div className="flex-1">
                        <h4 className="font-medium text-foreground">
                          {item?.productName || 'N/A'}
                        </h4>
                        <p className="text-sm text-muted-foreground">
                          單價: {formatMoney(item?.unitPrice)} × {item?.quantity || 0}
                        </p>
                      </div>
                      <div className="text-right">
                        <p className="font-medium text-foreground">
                          {formatMoney(item?.totalPrice)}
                        </p>
                      </div>
                    </div>
                  ))}
                </div>
              </CardContent>
            </Card>

            {/* 配送信息 */}
            <Card>
              <CardHeader>
                <CardTitle className="flex items-center">
                  <MapPin className="h-5 w-5 mr-2" />
                  配送信息
                </CardTitle>
              </CardHeader>
              <CardContent>
                <div className="space-y-3">
                  <div>
                    <label className="text-sm font-medium text-muted-foreground">配送地址</label>
                    <p className="text-foreground">{order.shippingAddress || 'N/A'}</p>
                  </div>
                </div>
              </CardContent>
            </Card>
          </div>

          {/* 右側：訂單摘要 */}
          <div className="space-y-6">
            {/* 訂單摘要 */}
            <Card>
              <CardHeader>
                <CardTitle className="flex items-center">
                  <CreditCard className="h-5 w-5 mr-2" />
                  訂單摘要
                </CardTitle>
              </CardHeader>
              <CardContent className="space-y-4">
                <div className="flex justify-between">
                  <span className="text-muted-foreground">商品總額</span>
                  <span>{formatMoney(order.totalAmount)}</span>
                </div>
                
                {order.effectiveAmount?.amount !== order.totalAmount?.amount && (
                  <div className="flex justify-between text-green-600">
                    <span>折扣後金額</span>
                    <span className="font-medium">{formatMoney(order.effectiveAmount)}</span>
                  </div>
                )}
                
                <div className="border-t pt-4">
                  <div className="flex justify-between items-center">
                    <span className="text-lg font-semibold">總計</span>
                    <span className="text-lg font-bold text-primary">
                      {formatMoney(order.effectiveAmount || order.totalAmount)}
                    </span>
                  </div>
                </div>
              </CardContent>
            </Card>

            {/* 客戶信息 */}
            <Card>
              <CardHeader>
                <CardTitle className="flex items-center">
                  <User className="h-5 w-5 mr-2" />
                  客戶信息
                </CardTitle>
              </CardHeader>
              <CardContent>
                <div className="space-y-3">
                  <div>
                    <label className="text-sm font-medium text-muted-foreground">客戶 ID</label>
                    <p className="text-foreground">{order.customerId || 'N/A'}</p>
                  </div>
                </div>
              </CardContent>
            </Card>

            {/* 訂單時間軸 */}
            <Card>
              <CardHeader>
                <CardTitle className="flex items-center">
                  <Clock className="h-5 w-5 mr-2" />
                  訂單時間軸
                </CardTitle>
              </CardHeader>
              <CardContent>
                <div className="space-y-3">
                  <div className="flex items-center space-x-3">
                    <div className="w-2 h-2 bg-primary rounded-full"></div>
                    <div>
                      <p className="text-sm font-medium">訂單創建</p>
                      <p className="text-xs text-muted-foreground">
                        {formatDate(order.createdAt)}
                      </p>
                    </div>
                  </div>
                  
                  {order.updatedAt !== order.createdAt && (
                    <div className="flex items-center space-x-3">
                      <div className="w-2 h-2 bg-muted rounded-full"></div>
                      <div>
                        <p className="text-sm font-medium">最後更新</p>
                        <p className="text-xs text-muted-foreground">
                          {formatDate(order.updatedAt)}
                        </p>
                      </div>
                    </div>
                  )}
                </div>
              </CardContent>
            </Card>
          </div>
        </div>
      </main>
    </div>
  )
}