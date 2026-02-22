'use client'

import { Cell, Legend, Pie, PieChart, ResponsiveContainer, Tooltip } from 'recharts'

interface PaymentMethodData {
    method: string
    count: number
    totalAmount: number
}

const COLORS = ['#3b82f6', '#22c55e', '#f59e0b', '#8b5cf6', '#ef4444', '#06b6d4']

const METHOD_LABELS: Record<string, string> = {
    CREDIT_CARD: '信用卡',
    DEBIT_CARD: '金融卡',
    BANK_TRANSFER: '銀行轉帳',
    CASH: '現金',
    LINE_PAY: 'LINE Pay',
    APPLE_PAY: 'Apple Pay',
}

export default function PaymentMethodPieChart({ data }: { data: PaymentMethodData[] }) {
    const chartData = data.map((d) => ({
        name: METHOD_LABELS[d.method] || d.method,
        value: d.count,
        amount: d.totalAmount,
    }))

    if (chartData.length === 0) {
        return <div className="h-64 flex items-center justify-center text-muted-foreground">暫無數據</div>
    }

    return (
        <ResponsiveContainer width="100%" height={280}>
            <PieChart>
                <Pie
                    data={chartData}
                    cx="50%"
                    cy="50%"
                    innerRadius={60}
                    outerRadius={100}
                    paddingAngle={3}
                    dataKey="value"
                    label={({ name, percent }: { name?: string; percent?: number }) => `${name} ${((percent ?? 0) * 100).toFixed(0)}%`}
                    labelLine={{ strokeWidth: 1 }}
                >
                    {chartData.map((_, index) => (
                        <Cell key={index} fill={COLORS[index % COLORS.length]} />
                    ))}
                </Pie>
                <Tooltip
                    formatter={((value: number, _name: string, props: any) => [
                        `${value ?? 0} 筆 (NT$ ${props.payload.amount?.toLocaleString() || 0})`,
                        '數量',
                    ]) as any}
                    contentStyle={{ borderRadius: 8, border: '1px solid #e2e8f0' }}
                />
                <Legend />
            </PieChart>
        </ResponsiveContainer>
    )
}
