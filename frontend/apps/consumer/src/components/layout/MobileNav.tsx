'use client'

import {
    Sheet, SheetClose, SheetContent, SheetHeader, SheetTitle, SheetTrigger,
} from '@repo/ui'
import { ClipboardList, Home, Menu, Package, ShoppingCart } from 'lucide-react'
import Link from 'next/link'
import { usePathname } from 'next/navigation'
import { SearchBar } from './SearchBar'

const navLinks = [
    { href: '/', label: '首頁', icon: Home },
    { href: '/products', label: '商品', icon: Package },
    { href: '/cart', label: '購物車', icon: ShoppingCart },
    { href: '/orders', label: '我的訂單', icon: ClipboardList },
]

export function MobileNav() {
    const pathname = usePathname()

    return (
        <Sheet>
            <SheetTrigger asChild>
                <button
                    className="lg:hidden p-2 text-foreground hover:text-primary transition-colors cursor-pointer"
                    aria-label="開啟選單"
                >
                    <Menu className="h-5 w-5" />
                </button>
            </SheetTrigger>
            <SheetContent side="left" className="w-72 p-0">
                <SheetHeader className="p-6 border-b">
                    <SheetTitle className="text-lg font-bold text-primary">
                        電商平台
                    </SheetTitle>
                </SheetHeader>
                <div className="p-4">
                    <SearchBar className="mb-4" />
                    <nav className="flex flex-col gap-1">
                        {navLinks.map(({ href, label, icon: Icon }) => {
                            const isActive = pathname === href
                            return (
                                <SheetClose asChild key={href}>
                                    <Link
                                        href={href}
                                        className={`flex items-center gap-3 px-4 py-3 rounded-xl text-sm font-medium transition-colors cursor-pointer ${isActive
                                                ? 'bg-primary/10 text-primary'
                                                : 'text-foreground hover:bg-muted'
                                            }`}
                                    >
                                        <Icon className="h-4 w-4" />
                                        {label}
                                    </Link>
                                </SheetClose>
                            )
                        })}
                    </nav>
                </div>
            </SheetContent>
        </Sheet>
    )
}
