// 測試前端是否能正確處理訂單 API 數據
const axios = require('axios');

async function testOrdersAPI() {
  console.log('測試訂單 API 數據處理...\n');

  try {
    // 測試後端 API
    console.log('1. 測試後端 API:');
    const backendResponse = await axios.get('http://localhost:8080/api/orders?page=0&size=1');
    const backendData = backendResponse.data;
    
    console.log('後端響應成功:', backendData.success);
    console.log('訂單數量:', backendData.data?.content?.length || 0);
    
    if (backendData.data?.content?.length > 0) {
      const order = backendData.data.content[0];
      console.log('訂單詳情:');
      console.log('  ID:', order.id);
      console.log('  狀態:', order.status);
      console.log('  商品數量:', order.items?.length || 0);
      console.log('  總金額:', order.totalAmount);
      console.log('  有效金額:', order.effectiveAmount);
      
      if (order.items?.length > 0) {
        console.log('  第一個商品:', order.items[0].productName);
        console.log('  商品價格:', order.items[0].totalPrice);
      }
    }

    // 測試前端 API 客戶端
    console.log('\n2. 測試前端 API 客戶端:');
    const frontendResponse = await axios.get('http://localhost:3000/api/orders?page=0&size=1');
    console.log('前端 API 響應狀態:', frontendResponse.status);
    
  } catch (error) {
    console.error('❌ 測試失敗:', error.message);
    if (error.response) {
      console.error('響應狀態:', error.response.status);
      console.error('響應數據:', error.response.data);
    }
  }
}

testOrdersAPI();
