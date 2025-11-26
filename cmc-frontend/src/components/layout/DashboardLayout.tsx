'use client'

import { useState } from 'react'
import { Sidebar } from './Sidebar'
import { Header } from './Header'

export default function DashboardLayout({
    children,
}: {
    children: React.ReactNode
}) {
    const [sidebarOpen, setSidebarOpen] = useState(false)

    return (
        <div className="flex min-h-screen flex-col bg-gray-100/40 dark:bg-gray-900">
            {/* Mobile Sidebar Overlay */}
            {sidebarOpen && (
                <div
                    className="fixed inset-0 z-40 bg-black/50 md:hidden"
                    onClick={() => setSidebarOpen(false)}
                />
            )}

            {/* Sidebar */}
            <div className={`
        fixed inset-y-0 left-0 z-50 w-64 bg-white transition-transform duration-200 ease-in-out md:translate-x-0 dark:bg-gray-950
        ${sidebarOpen ? 'translate-x-0' : '-translate-x-full'}
      `}>
                <Sidebar className="h-full" />
            </div>

            {/* Main Content */}
            <div className="flex flex-col md:pl-64">
                <Header onMenuClick={() => setSidebarOpen(!sidebarOpen)} />
                <main className="flex-1 p-4 md:p-6">
                    {children}
                </main>
            </div>
        </div>
    )
}
