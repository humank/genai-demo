import { type ClassValue, clsx } from "clsx"
import { twMerge } from "tailwind-merge"
import { format, parseISO } from "date-fns"
import { zhTW } from "date-fns/locale"
import { Money, OrderStatus, PaymentStatus } from "@/types/domain"

export function cn(...inputs: ClassValue[]) {
  return twMerge(clsx(inputs))
}

// 格式化金額
export function formatMoney(money?: Money | null): string {
  if (!money || money.amount === undefined || money.amount === null) {
    return 'NT$ 0'
  }
  
  return new Intl.NumberFormat('zh-TW', {
    style: 'currency',
    currency: money.currency || 'TWD',
  }).format(money.amount)
}

// 格式化日期
export function formatDate(dateString?: string | null, formatStr: string = 'yyyy/MM/dd HH:mm'): string {
  if (!dateString) {
    return 'N/A'
  }
  
  try {
    const date = parseISO(dateString)
    return format(date, formatStr, { locale: zhTW })
  } catch (error) {
    return dateString
  }
}

// 格式化相對時間
export function formatRelativeTime(dateString: string): string {
  try {
    const date = parseISO(dateString)
    const now = new Date()
    const diffInMinutes = Math.floor((now.getTime() - date.getTime()) / (1000 * 60))
    
    if (diffInMinutes < 1) return '剛剛'
    if (diffInMinutes < 60) return `${diffInMinutes} 分鐘前`
    
    const diffInHours = Math.floor(diffInMinutes / 60)
    if (diffInHours < 24) return `${diffInHours} 小時前`
    
    const diffInDays = Math.floor(diffInHours / 24)
    if (diffInDays < 7) return `${diffInDays} 天前`
    
    return formatDate(dateString, 'yyyy/MM/dd')
  } catch (error) {
    return dateString
  }
}

// 訂單狀態相關
export function getOrderStatusText(status: OrderStatus): string {
  const statusMap: Record<OrderStatus, string> = {
    [OrderStatus.CREATED]: '已創建',
    [OrderStatus.PENDING]: '待處理',
    [OrderStatus.CONFIRMED]: '已確認',
    [OrderStatus.SHIPPED]: '已出貨',
    [OrderStatus.DELIVERED]: '已送達',
    [OrderStatus.CANCELLED]: '已取消',
  }
  return statusMap[status] || status
}

export function getOrderStatusColor(status: OrderStatus): string {
  const colorMap: Record<OrderStatus, string> = {
    [OrderStatus.CREATED]: 'bg-gray-100 text-gray-800',
    [OrderStatus.PENDING]: 'bg-yellow-100 text-yellow-800',
    [OrderStatus.CONFIRMED]: 'bg-blue-100 text-blue-800',
    [OrderStatus.SHIPPED]: 'bg-purple-100 text-purple-800',
    [OrderStatus.DELIVERED]: 'bg-green-100 text-green-800',
    [OrderStatus.CANCELLED]: 'bg-red-100 text-red-800',
  }
  return colorMap[status] || 'bg-gray-100 text-gray-800'
}

// 支付狀態相關
export function getPaymentStatusText(status: PaymentStatus): string {
  const statusMap: Record<PaymentStatus, string> = {
    [PaymentStatus.PENDING]: '待支付',
    [PaymentStatus.PROCESSING]: '處理中',
    [PaymentStatus.COMPLETED]: '已完成',
    [PaymentStatus.FAILED]: '失敗',
    [PaymentStatus.CANCELLED]: '已取消',
  }
  return statusMap[status] || status
}

export function getPaymentStatusColor(status: PaymentStatus): string {
  const colorMap: Record<PaymentStatus, string> = {
    [PaymentStatus.PENDING]: 'bg-yellow-100 text-yellow-800',
    [PaymentStatus.PROCESSING]: 'bg-blue-100 text-blue-800',
    [PaymentStatus.COMPLETED]: 'bg-green-100 text-green-800',
    [PaymentStatus.FAILED]: 'bg-red-100 text-red-800',
    [PaymentStatus.CANCELLED]: 'bg-gray-100 text-gray-800',
  }
  return colorMap[status] || 'bg-gray-100 text-gray-800'
}

// 數字格式化
export function formatNumber(num: number): string {
  return new Intl.NumberFormat('zh-TW').format(num)
}

// 百分比格式化
export function formatPercentage(num: number): string {
  return new Intl.NumberFormat('zh-TW', {
    style: 'percent',
    minimumFractionDigits: 0,
    maximumFractionDigits: 2,
  }).format(num / 100)
}

// 截斷文字
export function truncateText(text: string, maxLength: number): string {
  if (text.length <= maxLength) return text
  return text.slice(0, maxLength) + '...'
}

// 生成隨機 ID
export function generateId(): string {
  return Math.random().toString(36).substr(2, 9)
}

// 驗證 email
export function isValidEmail(email: string): boolean {
  const emailRegex = /^[^\s@]+@[^\s@]+\.[^\s@]+$/
  return emailRegex.test(email)
}

// 驗證手機號碼 (台灣)
export function isValidPhone(phone: string): boolean {
  const phoneRegex = /^09\d{8}$/
  return phoneRegex.test(phone)
}

// 深拷貝
export function deepClone<T>(obj: T): T {
  return JSON.parse(JSON.stringify(obj))
}

// 防抖函數
export function debounce<T extends (...args: any[]) => any>(
  func: T,
  wait: number
): (...args: Parameters<T>) => void {
  let timeout: NodeJS.Timeout
  return (...args: Parameters<T>) => {
    clearTimeout(timeout)
    timeout = setTimeout(() => func(...args), wait)
  }
}

// 節流函數
export function throttle<T extends (...args: any[]) => any>(
  func: T,
  limit: number
): (...args: Parameters<T>) => void {
  let inThrottle: boolean
  return (...args: Parameters<T>) => {
    if (!inThrottle) {
      func(...args)
      inThrottle = true
      setTimeout(() => (inThrottle = false), limit)
    }
  }
}

// 計算折扣百分比
export function calculateDiscountPercentage(originalPrice: number, discountedPrice: number): number {
  if (originalPrice <= 0) return 0
  return Math.round(((originalPrice - discountedPrice) / originalPrice) * 100)
}

// 檢查是否為移動設備
export function isMobile(): boolean {
  if (typeof window === 'undefined') return false
  return window.innerWidth < 768
}

// 滾動到頂部
export function scrollToTop(): void {
  if (typeof window !== 'undefined') {
    window.scrollTo({ top: 0, behavior: 'smooth' })
  }
}

// 複製到剪貼板
export async function copyToClipboard(text: string): Promise<boolean> {
  try {
    await navigator.clipboard.writeText(text)
    return true
  } catch (error) {
    console.error('Failed to copy to clipboard:', error)
    return false
  }
}
