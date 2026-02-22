import { useCartStore } from '@/lib/stores/cart-store'

function formatPrice(amount: number) {
    return `NT$ ${amount.toLocaleString()}`
}

export function OrderSummary() {
    const items = useCartStore((s) => s.items)
    const totalAmount = items.reduce((sum, i) => sum + i.unitPrice.amount * i.quantity, 0)

    return (
        <div className="bg-white rounded-xl border border-stone-100 p-6 space-y-4">
            <h2 className="text-lg font-semibold text-foreground">訂單明細</h2>
            <div className="space-y-3">
                {items.map((item) => (
                    <div key={item.productId} className="flex justify-between text-sm">
                        <span className="text-foreground truncate mr-4">
                            {item.productName} x {item.quantity}
                        </span>
                        <span className="text-muted-foreground flex-shrink-0">
                            {formatPrice(item.unitPrice.amount * item.quantity)}
                        </span>
                    </div>
                ))}
            </div>
            <div className="flex justify-between font-bold text-base pt-3 border-t border-stone-100">
                <span>合計</span>
                <span className="text-primary">{formatPrice(totalAmount)}</span>
            </div>
        </div>
    )
}
