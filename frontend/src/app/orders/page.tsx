'use client'

import React from 'react'
import { Navbar } from '@/components/layout/Navbar'
import { OrderList } from '@/components/order/OrderList'
import { useRouter } from 'next/navigation'

export default function OrdersPage() {
  const router = useRouter()

  const handleCreateOrder = () => {
    router.push('/orders/new')
  }

  const handleViewOrder = (orderId: string) => {
    router.push(`/orders/${orderId}`)
  }

  const handleEditOrder = (orderId: string) => {
    router.push(`/orders/${orderId}/edit`)
  }

  return (
    <div className="min-h-screen bg-gradient-to-br from-background via-background to-muted/20">
      {/* 導航欄 */}
      <Navbar />

      {/* 主要內容 */}
      <main className="container-modern py-8">
        <OrderList
          onCreateOrder={handleCreateOrder}
          onViewOrder={handleViewOrder}
          onEditOrder={handleEditOrder}
        />
      </main>
    </div>
  )
}
