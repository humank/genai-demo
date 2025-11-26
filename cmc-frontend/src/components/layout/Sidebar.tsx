'use client'

import { cn } from '@/lib/utils'
import {
    CreditCard,
    LayoutDashboard,
    Package,
    Settings,
    ShoppingCart,
    TrendingUp,
    Truck,
    Users,
    LogOut
} from 'lucide-react'
import Link from 'next/link'
import { usePathname } from 'next/navigation'

const navigation = [
    { name: '總覽', href: '/', icon: LayoutDashboard },
    { name: '訂單管理', href: '/orders', icon: ShoppingCart },
    { name: '商品管理', href: '/products', icon: Package },
    { name: '客戶管理', href: '/customers', icon: Users },
    { name: '支付管理', href: '/payments', icon: CreditCard },
    { name: '物流配送', href: '/delivery', icon: Truck },
    { name: '促銷活動', href: '/promotions', icon: TrendingUp },
]

interface SidebarProps extends React.HTMLAttributes<HTMLDivElement> { }

export function Sidebar({ className }: SidebarProps) {
    const pathname = usePathname()

    return (
        <div className={cn("pb-12 min-h-screen border-r bg-gray-50/40 dark:bg-gray-900/40", className)}>
            <div className="space-y-4 py-4">
                <div className="px-3 py-2">
                    <Link href="/" className="flex items-center pl-2 mb-9">
                        <div className="w-8 h-8 bg-blue-600 rounded-lg flex items-center justify-center mr-3">
                            <span className="text-white font-bold text-sm">C</span>
                        </div>
                        <div className="flex flex-col">
                            <span className="text-xl font-bold text-gray-900 dark:text-white">
                                CMC
                            </span>
                            <span className="text-xs text-gray-500 -mt-1">
                                商務管理中心
                            </span>
                        </div>
                    </Link>
                    <div className="space-y-1">
                        {navigation.map((item) => {
                            const isActive = pathname === item.href
                            const Icon = item.icon

                            return (
                                <Link
                                    key={item.href}
                                    href={item.href}
                                    className={cn(
                                        "flex items-center gap-3 rounded-lg px-3 py-2 text-sm font-medium transition-all hover:text-blue-600 dark:hover:text-blue-400",
                                        isActive
                                            ? "bg-blue-100 text-blue-600 dark:bg-blue-900/20 dark:text-blue-400"
                                            : "text-gray-500 hover:bg-gray-100 dark:text-gray-400 dark:hover:bg-gray-800"
                                    )}
                                >
                                    <Icon className="h-4 w-4" />
                                    {item.name}
                                </Link>
                            )
                        })}
                    </div>
                </div>

                <div className="px-3 py-2">
                    <h2 className="mb-2 px-4 text-xs font-semibold tracking-tight text-gray-500 dark:text-gray-400">
                        系統設置
                    </h2>
                    <div className="space-y-1">
                        <Link
                            href="/settings"
                            className="flex items-center gap-3 rounded-lg px-3 py-2 text-sm font-medium text-gray-500 transition-all hover:text-blue-600 hover:bg-gray-100 dark:text-gray-400 dark:hover:text-blue-400 dark:hover:bg-gray-800"
                        >
                            <Settings className="h-4 w-4" />
                            設定
                        </Link>
                        <button
                            className="w-full flex items-center gap-3 rounded-lg px-3 py-2 text-sm font-medium text-red-500 transition-all hover:text-red-600 hover:bg-red-50 dark:hover:bg-red-900/10"
                        >
                            <LogOut className="h-4 w-4" />
                            登出
                        </button>
                    </div>
                </div>
            </div>
        </div>
    )
}
