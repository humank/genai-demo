'use client'

import React, { useState, useEffect } from 'react'
import { Dialog, DialogContent, DialogHeader, DialogTitle, DialogFooter } from '@/components/ui/dialog'
import { Button } from '@/components/ui/button'
import { Input } from '@/components/ui/input'
import { Textarea } from '@/components/ui/textarea'
import { Label } from '@/components/ui/label'
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from '@/components/ui/select'
import { Product } from '@/types/domain'
import { useUpdateProduct } from '@/hooks/useApi'
import { toast } from 'react-hot-toast'

interface EditProductDialogProps {
  product: Product | null
  open: boolean
  onOpenChange: (open: boolean) => void
}

export const EditProductDialog: React.FC<EditProductDialogProps> = ({
  product,
  open,
  onOpenChange,
}) => {
  const [formData, setFormData] = useState({
    name: '',
    description: '',
    price: '',
    currency: 'TWD',
    category: '',
  })

  const updateProductMutation = useUpdateProduct()

  useEffect(() => {
    if (product) {
      setFormData({
        name: product.name || '',
        description: product.description || '',
        price: product.price?.amount?.toString() || '',
        currency: product.price?.currency || 'TWD',
        category: product.category || '',
      })
    }
  }, [product])

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault()
    
    if (!product) return

    try {
      await updateProductMutation.mutateAsync({
        productId: product.id,
        product: {
          name: formData.name,
          description: formData.description,
          price: parseFloat(formData.price),
          currency: formData.currency,
          category: formData.category,
        }
      })

      toast.success('產品更新成功！')
      onOpenChange(false)
    } catch (error) {
      toast.error('更新產品時發生錯誤')
      console.error('Update product error:', error)
    }
  }

  const handleInputChange = (field: string, value: string) => {
    setFormData(prev => ({
      ...prev,
      [field]: value
    }))
  }

  return (
    <Dialog open={open} onOpenChange={onOpenChange}>
      <DialogContent className="sm:max-w-[500px]">
        <DialogHeader>
          <DialogTitle>編輯產品</DialogTitle>
        </DialogHeader>
        
        <form onSubmit={handleSubmit} className="space-y-4">
          <div className="space-y-2">
            <Label htmlFor="name">產品名稱</Label>
            <Input
              id="name"
              value={formData.name}
              onChange={(e) => handleInputChange('name', e.target.value)}
              placeholder="請輸入產品名稱"
              required
            />
          </div>

          <div className="space-y-2">
            <Label htmlFor="description">產品描述</Label>
            <Textarea
              id="description"
              value={formData.description}
              onChange={(e) => handleInputChange('description', e.target.value)}
              placeholder="請輸入產品描述"
              rows={3}
            />
          </div>

          <div className="grid grid-cols-2 gap-4">
            <div className="space-y-2">
              <Label htmlFor="price">價格</Label>
              <Input
                id="price"
                type="number"
                step="0.01"
                min="0"
                value={formData.price}
                onChange={(e) => handleInputChange('price', e.target.value)}
                placeholder="0.00"
                required
              />
            </div>

            <div className="space-y-2">
              <Label htmlFor="currency">貨幣</Label>
              <Select value={formData.currency} onValueChange={(value) => handleInputChange('currency', value)}>
                <SelectTrigger>
                  <SelectValue />
                </SelectTrigger>
                <SelectContent>
                  <SelectItem value="TWD">TWD</SelectItem>
                  <SelectItem value="USD">USD</SelectItem>
                  <SelectItem value="EUR">EUR</SelectItem>
                </SelectContent>
              </Select>
            </div>
          </div>

          <div className="space-y-2">
            <Label htmlFor="category">分類</Label>
            <Select value={formData.category} onValueChange={(value) => handleInputChange('category', value)}>
              <SelectTrigger>
                <SelectValue placeholder="選擇分類" />
              </SelectTrigger>
              <SelectContent>
                <SelectItem value="ELECTRONICS">電子產品</SelectItem>
                <SelectItem value="CLOTHING">服飾</SelectItem>
                <SelectItem value="FOOD">食品</SelectItem>
                <SelectItem value="BOOKS">書籍</SelectItem>
                <SelectItem value="HOME">居家用品</SelectItem>
                <SelectItem value="SPORTS">運動用品</SelectItem>
                <SelectItem value="GENERAL">一般商品</SelectItem>
              </SelectContent>
            </Select>
          </div>

          <DialogFooter>
            <Button type="button" variant="outline" onClick={() => onOpenChange(false)}>
              取消
            </Button>
            <Button type="submit" disabled={updateProductMutation.isPending}>
              {updateProductMutation.isPending ? '更新中...' : '更新產品'}
            </Button>
          </DialogFooter>
        </form>
      </DialogContent>
    </Dialog>
  )
}