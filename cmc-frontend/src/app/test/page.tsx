'use client'

import React from 'react'
import { useStats, useActivities } from '@/hooks/useApi'

export default function TestPage() {
  const { data: statsData, isLoading: statsLoading, error: statsError } = useStats()
  const { data: activities, isLoading: activitiesLoading, error: activitiesError } = useActivities({ limit: 5 })

  return (
    <div className="p-8">
      <h1 className="text-2xl font-bold mb-6">API 測試頁面</h1>
      
      <div className="space-y-6">
        {/* 統計數據測試 */}
        <div className="border p-4 rounded">
          <h2 className="text-lg font-semibold mb-2">統計數據</h2>
          <p>載入狀態: {statsLoading ? '載入中...' : '已載入'}</p>
          {statsError && <p className="text-red-500">錯誤: {statsError.message}</p>}
          {statsData && (
            <div className="mt-2">
              <p>總訂單: {statsData.totalOrders}</p>
              <p>獨特客戶: {statsData.uniqueCustomers}</p>
              <p>總庫存: {statsData.totalInventories}</p>
              <p>總營收: {statsData.totalCompletedOrderValue}</p>
            </div>
          )}
          <details className="mt-2">
            <summary>原始數據</summary>
            <pre className="text-xs bg-gray-100 p-2 mt-1 overflow-auto">
              {JSON.stringify(statsData, null, 2)}
            </pre>
          </details>
        </div>

        {/* 活動數據測試 */}
        <div className="border p-4 rounded">
          <h2 className="text-lg font-semibold mb-2">活動記錄</h2>
          <p>載入狀態: {activitiesLoading ? '載入中...' : '已載入'}</p>
          {activitiesError && <p className="text-red-500">錯誤: {activitiesError.message}</p>}
          {activities && (
            <div className="mt-2">
              <p>活動數量: {activities.length}</p>
              {activities.map((activity, index) => (
                <div key={activity.id || index} className="ml-4 text-sm">
                  <p>• {activity.title} - {activity.timestamp}</p>
                </div>
              ))}
            </div>
          )}
          <details className="mt-2">
            <summary>原始數據</summary>
            <pre className="text-xs bg-gray-100 p-2 mt-1 overflow-auto">
              {JSON.stringify(activities, null, 2)}
            </pre>
          </details>
        </div>
      </div>
    </div>
  )
}
