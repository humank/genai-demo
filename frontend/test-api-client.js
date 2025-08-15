// CMC 商務管理中心 - API 客戶端測試
const axios = require('axios');

// 模擬前端的 ApiClient 邏輯
async function testApiClient() {
  console.log('測試 CMC API 客戶端邏輯...\n');

  try {
    // 測試統計 API（使用 status: "success" 格式）
    console.log('1. 測試統計 API:');
    const statsResponse = await axios.get('http://localhost:8080/api/stats');
    const statsData = statsResponse.data;

    console.log('響應格式:', {
      hasSuccess: 'success' in statsData,
      hasStatus: 'status' in statsData,
      successValue: statsData.success,
      statusValue: statsData.status
    });

    // 應用修復後的邏輯
    const isStatsSuccess = statsData.success === true || statsData.status === 'success';
    console.log('是否成功:', isStatsSuccess);

    if (isStatsSuccess) {
      const result = statsData.data || statsData;
      console.log('✅ 統計 API 成功，數據:', {
        totalOrders: result.totalOrders,
        uniqueCustomers: result.uniqueCustomers,
        totalInventories: result.totalInventories
      });
    } else {
      console.log('❌ 統計 API 失敗');
    }

    console.log('\n2. 測試活動 API:');
    const activitiesResponse = await axios.get('http://localhost:8080/api/activities?limit=2');
    const activitiesData = activitiesResponse.data;

    console.log('響應格式:', {
      hasSuccess: 'success' in activitiesData,
      hasStatus: 'status' in activitiesData,
      successValue: activitiesData.success,
      statusValue: activitiesData.status
    });

    const isActivitiesSuccess = activitiesData.success === true || activitiesData.status === 'success';
    console.log('是否成功:', isActivitiesSuccess);

    if (isActivitiesSuccess) {
      const result = activitiesData.data || activitiesData;
      console.log('✅ 活動 API 成功，活動數量:', Array.isArray(result) ? result.length : 'N/A');
    } else {
      console.log('❌ 活動 API 失敗');
    }

  } catch (error) {
    console.error('❌ 測試失敗:', error.message);
  }
}

testApiClient();
