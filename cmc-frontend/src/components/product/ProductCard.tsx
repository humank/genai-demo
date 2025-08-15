'use client'

import React from 'react'
import { Card, CardContent, CardFooter, CardHeader } from '@/components/ui/card'
import { Button } from '@/components/ui/button'
import { Badge } from '@/components/ui/badge'
import { Product } from '@/types/domain'
import { formatMoney } from '@/lib/utils'
import { 
  Package, 
  Eye, 
  Edit, 
  Trash2, 
  AlertTriangle,
  CheckCircle,
  Star
} from 'lucide-react'
import { cn } from '@/lib/utils'

interface ProductCardProps {
  product: Product
  onView?: (productId: string) => void
  onEdit?: (productId: string) => void
  onDelete?: (productId: string) => void
  showActions?: boolean
}

export const ProductCard: React.FC<ProductCardProps> = ({
  product,
  onView,
  onEdit,
  onDelete,
  showActions = true,
}) => {
  const isLowStock = product.stockQuantity < 10
  const isOutOfStock = product.stockQuantity === 0

  return (
    <Card className="modern-card modern-card-hover group">
      <CardHeader className="pb-3">
        <div className="flex items-start justify-between">
          <div className="flex items-center space-x-3">
            <div className={cn(
              "w-12 h-12 rounded-lg flex items-center justify-center border",
              isOutOfStock 
                ? "bg-red-50 border-red-200 text-red-600"
                : isLowStock
                ? "bg-yellow-50 border-yellow-200 text-yellow-600"
                : "bg-green-50 border-green-200 text-green-600"
            )}>
              <Package className="h-6 w-6" />
            </div>
            
            <div className="flex-1 min-w-0">
              <h3 className="font-semibold text-foreground group-hover:text-primary transition-colors truncate">
                {product.name}
              </h3>
              <p className="text-sm text-muted-foreground">
                ID: {product.id.slice(-8)}
              </p>
            </div>
          </div>
          
          <div className="flex items-center space-x-2">
            {!product.inStock && (
              <Badge variant="destructive" className="text-xs">
                缺貨
              </Badge>
            )}
            {isLowStock && product.inStock && (
              <Badge className="bg-yellow-100 text-yellow-800 text-xs">
                庫存不足
              </Badge>
            )}
            {product.inStock && !isLowStock && (
              <Badge className="bg-green-100 text-green-800 text-xs">
                有庫存
              </Badge>
            )}
          </div>
        </div>
      </CardHeader>

      <CardContent className="space-y-4">
        {/* 產品描述 */}
        <p className="text-sm text-muted-foreground line-clamp-2">
          {product.description}
        </p>

        {/* 價格和庫存信息 */}
        <div className="space-y-3">
          <div className="flex items-center justify-between">
            <span className="text-sm text-muted-foreground">售價:</span>
            <span className="text-lg font-bold text-foreground">
              {formatMoney(product.price)}
            </span>
          </div>
          
          <div className="flex items-center justify-between">
            <span className="text-sm text-muted-foreground">分類:</span>
            <Badge variant="outline" className="text-xs">
              {product.category}
            </Badge>
          </div>
          
          <div className="flex items-center justify-between">
            <span className="text-sm text-muted-foreground">庫存:</span>
            <div className="flex items-center space-x-2">
              {isOutOfStock ? (
                <AlertTriangle className="h-4 w-4 text-red-500" />
              ) : isLowStock ? (
                <AlertTriangle className="h-4 w-4 text-yellow-500" />
              ) : (
                <CheckCircle className="h-4 w-4 text-green-500" />
              )}
              <span className={cn(
                "text-sm font-medium",
                isOutOfStock ? "text-red-600" : isLowStock ? "text-yellow-600" : "text-green-600"
              )}>
                {product.stockQuantity} 件
              </span>
            </div>
          </div>
        </div>

        {/* 庫存狀態條 */}
        <div className="space-y-2">
          <div className="flex justify-between text-xs text-muted-foreground">
            <span>庫存狀態</span>
            <span>{Math.min(100, (product.stockQuantity / 50) * 100).toFixed(0)}%</span>
          </div>
          <div className="w-full bg-muted rounded-full h-2">
            <div 
              className={cn(
                "h-2 rounded-full transition-all duration-300",
                isOutOfStock ? "bg-red-500" : isLowStock ? "bg-yellow-500" : "bg-green-500"
              )}
              style={{ 
                width: `${Math.min(100, (product.stockQuantity / 50) * 100)}%` 
              }}
            />
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
                onClick={() => onView(product.id)}
                className="flex-1"
              >
                <Eye className="h-4 w-4 mr-2" />
                查看
              </Button>
            )}
            
            {onEdit && (
              <Button
                variant="outline"
                size="sm"
                onClick={() => onEdit(product.id)}
                className="flex-1"
              >
                <Edit className="h-4 w-4 mr-2" />
                編輯
              </Button>
            )}
            
            {onDelete && (
              <Button
                variant="destructive"
                size="sm"
                onClick={() => onDelete(product.id)}
                className="flex-1"
              >
                <Trash2 className="h-4 w-4 mr-2" />
                刪除
              </Button>
            )}
          </div>
        </CardFooter>
      )}
    </Card>
  )
}
