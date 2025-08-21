// 測試前端 API 連接
const axios = require('axios');

const API_BASE_URL = 'http://localhost:8080/api';

async function testApiConnection() {
  console.log('測試前端 API 連接...\n');

  try {
    // 測試統計 API
    console.log('1. 測試統計 API...');
    const statsResponse = await axios.get(`${API_BASE_URL}/stats`);
    console.log('✅ 統計 API 成功:', {
      totalOrders: statsResponse.data.totalOrders,
      uniqueCustomers: statsResponse.data.uniqueCustomers,
      totalInventories: statsResponse.data.totalInventories
    });

    // 測試活動 API
    console.log('\n2. 測試活動 API...');
    const activitiesResponse = await axios.get(`${API_BASE_URL}/activities?limit=3`);
    console.log('✅ 活動 API 成功:', {
      success: activitiesResponse.data.success,
      activityCount: activitiesResponse.data.data?.length || 0
    });

    // 測試產品 API
    console.log('\n3. 測試產品 API...');
    const productsResponse = await axios.get(`${API_BASE_URL}/products?page=0&size=5`);
    console.log('✅ 產品 API 成功:', {
      success: productsResponse.data.success,
      totalElements: productsResponse.data.data?.totalElements || 0
    });

    console.log('\n🎉 所有 API 測試成功！');

  } catch (error) {
    console.error('❌ API 測試失敗:', {
      message: error.message,
      status: error.response?.status,
      statusText: error.response?.statusText,
      data: error.response?.data
    });
  }
}

testApiConnection();
