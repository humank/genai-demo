import Link from 'next/link'

const footerLinks = [
    {
        title: '購物指南',
        links: [
            { label: '商品瀏覽', href: '/products' },
            { label: '購物車', href: '/cart' },
            { label: '我的訂單', href: '/orders' },
        ],
    },
    {
        title: '客戶服務',
        links: [
            { label: '常見問題', href: '#' },
            { label: '退換貨政策', href: '#' },
            { label: '聯絡我們', href: '#' },
        ],
    },
    {
        title: '關於我們',
        links: [
            { label: '公司簡介', href: '#' },
            { label: '隱私權政策', href: '#' },
            { label: '服務條款', href: '#' },
        ],
    },
]

export function Footer() {
    return (
        <footer className="border-t border-stone-200 bg-stone-50 mt-12">
            <div className="container-shop py-12">
                <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-8">
                    {/* Brand */}
                    <div>
                        <h3 className="text-lg font-bold text-primary mb-3">電商平台</h3>
                        <p className="text-sm text-muted-foreground">
                            探索精選商品，享受便捷的線上購物體驗。
                        </p>
                    </div>

                    {/* Link groups */}
                    {footerLinks.map((group) => (
                        <div key={group.title}>
                            <h4 className="text-sm font-semibold text-foreground mb-3">
                                {group.title}
                            </h4>
                            <ul className="space-y-2">
                                {group.links.map((link) => (
                                    <li key={link.label}>
                                        <Link
                                            href={link.href}
                                            className="text-sm text-muted-foreground hover:text-foreground transition-colors cursor-pointer"
                                        >
                                            {link.label}
                                        </Link>
                                    </li>
                                ))}
                            </ul>
                        </div>
                    ))}
                </div>

                <div className="mt-10 pt-6 border-t border-stone-200 text-center text-sm text-muted-foreground">
                    © {new Date().getFullYear()} 電商平台。保留所有權利。
                </div>
            </div>
        </footer>
    )
}
