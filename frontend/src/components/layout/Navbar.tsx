'use client'

import React, { useState } from 'react'
import Link from 'next/link'
import { usePathname } from 'next/navigation'
import { Button } from '@/components/ui/button'
import { 
  Menu, 
  X, 
  Settings, 
  Bell, 
  User, 
  Search,
  Moon,
  Sun,
  ShoppingCart,
  Package,
  Users,
  CreditCard,
  Truck,
  TrendingUp
} from 'lucide-react'
import { cn } from '@/lib/utils'

const navigation = [
  { name: '首頁', href: '/', icon: null },
  { name: '訂單', href: '/orders', icon: ShoppingCart },
  { name: '商品', href: '/products', icon: Package },
  { name: '客戶', href: '/customers', icon: Users },
  { name: '支付', href: '/payments', icon: CreditCard },
  { name: '配送', href: '/delivery', icon: Truck },
  { name: '促銷', href: '/promotions', icon: TrendingUp },
]

export const Navbar: React.FC = () => {
  const [mobileMenuOpen, setMobileMenuOpen] = useState(false)
  const [darkMode, setDarkMode] = useState(false)
  const pathname = usePathname()

  const toggleDarkMode = () => {
    setDarkMode(!darkMode)
    document.documentElement.classList.toggle('dark')
  }

  return (
    <nav className="nav-modern">
      <div className="container-modern">
        <div className="flex justify-between items-center h-16">
          {/* Logo 和品牌 */}
          <div className="flex items-center space-x-4">
            <Link href="/" className="flex items-center space-x-3 group">
              <div className="w-8 h-8 gradient-bg rounded-lg flex items-center justify-center">
                <span className="text-white font-bold text-sm">G</span>
              </div>
              <div className="flex flex-col">
                <span className="text-xl font-bold text-gradient group-hover:scale-105 transition-transform">
                  GenAI Demo
                </span>
                <span className="text-xs text-muted-foreground -mt-1">
                  電商管理系統
                </span>
              </div>
            </Link>
          </div>

          {/* 桌面導航 */}
          <div className="hidden md:flex items-center space-x-1">
            {navigation.map((item) => {
              const isActive = pathname === item.href
              const Icon = item.icon
              
              return (
                <Link
                  key={item.name}
                  href={item.href}
                  className={cn(
                    "flex items-center space-x-2 px-3 py-2 rounded-lg text-sm font-medium transition-all duration-200",
                    isActive
                      ? "bg-primary/10 text-primary border border-primary/20"
                      : "text-muted-foreground hover:text-foreground hover:bg-muted/50"
                  )}
                >
                  {Icon && <Icon className="h-4 w-4" />}
                  <span>{item.name}</span>
                </Link>
              )
            })}
          </div>

          {/* 右側操作區 */}
          <div className="flex items-center space-x-3">
            {/* 搜尋按鈕 */}
            <Button variant="ghost" size="sm" className="hidden sm:flex">
              <Search className="h-4 w-4" />
            </Button>

            {/* 通知按鈕 */}
            <Button variant="ghost" size="sm" className="relative">
              <Bell className="h-4 w-4" />
              <span className="absolute -top-1 -right-1 h-3 w-3 bg-destructive rounded-full text-[10px] text-destructive-foreground flex items-center justify-center">
                3
              </span>
            </Button>

            {/* 主題切換 */}
            <Button 
              variant="ghost" 
              size="sm" 
              onClick={toggleDarkMode}
              className="hidden sm:flex"
            >
              {darkMode ? <Sun className="h-4 w-4" /> : <Moon className="h-4 w-4" />}
            </Button>

            {/* 用戶菜單 */}
            <Button variant="ghost" size="sm" className="hidden sm:flex">
              <User className="h-4 w-4" />
            </Button>

            {/* 設定 */}
            <Button variant="outline" size="sm" className="hidden sm:flex">
              <Settings className="h-4 w-4 mr-2" />
              設定
            </Button>

            {/* 移動端菜單按鈕 */}
            <Button
              variant="ghost"
              size="sm"
              className="md:hidden"
              onClick={() => setMobileMenuOpen(!mobileMenuOpen)}
            >
              {mobileMenuOpen ? <X className="h-5 w-5" /> : <Menu className="h-5 w-5" />}
            </Button>
          </div>
        </div>

        {/* 移動端菜單 */}
        {mobileMenuOpen && (
          <div className="md:hidden animate-slide-up">
            <div className="px-2 pt-2 pb-3 space-y-1 bg-card/50 backdrop-blur-sm rounded-lg mt-2 border border-border/50">
              {navigation.map((item) => {
                const isActive = pathname === item.href
                const Icon = item.icon
                
                return (
                  <Link
                    key={item.name}
                    href={item.href}
                    className={cn(
                      "flex items-center space-x-3 px-3 py-2 rounded-lg text-sm font-medium transition-all duration-200",
                      isActive
                        ? "bg-primary/10 text-primary border border-primary/20"
                        : "text-muted-foreground hover:text-foreground hover:bg-muted/50"
                    )}
                    onClick={() => setMobileMenuOpen(false)}
                  >
                    {Icon && <Icon className="h-4 w-4" />}
                    <span>{item.name}</span>
                  </Link>
                )
              })}
              
              {/* 移動端額外選項 */}
              <div className="border-t border-border/50 pt-2 mt-2">
                <button
                  onClick={toggleDarkMode}
                  className="flex items-center space-x-3 px-3 py-2 rounded-lg text-sm font-medium text-muted-foreground hover:text-foreground hover:bg-muted/50 w-full"
                >
                  {darkMode ? <Sun className="h-4 w-4" /> : <Moon className="h-4 w-4" />}
                  <span>{darkMode ? '淺色模式' : '深色模式'}</span>
                </button>
                
                <Link
                  href="/profile"
                  className="flex items-center space-x-3 px-3 py-2 rounded-lg text-sm font-medium text-muted-foreground hover:text-foreground hover:bg-muted/50"
                  onClick={() => setMobileMenuOpen(false)}
                >
                  <User className="h-4 w-4" />
                  <span>個人資料</span>
                </Link>
              </div>
            </div>
          </div>
        )}
      </div>
    </nav>
  )
}
