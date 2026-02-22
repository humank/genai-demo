import { Button } from '@repo/ui'
import { Home, ShoppingBag } from 'lucide-react'
import Link from 'next/link'

export default function NotFound() {
    return (
        <div className="min-h-screen flex items-center justify-center bg-background">
            <div className="text-center space-y-6 p-8 max-w-md">
                <div className="space-y-2">
                    <h1 className="text-6xl font-bold text-primary">404</h1>
                    <h2 className="text-2xl font-semibold text-foreground">
                        找不到頁面
                    </h2>
                    <p className="text-muted-foreground">
                        抱歉，您訪問的頁面不存在。可能是頁面已被移除，或者網址輸入有誤。
                    </p>
                </div>

                <div className="flex flex-col sm:flex-row gap-3 justify-center">
                    <Button asChild className="cursor-pointer">
                        <Link href="/" className="flex items-center gap-2">
                            <Home className="h-4 w-4" aria-hidden="true" />
                            返回首頁
                        </Link>
                    </Button>
                    <Button variant="outline" asChild className="cursor-pointer">
                        <Link href="/products" className="flex items-center gap-2">
                            <ShoppingBag className="h-4 w-4" aria-hidden="true" />
                            瀏覽商品
                        </Link>
                    </Button>
                </div>
            </div>
        </div>
    )
}
