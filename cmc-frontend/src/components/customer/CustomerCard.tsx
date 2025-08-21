'use client'

import React from 'react'
import { Card, CardContent, CardFooter, CardHeader } from '@/components/ui/card'
import { Button } from '@/components/ui/button'
import { Badge } from '@/components/ui/badge'
import { Customer } from '@/types/domain'
import { 
  User, 
  Mail, 
  Phone, 
  MapPin, 
  Eye, 
  Edit, 
  MessageCircle,
  Star,
  Calendar,
  ShoppingBag
} from 'lucide-react'
import { cn } from '@/lib/utils'

interface CustomerCardProps {
  customer: Customer
  onView?: (customerId: string) => void
  onEdit?: (customerId: string) => void
  onMessage?: (customerId: string) => void
  showActions?: boolean
  orderCount?: number
  totalSpent?: number
  lastOrderDate?: string
}

const membershipColors = {
  'Bronze': 'bg-amber-100 text-amber-800 border-amber-200',
  'Silver': 'bg-gray-100 text-gray-800 border-gray-200',
  'Gold': 'bg-yellow-100 text-yellow-800 border-yellow-200',
  'Platinum': 'bg-purple-100 text-purple-800 border-purple-200',
  'Diamond': 'bg-blue-100 text-blue-800 border-blue-200'
}

export const CustomerCard: React.FC<CustomerCardProps> = ({
  customer,
  onView,
  onEdit,
  onMessage,
  showActions = true,
  orderCount = 0,
  totalSpent = 0,
  lastOrderDate
}) => {
  const membershipColor = membershipColors[customer.membershipLevel as keyof typeof membershipColors] || membershipColors.Bronze

  return (
    <Card className="modern-card modern-card-hover group">
      <CardHeader className="pb-4">
        <div className="flex items-start justify-between">
          <div className="flex items-center space-x-3">
            <div className="w-12 h-12 bg-gradient-to-br from-primary/20 to-accent/20 rounded-full flex items-center justify-center border border-primary/20">
              <User className="h-6 w-6 text-primary" />
            </div>
            
            <div className="flex-1 min-w-0">
              <h3 className="font-semibold text-foreground group-hover:text-primary transition-colors">
                {customer.name}
              </h3>
              <p className="text-sm text-muted-foreground">
                ID: {customer.id.slice(-8)}
              </p>
            </div>
          </div>
          
          <Badge className={cn("text-xs", membershipColor)}>
            {customer.membershipLevel}
          </Badge>
        </div>
      </CardHeader>

      <CardContent className="space-y-4">
        {/* 聯絡資訊 */}
        <div className="space-y-3">
          <div className="flex items-center space-x-3 text-sm">
            <Mail className="h-4 w-4 text-muted-foreground flex-shrink-0" />
            <span className="text-muted-foreground truncate">{customer.email}</span>
          </div>
          
          <div className="flex items-center space-x-3 text-sm">
            <Phone className="h-4 w-4 text-muted-foreground flex-shrink-0" />
            <span className="text-muted-foreground">{customer.phone}</span>
          </div>
          
          <div className="flex items-start space-x-3 text-sm">
            <MapPin className="h-4 w-4 text-muted-foreground flex-shrink-0 mt-0.5" />
            <span className="text-muted-foreground line-clamp-2">{customer.address}</span>
          </div>
        </div>

        {/* 統計信息 */}
        <div className="grid grid-cols-2 gap-4 pt-4 border-t border-border/50">
          <div className="text-center">
            <div className="flex items-center justify-center space-x-1 mb-1">
              <ShoppingBag className="h-4 w-4 text-primary" />
              <span className="text-lg font-bold text-foreground">{orderCount}</span>
            </div>
            <p className="text-xs text-muted-foreground">訂單數量</p>
          </div>
          
          <div className="text-center">
            <div className="flex items-center justify-center space-x-1 mb-1">
              <Star className="h-4 w-4 text-yellow-500" />
              <span className="text-lg font-bold text-foreground">
                NT$ {totalSpent.toLocaleString()}
              </span>
            </div>
            <p className="text-xs text-muted-foreground">總消費</p>
          </div>
        </div>

        {/* 最後訂單日期 */}
        {lastOrderDate && (
          <div className="flex items-center space-x-2 text-sm bg-muted/30 rounded-lg p-3">
            <Calendar className="h-4 w-4 text-muted-foreground" />
            <span className="text-muted-foreground">最後訂單:</span>
            <span className="font-medium text-foreground">{lastOrderDate}</span>
          </div>
        )}

        {/* 會員等級進度 */}
        <div className="space-y-2">
          <div className="flex justify-between text-xs text-muted-foreground">
            <span>會員等級進度</span>
            <span>{Math.min(100, (totalSpent / 100000) * 100).toFixed(0)}%</span>
          </div>
          <div className="w-full bg-muted rounded-full h-2">
            <div 
              className="h-2 rounded-full bg-gradient-to-r from-primary to-accent transition-all duration-300"
              style={{ 
                width: `${Math.min(100, (totalSpent / 100000) * 100)}%` 
              }}
            />
          </div>
          <p className="text-xs text-muted-foreground">
            距離下一等級還需消費 NT$ {Math.max(0, 100000 - totalSpent).toLocaleString()}
          </p>
        </div>
      </CardContent>

      {showActions && (
        <CardFooter className="pt-3 border-t">
          <div className="flex space-x-2 w-full">
            {onView && (
              <Button
                variant="outline"
                size="sm"
                onClick={() => onView(customer.id)}
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
                onClick={() => onEdit(customer.id)}
                className="flex-1"
              >
                <Edit className="h-4 w-4 mr-2" />
                編輯
              </Button>
            )}
            
            {onMessage && (
              <Button
                variant="outline"
                size="sm"
                onClick={() => onMessage(customer.id)}
                className="flex-1"
              >
                <MessageCircle className="h-4 w-4 mr-2" />
                聯絡
              </Button>
            )}
          </div>
        </CardFooter>
      )}
    </Card>
  )
}
