'use client'

import React, { useState } from 'react'
import { useParams, useRouter } from 'next/navigation'
import { ArrowLeft, Package, DollarSign, Archive, Tag, AlertTriangle, CheckCircle } from 'lucide-react'
import { Button } from '@/components/ui/button'
import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card'
import { Badge } from '@/components/ui/badge'
import { Navbar } from '@/components/layout/Navbar'
import { EditProductDialog } from '@/components/product/EditProductDialog'
import { AdjustInventoryDialog } from '@/components/inventory/AdjustInventoryDialog'
import { useProduct, useDeleteProduct, useInventory } from '@/hooks/useApi'
import { formatMoney } from '@/lib/utils'
import { cn } from '@/lib/utils'
import { toast } from 'react-hot-toast'

export default function ProductDetailPage() {
  const params = useParams()
  const router = useRouter()
  const productId = params.productId as string

  const [editDialogOpen, setEditDialogOpen] = useState(false)
  const [inventoryDialogOpen, setInventoryDialogOpen] = useState(false)

  const { data: product, isLoading, error } = useProduct(productId)
  const { data: inventory } = useInventory(productId)
  const deleteProductMutation = useDeleteProduct()

  const handleEditProduct = () => {
    setEditDialogOpen(true)
  }

  const handleAdjustInventory = () => {
    setInventoryDialogOpen(true)
  }

  const handleDeleteProduct = async () => {
    if (!product) return
    
    const confirmed = window.confirm(
      `確定要刪除產品「${product.name}」嗎？此操作無法撤銷。`
    )
    
    if (confirmed) {
      try {
        await deleteProductMutation.mutateAsync(product.id)
        toast.success('產品已成功刪除')
        router.push('/products')
      } catch (error) {
        toast.error('刪除產品時發生錯誤')
        console.error('Delete product error:', error)
      }
    }
  }

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

  if (error || !product) {
    return (
      <div className="min-h-screen bg-background">
        <Navbar />
        <main className="container-modern py-8">
          <div className="text-center space-y-4">
            <h1 className="text-2xl font-bold text-foreground">商品不存在</h1>
            <p className="text-muted-foreground">找不到指定的商品，可能已被刪除或您沒有權限查看。</p>
            <Button onClick={() => router.back()}>
              <ArrowLeft className="h-4 w-4 mr-2" />
              返回
            </Button>
          </div>
        </main>
      </div>
    )
  }

  const currentStock = inventory?.availableQuantity ?? product.stockQuantity ?? 0
  const isLowStock = currentStock < 10
  const isOutOfStock = currentStock === 0

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
                {product.name}
              </h1>
              <p className="text-muted-foreground">
                商品 ID: {product.id}
              </p>
            </div>
          </div>
          <div className="flex items-center space-x-2">
            {isOutOfStock ? (
              <Badge variant="destructive">缺貨</Badge>
            ) : isLowStock ? (
              <Badge className="bg-yellow-100 text-yellow-800">庫存不足</Badge>
            ) : (
              <Badge className="bg-green-100 text-green-800">有庫存</Badge>
            )}
          </div>
        </div>

        <div className="grid grid-cols-1 lg:grid-cols-3 gap-6">
          {/* 左側：商品信息 */}
          <div className="lg:col-span-2 space-y-6">
            {/* 基本信息 */}
            <Card>
              <CardHeader>
                <CardTitle className="flex items-center">
                  <Package className="h-5 w-5 mr-2" />
                  商品信息
                </CardTitle>
              </CardHeader>
              <CardContent className="space-y-4">
                <div>
                  <label className="text-sm font-medium text-muted-foreground">商品名稱</label>
                  <p className="text-lg font-semibold text-foreground">{product.name}</p>
                </div>
                
                <div>
                  <label className="text-sm font-medium text-muted-foreground">商品描述</label>
                  <p className="text-foreground">{product.description || '暫無描述'}</p>
                </div>
                
                <div className="grid grid-cols-2 gap-4">
                  <div>
                    <label className="text-sm font-medium text-muted-foreground">分類</label>
                    <div className="flex items-center space-x-2 mt-1">
                      <Tag className="h-4 w-4 text-muted-foreground" />
                      <Badge variant="outline">{product.category}</Badge>
                    </div>
                  </div>
                  
                  <div>
                    <label className="text-sm font-medium text-muted-foreground">商品 ID</label>
                    <p className="text-sm text-muted-foreground font-mono">{product.id}</p>
                  </div>
                </div>
              </CardContent>
            </Card>

            {/* 庫存詳情 */}
            <Card>
              <CardHeader>
                <CardTitle className="flex items-center">
                  <Archive className="h-5 w-5 mr-2" />
                  庫存詳情
                </CardTitle>
              </CardHeader>
              <CardContent className="space-y-4">
                <div className="grid grid-cols-1 md:grid-cols-3 gap-4">
                  <div className="text-center p-4 bg-muted/30 rounded-lg">
                    <div className="flex items-center justify-center mb-2">
                      {isOutOfStock ? (
                        <AlertTriangle className="h-6 w-6 text-red-500" />
                      ) : isLowStock ? (
                        <AlertTriangle className="h-6 w-6 text-yellow-500" />
                      ) : (
                        <CheckCircle className="h-6 w-6 text-green-500" />
                      )}
                    </div>
                    <p className="text-2xl font-bold text-foreground">{currentStock}</p>
                    <p className="text-sm text-muted-foreground">當前庫存</p>
                  </div>
                  
                  <div className="text-center p-4 bg-muted/30 rounded-lg">
                    <div className="flex items-center justify-center mb-2">
                      <Package className="h-6 w-6 text-blue-500" />
                    </div>
                    <p className="text-2xl font-bold text-foreground">
                      {product.inStock ? '有庫存' : '缺貨'}
                    </p>
                    <p className="text-sm text-muted-foreground">庫存狀態</p>
                  </div>
                  
                  <div className="text-center p-4 bg-muted/30 rounded-lg">
                    <div className="flex items-center justify-center mb-2">
                      <DollarSign className="h-6 w-6 text-purple-500" />
                    </div>
                    <p className="text-2xl font-bold text-foreground">
                      {formatMoney({ amount: product.price.amount * currentStock, currency: product.price.currency })}
                    </p>
                    <p className="text-sm text-muted-foreground">庫存價值</p>
                  </div>
                </div>

                {/* 庫存狀態條 */}
                <div className="space-y-2">
                  <div className="flex justify-between text-sm">
                    <span className="text-muted-foreground">庫存水平</span>
                    <span className={cn(
                      "font-medium",
                      isOutOfStock ? "text-red-600" : isLowStock ? "text-yellow-600" : "text-green-600"
                    )}>
                      {isOutOfStock ? '缺貨' : isLowStock ? '庫存不足' : '庫存充足'}
                    </span>
                  </div>
                  <div className="w-full bg-muted rounded-full h-3">
                    <div 
                      className={cn(
                        "h-3 rounded-full transition-all duration-300",
                        isOutOfStock ? "bg-red-500" : isLowStock ? "bg-yellow-500" : "bg-green-500"
                      )}
                      style={{ 
                        width: `${Math.min(100, Math.max(5, (currentStock / 50) * 100))}%` 
                      }}
                    />
                  </div>
                </div>
              </CardContent>
            </Card>
          </div>

          {/* 右側：價格和操作 */}
          <div className="space-y-6">
            {/* 價格信息 */}
            <Card>
              <CardHeader>
                <CardTitle className="flex items-center">
                  <DollarSign className="h-5 w-5 mr-2" />
                  價格信息
                </CardTitle>
              </CardHeader>
              <CardContent className="space-y-4">
                <div className="text-center p-6 bg-gradient-to-br from-primary/5 to-primary/10 rounded-lg border border-primary/20">
                  <p className="text-sm text-muted-foreground mb-2">售價</p>
                  <p className="text-3xl font-bold text-primary">
                    {formatMoney(product.price)}
                  </p>
                </div>
                
                <div className="space-y-3 text-sm">
                  <div className="flex justify-between">
                    <span className="text-muted-foreground">貨幣</span>
                    <span className="font-medium">{product.price.currency}</span>
                  </div>
                  
                  <div className="flex justify-between">
                    <span className="text-muted-foreground">單位</span>
                    <span className="font-medium">每件</span>
                  </div>
                </div>
              </CardContent>
            </Card>

            {/* 操作按鈕 */}
            <Card>
              <CardHeader>
                <CardTitle>操作</CardTitle>
              </CardHeader>
              <CardContent className="space-y-3">
                <Button 
                  className="w-full" 
                  variant="outline"
                  onClick={handleEditProduct}
                >
                  <Package className="h-4 w-4 mr-2" />
                  編輯商品
                </Button>
                
                <Button 
                  className="w-full" 
                  variant="outline"
                  onClick={handleAdjustInventory}
                >
                  <Archive className="h-4 w-4 mr-2" />
                  調整庫存
                </Button>
                
                <Button 
                  className="w-full" 
                  variant="destructive"
                  onClick={handleDeleteProduct}
                  disabled={deleteProductMutation.isPending}
                >
                  <AlertTriangle className="h-4 w-4 mr-2" />
                  {deleteProductMutation.isPending ? '刪除中...' : '刪除商品'}
                </Button>
              </CardContent>
            </Card>
          </div>
        </div>

        {/* 對話框 */}
        <EditProductDialog
          product={product}
          open={editDialogOpen}
          onOpenChange={setEditDialogOpen}
        />
        
        <AdjustInventoryDialog
          productId={product?.id || null}
          productName={product?.name || ''}
          currentStock={currentStock}
          open={inventoryDialogOpen}
          onOpenChange={setInventoryDialogOpen}
        />
      </main>
    </div>
  )
}