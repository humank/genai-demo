import type { CartItem as CartItemType } from '@repo/api-client'
import { CartItem } from './CartItem'

export function CartItemList({ items }: { items: CartItemType[] }) {
    return (
        <div className="space-y-3">
            {items.map((item) => (
                <CartItem key={item.productId} item={item} />
            ))}
        </div>
    )
}
