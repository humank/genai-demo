'use client'

import { useStats } from '@repo/api-client/hooks'
import { Button } from '@repo/ui'
import { DollarSign, Package, RefreshCw, ShoppingCart, Users } from 'lucide-react'
import { StatsCard } from './StatsCard'

export function KPIGrid() {
    const { data, isLoading, error, refetch } = useStats()

    if (error) {
        return (
            <div className="flex flex-col items-center justify-center p-8 text-center bg-red-50 dark:bg-red-950/20 rounded-lg">
                <p className="text-red-500 mb-3">統計數據載入失敗</p>
                <Button variant="outline" size="sm" onClick={() => refetch()}>
                    <RefreshCw className="h-4 w-4 mr-2" /> 重試
                </Button>
            </div>
        )
    }

    const stats = [
        {
            title: '總訂單數',
            value: data?.totalOrders ?? 0,
            change: { value: '↗ +12%', type: 'increase' as const, period: '比上週' },
            icon: ShoppingCart,
            color: 'blue' as const,
        },
        {
            title: '總營收',
            value: data?.totalRevenue
                ? `NT$ ${Number(data.totalRevenue).toLocaleString()}`
                : 'NT$ 0',
            change: { value: '↗ +8%', type: 'increase' as const, period: '比上週' },
            icon: DollarSign,
            color: 'green' as const,
        },
        {
            title: '總客戶數',
            value: data?.totalCustomers ?? 0,
            change: { value: '↗ +5%', type: 'increase' as const, period: '比上月' },
            icon: Users,
            color: 'purple' as const,
        },
        {
            title: '庫存總量',
            value: data?.totalInventory ?? 0,
            change: { value: '→ 正常', type: 'neutral' as const, period: '庫存狀態' },
            icon: Package,
            color: 'orange' as const,
        },
    ]

    return (
        <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-4">
            {stats.map((s) => (
                <StatsCard key={s.title} {...s} loading={isLoading} />
            ))}
        </div>
    )
}
