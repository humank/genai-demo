import Link from 'next/link'
import { Button } from '@/components/ui/button'
import { Home, Search } from 'lucide-react'

export default function NotFound() {
  return (
    <div className="min-h-screen flex items-center justify-center bg-background">
      <div className="text-center space-y-6 p-8">
        <div className="space-y-2">
          <h1 className="text-6xl font-bold text-primary">404</h1>
          <h2 className="text-2xl font-semibold text-foreground">
            頁面未找到
          </h2>
          <p className="text-muted-foreground max-w-md">
            抱歉，您訪問的頁面不存在。可能是頁面已被移動、刪除，或者您輸入了錯誤的網址。
          </p>
        </div>

        <div className="flex flex-col sm:flex-row gap-4 justify-center">
          <Button asChild>
            <Link href="/" className="flex items-center gap-2">
              <Home className="h-4 w-4" />
              返回首頁
            </Link>
          </Button>
          <Button variant="outline" asChild>
            <Link href="/orders" className="flex items-center gap-2">
              <Search className="h-4 w-4" />
              瀏覽訂單
            </Link>
          </Button>
        </div>

        <div className="mt-8 text-sm text-muted-foreground">
          <p>如果您認為這是一個錯誤，請聯繫技術支援。</p>
        </div>
      </div>
    </div>
  )
}