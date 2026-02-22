'use client'

import { useOrderStatusStats } from '@repo/api-client/hooks'
import { Button, Card, CardContent } from '@repo/ui'
import { RefreshCw } from 'lucide-react'
import dynamic from 'next/dynamic'

const RechartsBarChart = dynamic(
    () => import('./charts/OrderStatusBarChart'),
    { ssr: false, loading: () => <div className="h-64 animate-pulse bg-muted rounded" /> }
)

export function OrderStatusChart() {
    const { data, isLoading, error, refetch } = useOrderStatusStats()

    if (error) {
        return (
            <Card>
                <CardContent className="flex flex-col items-center justify-center p-8 text-center">
                    <p className="text-red-500 mb-3">訂單狀態數據載入失敗</p>
                    <Button variant="outline" size="sm" onClick={() => refetch()}>
                        <RefreshCw className="h-4 w-4 mr-2" /> 重試
                    </Button>
                </CardContent>
            </Card>
        )
    }

    if (isLoading) {
        return (
            <Card>
                <CardContent className="p-6">
                    <div className="h-5 w-32 bg-muted rounded mb-4 animate-pulse" />
                    <div className="h-64 bg-muted rounded animate-pulse" />
                </CardContent>
            </Card>
        )
    }

    return (
        <Card>
            <CardContent className="p-6">
                <h3 className="text-lg font-semibold text-foreground mb-4">訂單狀態分佈</h3>
                <RechartsBarChart data={data || []} />
            </CardContent>
        </Card>
    )
}
