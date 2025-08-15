'use client'

import React, { useState } from 'react'
import { useRouter } from 'next/navigation'
import { Navbar } from '@/components/layout/Navbar'
import { ProductCard } from '@/components/product/ProductCard'
import { StatsCard } from '@/components/dashboard/StatsCard'
import { Button } from '@/components/ui/button'
import { Input } from '@/components/ui/input'
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from '@/components/ui/select'
import { EmptyState } from '@/components/ui/empty-state'
import { Loading } from '@/components/ui/loading'
import { useProducts } from '@/hooks/useApi'
import { Product } from '@/types/domain'
import { 
  Package, 
  Plus, 
  Search, 
  Filter,
  RefreshCw,
  AlertTriangle,
  TrendingUp,
  DollarSign,
  Archive
} from 'lucide-react'

export default function ProductsPage() {
  const router = useRouter()
  const [searchTerm, setSearchTerm] = useState('')
  const [categoryFilter, setCategoryFilter] = useState<string>('ALL')
  const [stockFilter, setStockFilter] = useState<string>('ALL')
  const [currentPage, setCurrentPage] = useState(0)
  const pageSize = 20

  // 使用 API 獲取產品數據
  const { data: productsResponse, isLoading, error, refetch } = useProducts({
    page: currentPage,
    size: pageSize,
  })

  const products = productsResponse?.content || []

  // 統計數據
  const totalProducts = productsResponse?.totalElements || 0
  const inStockProducts = products.filter(p => p.inStock).length
  const lowStockProducts = products.filter(p => p.inStock && p.stockQuantity < 10).length
  const outOfStockProducts = products.filter(p => !p.inStock).length
  const totalValue = products.reduce((sum, p) => sum + (p.price.amount * p.stockQuantity), 0)

  // 過濾產品
  const filteredProducts = products.filter(product => {
    const matchesSearch = 
      product.name.toLowerCase().includes(searchTerm.toLowerCase()) ||
      product.description.toLowerCase().includes(searchTerm.toLowerCase()) ||
      product.category.toLowerCase().includes(searchTerm.toLowerCase())
    
    const matchesCategory = categoryFilter === 'ALL' || product.category === categoryFilter
    
    const matchesStock = 
      stockFilter === 'ALL' ||
      (stockFilter === 'IN_STOCK' && product.inStock) ||
      (stockFilter === 'LOW_STOCK' && product.inStock && product.stockQuantity < 10) ||
      (stockFilter === 'OUT_OF_STOCK' && !product.inStock)
    
    return matchesSearch && matchesCategory && matchesStock
  })

  // 獲取所有分類
  const categories = Array.from(new Set(products.map(p => p.category)))

  const handleViewProduct = (productId: string) => {
    // 導航到商品詳情頁面
    router.push(`/products/${productId}`)
  }

  const handleEditProduct = (productId: string) => {
    // 導航到商品編輯頁面（暫時跳轉到詳情頁面）
    router.push(`/products/${productId}`)
  }

  const handleDeleteProduct = (productId: string) => {
    if (window.confirm('確定要刪除這個產品嗎？此操作無法撤銷。')) {
      // TODO: 實現刪除功能
      alert('刪除功能尚未實現')
    }
  }

  const handleCreateProduct = () => {
    // TODO: 導航到商品創建頁面
    alert('新增商品功能尚未實現')
  }

  const handleRefresh = () => {
    refetch()
  }

  const handleResetFilters = () => {
    setSearchTerm('')
    setCategoryFilter('ALL')
    setStockFilter('ALL')
    setCurrentPage(0)
  }

  if (error) {
    return (
      <div className="min-h-screen bg-gradient-to-br from-background via-background to-muted/20">
        <Navbar />
        <main className="container-modern py-8">
          <div className="flex flex-col items-center justify-center p-8 text-center">
            <p className="text-red-500 mb-4">載入產品時發生錯誤</p>
            <Button onClick={handleRefresh} variant="outline">
              <RefreshCw className="h-4 w-4 mr-2" />
              重試
            </Button>
          </div>
        </main>
      </div>
    )
  }

  return (
    <div className="min-h-screen bg-gradient-to-br from-background via-background to-muted/20">
      <Navbar />
      
      <main className="container-modern py-8 space-y-8">
        {/* 頁面標題 */}
        <div className="flex flex-col sm:flex-row justify-between items-start sm:items-center gap-4">
          <div>
            <h1 className="text-3xl font-bold text-foreground">商品管理</h1>
            <p className="text-muted-foreground">
              管理商品資訊、庫存和定價策略
            </p>
          </div>
          
          <div className="flex space-x-2">
            <Button variant="outline" size="sm" onClick={handleRefresh}>
              <RefreshCw className="h-4 w-4 mr-2" />
              刷新
            </Button>
            <Button onClick={handleCreateProduct} className="btn-primary">
              <Plus className="h-4 w-4 mr-2" />
              新增商品
            </Button>
          </div>
        </div>

        {/* 統計卡片 */}
        <div className="grid-stats">
          <StatsCard
            title="商品總數"
            value={totalProducts}
            icon={Package}
            color="blue"
            loading={isLoading}
          />
          <StatsCard
            title="有庫存"
            value={inStockProducts}
            change={{ 
              value: totalProducts > 0 ? `${Math.round((inStockProducts/totalProducts)*100)}%` : '0%', 
              type: 'neutral', 
              period: '佔比' 
            }}
            icon={Archive}
            color="green"
            loading={isLoading}
          />
          <StatsCard
            title="庫存不足"
            value={lowStockProducts}
            change={{ 
              value: lowStockProducts > 0 ? '需要補貨' : '狀況良好', 
              type: lowStockProducts > 0 ? 'warning' : 'neutral', 
              period: '' 
            }}
            icon={AlertTriangle}
            color="orange"
            loading={isLoading}
          />
          <StatsCard
            title="庫存價值"
            value={`NT$ ${totalValue.toLocaleString()}`}
            icon={DollarSign}
            color="purple"
            loading={isLoading}
          />
        </div>

        {/* 搜尋和篩選 */}
        <div className="modern-card p-6 space-y-4">
          <div className="flex items-center space-x-2 mb-4">
            <Filter className="h-5 w-5 text-primary" />
            <h3 className="font-semibold text-foreground">篩選和搜尋</h3>
          </div>
          
          <div className="grid grid-cols-1 md:grid-cols-4 gap-4">
            <div className="relative">
              <Search className="absolute left-3 top-1/2 transform -translate-y-1/2 h-4 w-4 text-muted-foreground" />
              <Input
                placeholder="搜尋商品名稱、描述或分類..."
                value={searchTerm}
                onChange={(e) => setSearchTerm(e.target.value)}
                className="pl-10"
              />
            </div>
            
            <Select value={categoryFilter} onValueChange={setCategoryFilter}>
              <SelectTrigger>
                <SelectValue placeholder="選擇分類" />
              </SelectTrigger>
              <SelectContent>
                <SelectItem value="ALL">所有分類</SelectItem>
                {categories.map(category => (
                  <SelectItem key={category} value={category}>
                    {category}
                  </SelectItem>
                ))}
              </SelectContent>
            </Select>
            
            <Select value={stockFilter} onValueChange={setStockFilter}>
              <SelectTrigger>
                <SelectValue placeholder="庫存狀態" />
              </SelectTrigger>
              <SelectContent>
                <SelectItem value="ALL">所有狀態</SelectItem>
                <SelectItem value="IN_STOCK">有庫存</SelectItem>
                <SelectItem value="LOW_STOCK">庫存不足</SelectItem>
                <SelectItem value="OUT_OF_STOCK">缺貨</SelectItem>
              </SelectContent>
            </Select>
            
            <Button variant="outline" className="w-full" onClick={handleResetFilters}>
              <RefreshCw className="h-4 w-4 mr-2" />
              重置篩選
            </Button>
          </div>
        </div>

        {/* 產品列表 */}
        {isLoading ? (
          <div className="grid-modern">
            {Array.from({ length: 6 }).map((_, index) => (
              <div key={index} className="modern-card p-6 animate-pulse">
                <div className="space-y-4">
                  <div className="flex items-center space-x-3">
                    <div className="w-12 h-12 bg-muted rounded-lg"></div>
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
        ) : filteredProducts.length === 0 ? (
          <EmptyState
            icon={Package}
            title={searchTerm || categoryFilter !== 'ALL' || stockFilter !== 'ALL' 
              ? "沒有符合條件的商品" 
              : "還沒有任何商品"
            }
            description={searchTerm || categoryFilter !== 'ALL' || stockFilter !== 'ALL'
              ? "請嘗試調整搜尋條件或篩選器"
              : "開始添加您的第一個商品來建立商品目錄"
            }
            action={{
              label: "新增商品",
              onClick: handleCreateProduct
            }}
          />
        ) : (
          <div className="grid-modern">
            {filteredProducts.map((product, index) => (
              <div 
                key={product.id} 
                className="animate-scale-in" 
                style={{ animationDelay: `${index * 50}ms` }}
              >
                <ProductCard
                  product={product}
                  onView={handleViewProduct}
                  onEdit={handleEditProduct}
                  onDelete={handleDeleteProduct}
                />
              </div>
            ))}
          </div>
        )}

        {/* 分頁 */}
        {productsResponse && productsResponse.totalPages > 1 && (
          <div className="flex justify-center items-center space-x-2">
            <Button
              variant="outline"
              size="sm"
              onClick={() => setCurrentPage(Math.max(0, currentPage - 1))}
              disabled={currentPage === 0}
            >
              上一頁
            </Button>
            
            <span className="text-sm text-muted-foreground">
              第 {currentPage + 1} 頁，共 {productsResponse.totalPages} 頁
            </span>
            
            <Button
              variant="outline"
              size="sm"
              onClick={() => setCurrentPage(Math.min(productsResponse.totalPages - 1, currentPage + 1))}
              disabled={currentPage >= productsResponse.totalPages - 1}
            >
              下一頁
            </Button>
          </div>
        )}

        {/* 分頁信息 */}
        {filteredProducts.length > 0 && (
          <div className="flex justify-center items-center space-x-4 text-sm text-muted-foreground">
            <span>顯示 {filteredProducts.length} 個商品，共 {totalProducts} 個</span>
            {searchTerm && (
              <span>• 搜尋: "{searchTerm}"</span>
            )}
            {categoryFilter !== 'ALL' && (
              <span>• 分類: {categoryFilter}</span>
            )}
          </div>
        )}
      </main>
    </div>
  )
}
