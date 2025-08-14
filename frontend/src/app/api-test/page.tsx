'use client'

import React, { useEffect, useState } from 'react'
import { useStats, useActivities } from '@/hooks/useApi'

export default function ApiTestPage() {
  const [testResults, setTestResults] = useState<any>({})
  
  // 使用實際的 hooks
  const { data: statsData, isLoading: statsLoading, error: statsError } = useStats()
  const { data: activities, isLoading: activitiesLoading, error: activitiesError } = useActivities({ limit: 3 })

  // 直接測試 API
  useEffect(() => {
    const testDirectAPI = async () => {
      try {
        console.log('開始直接 API 測試...')
        
        // 直接使用 fetch 測試
        const response = await fetch('http://localhost:8080/api/stats')
        const data = await response.json()
        
        setTestResults(prev => ({
          ...prev,
          directFetch: {
            success: true,
            data: data,
            isSuccess: data.success === true || data.status === 'success'
          }
        }))
        
        console.log('直接 fetch 結果:', data)
      } catch (error: any) {
        setTestResults(prev => ({
          ...prev,
          directFetch: {
            success: false,
            error: error.message
          }
        }))
        console.error('直接 fetch 錯誤:', error)
      }
    }

    testDirectAPI()
  }, [])

  // 監控 hooks 狀態變化
  useEffect(() => {
    console.log('Stats Hook 狀態:', { statsData, statsLoading, statsError })
  }, [statsData, statsLoading, statsError])

  useEffect(() => {
    console.log('Activities Hook 狀態:', { activities, activitiesLoading, activitiesError })
  }, [activities, activitiesLoading, activitiesError])

  return (
    <div className="p-8 max-w-4xl mx-auto">
      <h1 className="text-3xl font-bold mb-8">API 測試頁面</h1>
      
      {/* 直接 API 測試結果 */}
      <div className="mb-8 p-6 border rounded-lg">
        <h2 className="text-xl font-semibold mb-4">直接 API 測試</h2>
        {testResults.directFetch ? (
          <div className={`p-4 rounded ${testResults.directFetch.success ? 'bg-green-50 border-green-200' : 'bg-red-50 border-red-200'}`}>
            <h3 className="font-medium mb-2">
              {testResults.directFetch.success ? '✅ 成功' : '❌ 失敗'}
            </h3>
            {testResults.directFetch.success ? (
              <div>
                <p>狀態檢查: {testResults.directFetch.isSuccess ? '✅ 成功' : '❌ 失敗'}</p>
                <p>總訂單: {testResults.directFetch.data?.totalOrders}</p>
                <p>獨特客戶: {testResults.directFetch.data?.uniqueCustomers}</p>
                <details className="mt-2">
                  <summary>完整響應</summary>
                  <pre className="text-xs bg-gray-100 p-2 mt-1 overflow-auto">
                    {JSON.stringify(testResults.directFetch.data, null, 2)}
                  </pre>
                </details>
              </div>
            ) : (
              <p className="text-red-600">錯誤: {testResults.directFetch.error}</p>
            )}
          </div>
        ) : (
          <p>測試中...</p>
        )}
      </div>

      {/* React Query Hooks 測試 */}
      <div className="grid grid-cols-1 md:grid-cols-2 gap-6">
        {/* 統計 Hook */}
        <div className="p-6 border rounded-lg">
          <h2 className="text-xl font-semibold mb-4">統計 Hook</h2>
          <div className="space-y-2">
            <p>載入狀態: {statsLoading ? '載入中...' : '已完成'}</p>
            <p>錯誤狀態: {statsError ? `❌ ${statsError.message}` : '✅ 無錯誤'}</p>
            <p>數據狀態: {statsData ? '✅ 有數據' : '❌ 無數據'}</p>
            
            {statsData && (
              <div className="mt-4">
                <h3 className="font-medium">數據內容:</h3>
                <p>總訂單: {statsData.totalOrders}</p>
                <p>獨特客戶: {statsData.uniqueCustomers}</p>
                <p>總庫存: {statsData.totalInventories}</p>
              </div>
            )}
            
            <details className="mt-2">
              <summary>完整數據</summary>
              <pre className="text-xs bg-gray-100 p-2 mt-1 overflow-auto">
                {JSON.stringify(statsData, null, 2)}
              </pre>
            </details>
          </div>
        </div>

        {/* 活動 Hook */}
        <div className="p-6 border rounded-lg">
          <h2 className="text-xl font-semibold mb-4">活動 Hook</h2>
          <div className="space-y-2">
            <p>載入狀態: {activitiesLoading ? '載入中...' : '已完成'}</p>
            <p>錯誤狀態: {activitiesError ? `❌ ${activitiesError.message}` : '✅ 無錯誤'}</p>
            <p>數據狀態: {activities ? '✅ 有數據' : '❌ 無數據'}</p>
            
            {activities && (
              <div className="mt-4">
                <h3 className="font-medium">活動數量: {activities.length}</h3>
                {activities.slice(0, 2).map((activity: any, index: number) => (
                  <div key={index} className="text-sm ml-4">
                    <p>• {activity.title} - {activity.timestamp}</p>
                  </div>
                ))}
              </div>
            )}
            
            <details className="mt-2">
              <summary>完整數據</summary>
              <pre className="text-xs bg-gray-100 p-2 mt-1 overflow-auto">
                {JSON.stringify(activities, null, 2)}
              </pre>
            </details>
          </div>
        </div>
      </div>

      {/* 環境信息 */}
      <div className="mt-8 p-6 bg-blue-50 border border-blue-200 rounded-lg">
        <h2 className="text-xl font-semibold mb-4">環境信息</h2>
        <p>API Base URL: {process.env.NEXT_PUBLIC_API_URL || 'http://localhost:8080/api'}</p>
        <p>當前時間: {new Date().toLocaleString()}</p>
      </div>
    </div>
  )
}
