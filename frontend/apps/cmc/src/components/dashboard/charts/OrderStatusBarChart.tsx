'use client'

import { Bar, BarChart, CartesianGrid, Cell, ResponsiveContainer, Tooltip, XAxis, YAxis } from 'recharts'

interface OrderStatusData {
    status: string
    count: number
}

const STATUS_LABELS: Record<string, string> = {
    CREATED: '已建立',
    PENDING: '待處理',
    CONFIRMED: '已確認',
    SHIPPED: '已出貨',
    DELIVERED: '已送達',
    COMPLETED: '已完成',
    CANCELLED: '已取消',
}

const STATUS_COLORS: Record<string, string> = {
    CREATED: '#94a3b8',
    PENDING: '#f59e0b',
    CONFIRMED: '#3b82f6',
    SHIPPED: '#8b5cf6',
    DELIVERED: '#06b6d4',
    COMPLETED: '#22c55e',
    CANCELLED: '#ef4444',
}

export default function OrderStatusBarChart({ data }: { data: OrderStatusData[] }) {
    const chartData = data.map((d) => ({
        name: STATUS_LABELS[d.status] || d.status,
        count: d.count,
        status: d.status,
    }))

    if (chartData.length === 0) {
        return <div className="h-64 flex items-center justify-center text-muted-foreground">暫無數據</div>
    }

    return (
        <ResponsiveContainer width="100%" height={280}>
            <BarChart data={chartData} margin={{ top: 5, right: 20, left: 0, bottom: 5 }}>
                <CartesianGrid strokeDasharray="3 3" stroke="#e2e8f0" />
                <XAxis dataKey="name" tick={{ fontSize: 12 }} />
                <YAxis allowDecimals={false} tick={{ fontSize: 12 }} />
                <Tooltip
                    formatter={(value) => [`${value} 筆`, '數量']}
                    contentStyle={{ borderRadius: 8, border: '1px solid #e2e8f0' }}
                />
                <Bar dataKey="count" radius={[4, 4, 0, 0]}>
                    {chartData.map((entry) => (
                        <Cell key={entry.status} fill={STATUS_COLORS[entry.status] || '#94a3b8'} />
                    ))}
                </Bar>
            </BarChart>
        </ResponsiveContainer>
    )
}
