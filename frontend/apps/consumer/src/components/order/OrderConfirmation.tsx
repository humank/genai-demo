import { Button } from '@repo/ui'
import { CheckCircle } from 'lucide-react'
import Link from 'next/link'

export function OrderConfirmation({ orderId }: { orderId: string }) {
    return (
        <div className="container-shop py-16 text-center animate-fade-in">
            <CheckCircle className="h-16 w-16 text-green-500 mx-auto mb-4" />
            <h1 className="text-2xl font-bold text-foreground mb-2">訂單已送出</h1>
            <p className="text-muted-foreground mb-1">感謝您的購買！</p>
            <p className="text-sm text-muted-foreground mb-8">
                訂單編號：<span className="font-mono font-medium text-foreground">{orderId}</span>
            </p>
            <div className="flex justify-center gap-3">
                <Link href={`/orders/${orderId}`}>
                    <Button className="cursor-pointer">查看訂單</Button>
                </Link>
                <Link href="/products">
                    <Button variant="outline" className="cursor-pointer">繼續購物</Button>
                </Link>
            </div>
        </div>
    )
}
