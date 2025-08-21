'use client'

import React, { useState } from 'react'
import { Dialog, DialogContent, DialogHeader, DialogTitle, DialogFooter } from '@/components/ui/dialog'
import { Button } from '@/components/ui/button'
import { Input } from '@/components/ui/input'
import { Textarea } from '@/components/ui/textarea'
import { Label } from '@/components/ui/label'
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from '@/components/ui/select'
import { useAdjustInventory } from '@/hooks/useApi'
import { toast } from 'react-hot-toast'

interface AdjustInventoryDialogProps {
  productId: string | null
  productName: string
  currentStock: number
  open: boolean
  onOpenChange: (open: boolean) => void
}

export const AdjustInventoryDialog: React.FC<AdjustInventoryDialogProps> = ({
  productId,
  productName,
  currentStock,
  open,
  onOpenChange,
}) => {
  const [formData, setFormData] = useState({
    type: 'SET',
    quantity: '',
    reason: '',
  })

  const adjustInventoryMutation = useAdjustInventory()

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault()
    
    if (!productId) return

    const quantity = parseInt(formData.quantity)
    if (isNaN(quantity) || quantity < 0) {
      toast.error('請輸入有效的數量')
      return
    }

    try {
      await adjustInventoryMutation.mutateAsync({
        productId,
        adjustment: {
          quantity,
          reason: formData.reason,
          type: formData.type,
        }
      })

      toast.success('庫存調整成功！')
      onOpenChange(false)
      setFormData({ type: 'SET', quantity: '', reason: '' })
    } catch (error) {
      toast.error('調整庫存時發生錯誤')
      console.error('Adjust inventory error:', error)
    }
  }

  const handleInputChange = (field: string, value: string) => {
    setFormData(prev => ({
      ...prev,
      [field]: value
    }))
  }

  const getPreviewText = () => {
    const quantity = parseInt(formData.quantity) || 0
    switch (formData.type) {
      case 'INCREASE':
        return `${currentStock} + ${quantity} = ${currentStock + quantity}`
      case 'DECREASE':
        return `${currentStock} - ${quantity} = ${Math.max(0, currentStock - quantity)}`
      case 'SET':
        return `設定為 ${quantity}`
      default:
        return ''
    }
  }

  return (
    <Dialog open={open} onOpenChange={onOpenChange}>
      <DialogContent className="sm:max-w-[500px]">
        <DialogHeader>
          <DialogTitle>調整庫存</DialogTitle>
          <p className="text-sm text-muted-foreground">
            產品: {productName} | 當前庫存: {currentStock} 件
          </p>
        </DialogHeader>
        
        <form onSubmit={handleSubmit} className="space-y-4">
          <div className="space-y-2">
            <Label htmlFor="type">調整類型</Label>
            <Select value={formData.type} onValueChange={(value) => handleInputChange('type', value)}>
              <SelectTrigger>
                <SelectValue />
              </SelectTrigger>
              <SelectContent>
                <SelectItem value="SET">設定庫存</SelectItem>
                <SelectItem value="INCREASE">增加庫存</SelectItem>
                <SelectItem value="DECREASE">減少庫存</SelectItem>
              </SelectContent>
            </Select>
          </div>

          <div className="space-y-2">
            <Label htmlFor="quantity">數量</Label>
            <Input
              id="quantity"
              type="number"
              min="0"
              value={formData.quantity}
              onChange={(e) => handleInputChange('quantity', e.target.value)}
              placeholder="請輸入數量"
              required
            />
            {formData.quantity && (
              <p className="text-sm text-muted-foreground">
                預覽: {getPreviewText()}
              </p>
            )}
          </div>

          <div className="space-y-2">
            <Label htmlFor="reason">調整原因</Label>
            <Textarea
              id="reason"
              value={formData.reason}
              onChange={(e) => handleInputChange('reason', e.target.value)}
              placeholder="請輸入調整原因（選填）"
              rows={3}
            />
          </div>

          <DialogFooter>
            <Button type="button" variant="outline" onClick={() => onOpenChange(false)}>
              取消
            </Button>
            <Button type="submit" disabled={adjustInventoryMutation.isPending}>
              {adjustInventoryMutation.isPending ? '調整中...' : '確認調整'}
            </Button>
          </DialogFooter>
        </form>
      </DialogContent>
    </Dialog>
  )
}