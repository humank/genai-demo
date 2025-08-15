'use client'

import React, { useEffect, useState } from 'react'
import axios from 'axios'

export default function DebugPage() {
  const [apiResults, setApiResults] = useState<any>({})
  const [loading, setLoading] = useState(true)

  useEffect(() => {
    const testAPIs = async () => {
      const results: any = {}
      
      try {
        // 直接使用 axios 測試 API
        console.log('開始測試 API...')
        
        // 測試統計 API
        try {
          const statsResponse = await axios.get('http://localhost:8080/api/stats')
          results.stats = { success: true, data: statsResponse.data }
          console.log('統計 API 成功:', statsResponse.data)
        } catch (error: any) {
          results.stats = { success: false, error: error.message }
          console.error('統計 API 失敗:', error)
        }

        // 測試活動 API
        try {
          const activitiesResponse = await axios.get('http://localhost:8080/api/activities?limit=3')
          results.activities = { success: true, data: activitiesResponse.data }
          console.log('活動 API 成功:', activitiesResponse.data)
        } catch (error: any) {
          results.activities = { success: false, error: error.message }
          console.error('活動 API 失敗:', error)
        }

        // 測試產品 API
        try {
          const productsResponse = await axios.get('http://localhost:8080/api/products?page=0&size=2')
          results.products = { success: true, data: productsResponse.data }
          console.log('產品 API 成功:', productsResponse.data)
        } catch (error: any) {
          results.products = { success: false, error: error.message }
          console.error('產品 API 失敗:', error)
        }

      } catch (error) {
        console.error('整體測試失敗:', error)
      }

      setApiResults(results)
      setLoading(false)
    }

    testAPIs()
  }, [])

  if (loading) {
    return (
      <div className="p-8">
        <h1 className="text-2xl font-bold mb-4">API 調試頁面</h1>
        <p>正在測試 API...</p>
      </div>
    )
  }

  return (
    <div className="p-8">
      <h1 className="text-2xl font-bold mb-4">API 調試頁面</h1>
      
      <div className="space-y-4">
        {Object.entries(apiResults).map(([key, result]: [string, any]) => (
          <div key={key} className={`p-4 border rounded ${result.success ? 'bg-green-50 border-green-200' : 'bg-red-50 border-red-200'}`}>
            <h3 className="font-semibold mb-2">{key.toUpperCase()} API</h3>
            <p className="mb-2">狀態: {result.success ? '✅ 成功' : '❌ 失敗'}</p>
            {result.success ? (
              <details>
                <summary>查看數據</summary>
                <pre className="text-xs bg-gray-100 p-2 mt-2 overflow-auto">
                  {JSON.stringify(result.data, null, 2)}
                </pre>
              </details>
            ) : (
              <p className="text-red-600">錯誤: {result.error}</p>
            )}
          </div>
        ))}
      </div>

      <div className="mt-8 p-4 bg-blue-50 border border-blue-200 rounded">
        <h3 className="font-semibold mb-2">環境信息</h3>
        <p>API Base URL: {process.env.NEXT_PUBLIC_API_URL || 'http://localhost:8080/api'}</p>
        <p>當前時間: {new Date().toLocaleString()}</p>
      </div>
    </div>
  )
}
