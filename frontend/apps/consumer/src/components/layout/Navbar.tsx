'use client'

import Link from 'next/link'
import { usePathname } from 'next/navigation'
import { CartBadge } from './CartBadge'
import { MobileNav } from './MobileNav'
import { SearchBar } from './SearchBar'

const navLinks = [
    { href: '/', label: '首頁' },
    { href: '/products', label: '商品' },
    { href: '/orders', label: '我的訂單' },
]

export function Navbar() {
    const pathname = usePathname()

    return (
        <header className="sticky top-0 z-50 w-full bg-white/80 backdrop-blur-md border-b border-stone-100">
            <div className="container-shop flex h-16 items-center justify-between gap-4">
                {/* Left: Mobile menu + Logo */}
                <div className="flex items-center gap-2">
                    <MobileNav />
                    <Link href="/" className="text-xl font-bold text-primary cursor-pointer">
                        電商平台
                    </Link>
                </div>

                {/* Center: Desktop nav links + search */}
                <nav className="hidden lg:flex items-center gap-6 flex-1 justify-center" aria-label="主要導航">
                    {navLinks.map(({ href, label }) => {
                        const isActive = pathname === href || (href !== '/' && pathname.startsWith(href))
                        return (
                            <Link
                                key={href}
                                href={href}
                                className={`text-sm font-medium transition-colors cursor-pointer ${isActive
                                        ? 'text-primary'
                                        : 'text-muted-foreground hover:text-foreground'
                                    }`}
                            >
                                {label}
                            </Link>
                        )
                    })}
                    <SearchBar className="w-64" />
                </nav>

                {/* Right: Cart */}
                <div className="flex items-center gap-2">
                    <CartBadge />
                </div>
            </div>
        </header>
    )
}
