'use client'

import { Button } from '@/components/ui/button'
import { Bell, Menu, Moon, Search, Sun, User } from 'lucide-react'
import { useState } from 'react'

interface HeaderProps {
    onMenuClick: () => void
}

export function Header({ onMenuClick }: HeaderProps) {
    const [darkMode, setDarkMode] = useState(false)

    const toggleDarkMode = () => {
        setDarkMode(!darkMode)
        document.documentElement.classList.toggle('dark')
    }

    return (
        <header className="sticky top-0 z-30 flex h-16 items-center gap-4 border-b bg-white px-6 shadow-sm dark:bg-gray-950 dark:border-gray-800">
            <Button
                variant="ghost"
                size="icon"
                className="md:hidden"
                onClick={onMenuClick}
            >
                <Menu className="h-5 w-5" />
                <span className="sr-only">Toggle menu</span>
            </Button>

            <div className="w-full flex-1">
                <form>
                    <div className="relative">
                        <Search className="absolute left-2.5 top-2.5 h-4 w-4 text-gray-500 dark:text-gray-400" />
                        <input
                            className="flex h-9 w-full rounded-md border border-gray-200 bg-white px-3 py-1 pl-8 text-sm shadow-sm transition-colors file:border-0 file:bg-transparent file:text-sm file:font-medium placeholder:text-gray-500 focus-visible:outline-none focus-visible:ring-1 focus-visible:ring-gray-950 disabled:cursor-not-allowed disabled:opacity-50 dark:border-gray-800 dark:bg-gray-950 dark:placeholder:text-gray-400 dark:focus-visible:ring-gray-300 md:w-[200px] lg:w-[300px]"
                            placeholder="搜尋..."
                            type="search"
                        />
                    </div>
                </form>
            </div>

            <div className="flex items-center gap-2">
                <Button variant="ghost" size="icon" onClick={toggleDarkMode}>
                    {darkMode ? <Sun className="h-5 w-5" /> : <Moon className="h-5 w-5" />}
                    <span className="sr-only">Toggle theme</span>
                </Button>

                <Button variant="ghost" size="icon" className="relative">
                    <Bell className="h-5 w-5" />
                    <span className="absolute top-1.5 right-1.5 h-2 w-2 rounded-full bg-red-500" />
                    <span className="sr-only">Notifications</span>
                </Button>

                <Button variant="ghost" size="icon" className="rounded-full">
                    <User className="h-5 w-5" />
                    <span className="sr-only">User profile</span>
                </Button>
            </div>
        </header>
    )
}
