import { Button, EmptyState } from '@repo/ui'
import { ShoppingCart } from 'lucide-react'
import Link from 'next/link'

export function CartEmpty() {
    return (
        <div className="py-16">
            <EmptyState
                icon={ShoppingCart}
                title="購物車是空的"
                description="快去挑選喜歡的商品吧"
            />
            <div className="flex justify-center mt-6">
                <Link href="/products">
                    <Button className="cursor-pointer">繼續購物</Button>
                </Link>
            </div>
        </div>
    )
}
