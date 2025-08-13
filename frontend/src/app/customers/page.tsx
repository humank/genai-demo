'use client'

import React, { useState } from 'react'
import { Navbar } from '@/components/layout/Navbar'
import { CustomerCard } from '@/components/customer/CustomerCard'
import { StatsCard } from '@/components/dashboard/StatsCard'
import { Button } from '@/components/ui/button'
import { Input } from '@/components/ui/input'
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from '@/components/ui/select'
import { EmptyState } from '@/components/ui/empty-state'
import { Customer } from '@/types/domain'
import { 
  Users, 
  Plus, 
  Search, 
  Filter,
  RefreshCw,
  UserPlus,
  Crown,
  TrendingUp,
  Calendar
} from 'lucide-react'

// 模擬客戶數據
const mockCustomers: Customer[] = [
  {
    id: 'cust-001',
    name: '張小明',
    email: 'zhang.xiaoming@email.com',
    phone: '0912-345-678',
    address: '台北市信義區信義路五段7號',
    membershipLevel: 'Gold'
  },
  {
    id: 'cust-002',
    name: '李小華',
    email: 'li.xiaohua@email.com',
    phone: '0923-456-789',
    address: '新北市板橋區中山路一段161號',
    membershipLevel: 'Silver'
  },
  {
    id: 'cust-003',
    name: '王大明',
    email: 'wang.daming@email.com',
    phone: '0934-567-890',
    address: '桃園市中壢區中正路123號',
    membershipLevel: 'Platinum'
  },
  {
    id: 'cust-004',
    name: '陳美麗',
    email: 'chen.meili@email.com',
    phone: '0945-678-901',
    address: '台中市西屯區台灣大道三段99號',
    membershipLevel: 'Bronze'
  },
  {
    id: 'cust-005',
    name: '林志偉',
    email: 'lin.zhiwei@email.com',
    phone: '0956-789-012',
    address: '高雄市前金區中正四路211號',
    membershipLevel: 'Diamond'
  },
  {
    id: 'cust-006',
    name: '黃淑芬',
    email: 'huang.shufen@email.com',
    phone: '0967-890-123',
    address: '台南市東區東門路二段89號',
    membershipLevel: 'Gold'
  }
]

// 模擬客戶統計數據
const mockCustomerStats = {
  'cust-001': { orderCount: 15, totalSpent: 45600, lastOrderDate: '2024-08-10' },
  'cust-002': { orderCount: 8, totalSpent: 23400, lastOrderDate: '2024-08-08' },
  'cust-003': { orderCount: 22, totalSpent: 89200, lastOrderDate: '2024-08-12' },
  'cust-004': { orderCount: 3, totalSpent: 8900, lastOrderDate: '2024-07-25' },
  'cust-005': { orderCount: 35, totalSpent: 156700, lastOrderDate: '2024-08-13' },
  'cust-006': { orderCount: 12, totalSpent: 34500, lastOrderDate: '2024-08-09' }
}

export default function CustomersPage() {
  const [customers] = useState<Customer[]>(mockCustomers)
  const [searchTerm, setSearchTerm] = useState('')
  const [membershipFilter, setMembershipFilter] = useState<string>('ALL')
  const [loading] = useState(false)

  // 統計數據
  const totalCustomers = customers.length
  const activeCustomers = customers.filter(c => {
    const stats = mockCustomerStats[c.id as keyof typeof mockCustomerStats]
    return stats && new Date(stats.lastOrderDate) > new Date(Date.now() - 30 * 24 * 60 * 60 * 1000)
  }).length
  const vipCustomers = customers.filter(c => ['Gold', 'Platinum', 'Diamond'].includes(c.membershipLevel)).length
  const totalRevenue = Object.values(mockCustomerStats).reduce((sum, stats) => sum + stats.totalSpent, 0)

  // 過濾客戶
  const filteredCustomers = customers.filter(customer => {
    const matchesSearch = 
      customer.name.toLowerCase().includes(searchTerm.toLowerCase()) ||
      customer.email.toLowerCase().includes(searchTerm.toLowerCase()) ||
      customer.phone.includes(searchTerm)
    
    const matchesMembership = membershipFilter === 'ALL' || customer.membershipLevel === membershipFilter
    
    return matchesSearch && matchesMembership
  })

  // 獲取所有會員等級
  const membershipLevels = Array.from(new Set(customers.map(c => c.membershipLevel)))

  const handleViewCustomer = (customerId: string) => {
    console.log('View customer:', customerId)
  }

  const handleEditCustomer = (customerId: string) => {
    console.log('Edit customer:', customerId)
  }

  const handleMessageCustomer = (customerId: string) => {
    console.log('Message customer:', customerId)
  }

  const handleCreateCustomer = () => {
    console.log('Create new customer')
  }

  return (
    <div className="min-h-screen bg-gradient-to-br from-background via-background to-muted/20">
      <Navbar />
      
      <main className="container-modern py-8 space-y-8">
        {/* 頁面標題 */}
        <div className="flex flex-col sm:flex-row justify-between items-start sm:items-center gap-4">
          <div>
            <h1 className="text-3xl font-bold text-foreground">客戶管理</h1>
            <p className="text-muted-foreground">
              管理客戶資料、會員等級和購買記錄
            </p>
          </div>
          
          <div className="flex space-x-2">
            <Button variant="outline" size="sm">
              <RefreshCw className="h-4 w-4 mr-2" />
              刷新
            </Button>
            <Button onClick={handleCreateCustomer} className="btn-primary">
              <UserPlus className="h-4 w-4 mr-2" />
              新增客戶
            </Button>
          </div>
        </div>

        {/* 統計卡片 */}
        <div className="grid-stats">
          <StatsCard
            title="客戶總數"
            value={totalCustomers}
            change={{ value: '+5%', type: 'increase', period: '本月' }}
            icon={Users}
            color="blue"
          />
          <StatsCard
            title="活躍客戶"
            value={activeCustomers}
            change={{ value: `${Math.round((activeCustomers/totalCustomers)*100)}%`, type: 'neutral', period: '活躍率' }}
            icon={TrendingUp}
            color="green"
          />
          <StatsCard
            title="VIP 客戶"
            value={vipCustomers}
            change={{ value: `${Math.round((vipCustomers/totalCustomers)*100)}%`, type: 'neutral', period: 'VIP 率' }}
            icon={Crown}
            color="purple"
          />
          <StatsCard
            title="總營收貢獻"
            value={`NT$ ${totalRevenue.toLocaleString()}`}
            change={{ value: '+12%', type: 'increase', period: '本月' }}
            icon={Calendar}
            color="orange"
          />
        </div>

        {/* 搜尋和篩選 */}
        <div className="modern-card p-6 space-y-4">
          <div className="flex items-center space-x-2 mb-4">
            <Filter className="h-5 w-5 text-primary" />
            <h3 className="font-semibold text-foreground">篩選和搜尋</h3>
          </div>
          
          <div className="grid grid-cols-1 md:grid-cols-3 gap-4">
            <div className="relative md:col-span-2">
              <Search className="absolute left-3 top-1/2 transform -translate-y-1/2 h-4 w-4 text-muted-foreground" />
              <Input
                placeholder="搜尋客戶姓名、Email 或電話..."
                value={searchTerm}
                onChange={(e) => setSearchTerm(e.target.value)}
                className="pl-10"
              />
            </div>
            
            <Select value={membershipFilter} onValueChange={setMembershipFilter}>
              <SelectTrigger>
                <SelectValue placeholder="會員等級" />
              </SelectTrigger>
              <SelectContent>
                <SelectItem value="ALL">所有等級</SelectItem>
                {membershipLevels.map(level => (
                  <SelectItem key={level} value={level}>
                    {level}
                  </SelectItem>
                ))}
              </SelectContent>
            </Select>
          </div>
        </div>

        {/* 客戶列表 */}
        {loading ? (
          <div className="grid-modern">
            {Array.from({ length: 6 }).map((_, index) => (
              <div key={index} className="modern-card p-6 animate-pulse">
                <div className="space-y-4">
                  <div className="flex items-center space-x-3">
                    <div className="w-12 h-12 bg-muted rounded-full"></div>
                    <div className="flex-1 space-y-2">
                      <div className="h-4 bg-muted rounded w-3/4"></div>
                      <div className="h-3 bg-muted rounded w-1/2"></div>
                    </div>
                  </div>
                  <div className="space-y-2">
                    <div className="h-3 bg-muted rounded"></div>
                    <div className="h-3 bg-muted rounded w-2/3"></div>
                  </div>
                </div>
              </div>
            ))}
          </div>
        ) : filteredCustomers.length === 0 ? (
          <EmptyState
            icon={Users}
            title={searchTerm || membershipFilter !== 'ALL' 
              ? "沒有符合條件的客戶" 
              : "還沒有任何客戶"
            }
            description={searchTerm || membershipFilter !== 'ALL'
              ? "請嘗試調整搜尋條件或篩選器"
              : "開始添加您的第一個客戶來建立客戶資料庫"
            }
            action={{
              label: "新增客戶",
              onClick: handleCreateCustomer
            }}
          />
        ) : (
          <div className="grid-modern">
            {filteredCustomers.map((customer, index) => {
              const stats = mockCustomerStats[customer.id as keyof typeof mockCustomerStats] || 
                { orderCount: 0, totalSpent: 0, lastOrderDate: undefined }
              
              return (
                <div 
                  key={customer.id} 
                  className="animate-scale-in" 
                  style={{ animationDelay: `${index * 50}ms` }}
                >
                  <CustomerCard
                    customer={customer}
                    onView={handleViewCustomer}
                    onEdit={handleEditCustomer}
                    onMessage={handleMessageCustomer}
                    orderCount={stats.orderCount}
                    totalSpent={stats.totalSpent}
                    lastOrderDate={stats.lastOrderDate}
                  />
                </div>
              )
            })}
          </div>
        )}

        {/* 分頁信息 */}
        {filteredCustomers.length > 0 && (
          <div className="flex justify-center items-center space-x-4 text-sm text-muted-foreground">
            <span>顯示 {filteredCustomers.length} 位客戶</span>
            {searchTerm && (
              <span>• 搜尋: "{searchTerm}"</span>
            )}
            {membershipFilter !== 'ALL' && (
              <span>• 等級: {membershipFilter}</span>
            )}
          </div>
        )}
      </main>
    </div>
  )
}
