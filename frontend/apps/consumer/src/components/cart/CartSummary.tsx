import type { CartItem } from '@repo/api-client'
import { Button } from '@repo/ui'
import Link from 'next/link'

function formatPrice(amount: number) {
    return `NT$ ${amount.toLocaleString()}`
}

export function CartSummary({ items }: { items: CartItem[] }) {
    const totalItems = items.reduce((sum, i) => sum + i.quantity, 0)
    const totalAmount = items.reduce((sum, i) => sum + i.unitPrice.amount * i.quantity, 0)

    return (
        <div className="bg-white rounded-xl border border-stone-100 p-6 space-y-4">
            <h2 className="text-lg font-semibold text-foreground">訂單摘要</h2>
            <div className="space-y-2 text-sm">
                <div className="flex justify-between text-muted-foreground">
                    <span>商品數量</span>
                    <span>{totalItems} 件</span>
                </div>
                <div className="flex justify-between font-bold text-foreground text-base pt-2 border-t border-stone-100">
                    <span>合計</span>
                    <span className="text-primary">{formatPrice(totalAmount)}</span>
                </div>
            </div>
            <Link href="/checkout" className="block">
                <Button className="w-full cursor-pointer" size="lg">
                    前往結帳
                </Button>
            </Link>
            <Link href="/products" className="block">
                <Button variant="outline" className="w-full cursor-pointer" size="sm">
                    繼續購物
                </Button>
            </Link>
        </div>
    )
}
