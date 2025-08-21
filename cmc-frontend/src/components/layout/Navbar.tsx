'use client'

import { Button } from '@/components/ui/button'
import { cn } from '@/lib/utils'
import {
  Bell,
  CreditCard,
  Menu,
  Moon,
  Package,
  Search,
  Settings,
  ShoppingCart,
  Sun,
  TrendingUp,
  Truck,
  User,
  Users,
  X
} from 'lucide-react'
import Link from 'next/link'
import { usePathname } from 'next/navigation'
import React, { useState } from 'react'

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
    <nav className="bg-white border-b border-gray-200 sticky top-0 z-50">
      <div className="max-w-7xl mx-auto px-4">
        <div className="flex justify-between items-center h-16">
          {/* Logo */}
          <div className="flex items-center space-x-4">
            <Link href="/" className="flex items-center space-x-3">
              <div className="w-8 h-8 bg-blue-600 rounded-lg flex items-center justify-center">
                <span className="text-white font-bold text-sm">C</span>
              </div>
              <div className="flex flex-col">
                <span className="text-xl font-bold text-gray-900">
                  CMC
                </span>
                <span className="text-xs text-gray-500 -mt-1">
                  商務管理中心
                </span>
              </div>
            </Link>
          </div>

          {/* Desktop Navigation */}
          <div className="hidden md:flex items-center space-x-1">
            {navigation.map((item) => {
              const isActive = pathname === item.href
              const Icon = item.icon

              return (
                <Link
                  key={item.name}
                  href={item.href}
                  className={cn(
                    "flex items-center space-x-2 px-3 py-2 rounded-md text-sm font-medium transition-colors",
                    isActive
                      ? "bg-blue-100 text-blue-700"
                      : "text-gray-600 hover:text-gray-900 hover:bg-gray-100"
                  )}
                >
                  {Icon && <Icon className="h-4 w-4" />}
                  <span>{item.name}</span>
                </Link>
              )
            })}
          </div>

          {/* Right Actions */}
          <div className="flex items-center space-x-3">
            <Button variant="ghost" size="sm" className="hidden sm:flex">
              <Search className="h-4 w-4" />
            </Button>

            <Button variant="ghost" size="sm" className="relative">
              <Bell className="h-4 w-4" />
              <span className="absolute -top-1 -right-1 h-3 w-3 bg-red-500 rounded-full text-[10px] text-white flex items-center justify-center">
                3
              </span>
            </Button>

            <Button
              variant="ghost"
              size="sm"
              onClick={toggleDarkMode}
              className="hidden sm:flex"
            >
              {darkMode ? <Sun className="h-4 w-4" /> : <Moon className="h-4 w-4" />}
            </Button>

            <Button variant="ghost" size="sm" className="hidden sm:flex">
              <User className="h-4 w-4" />
            </Button>

            <Button variant="outline" size="sm" className="hidden sm:flex">
              <Settings className="h-4 w-4 mr-2" />
              設定
            </Button>

            {/* Mobile menu button */}
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

        {/* Mobile Menu */}
        {mobileMenuOpen && (
          <div className="md:hidden">
            <div className="px-2 pt-2 pb-3 space-y-1 bg-white rounded-lg mt-2 border border-gray-200">
              {navigation.map((item) => {
                const isActive = pathname === item.href
                const Icon = item.icon

                return (
                  <Link
                    key={item.name}
                    href={item.href}
                    className={cn(
                      "flex items-center space-x-3 px-3 py-2 rounded-md text-sm font-medium transition-colors",
                      isActive
                        ? "bg-blue-100 text-blue-700"
                        : "text-gray-600 hover:text-gray-900 hover:bg-gray-100"
                    )}
                    onClick={() => setMobileMenuOpen(false)}
                  >
                    {Icon && <Icon className="h-4 w-4" />}
                    <span>{item.name}</span>
                  </Link>
                )
              })}

              <div className="border-t border-gray-200 pt-2 mt-2">
                <button
                  onClick={toggleDarkMode}
                  className="flex items-center space-x-3 px-3 py-2 rounded-md text-sm font-medium text-gray-600 hover:text-gray-900 hover:bg-gray-100 w-full"
                >
                  {darkMode ? <Sun className="h-4 w-4" /> : <Moon className="h-4 w-4" />}
                  <span>{darkMode ? '淺色模式' : '深色模式'}</span>
                </button>

                <Link
                  href="/profile"
                  className="flex items-center space-x-3 px-3 py-2 rounded-md text-sm font-medium text-gray-600 hover:text-gray-900 hover:bg-gray-100"
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