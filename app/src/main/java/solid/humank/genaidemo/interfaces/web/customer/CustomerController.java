package solid.humank.genaidemo.interfaces.web.customer;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 客戶控制器
 * 提供客戶相關的 API 端點
 */
@RestController
@RequestMapping("/api/customers")
public class CustomerController {

    @Autowired
    private DataSource dataSource;

    /**
     * 獲取客戶列表
     */
    @GetMapping
    public ResponseEntity<Map<String, Object>> getCustomers(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        
        Map<String, Object> response = new HashMap<>();
        List<Map<String, Object>> customers = new ArrayList<>();
        
        try (Connection conn = dataSource.getConnection()) {
            // 獲取唯一客戶ID列表
            List<String> customerIds = new ArrayList<>();
            try (PreparedStatement ps = conn.prepareStatement("SELECT DISTINCT customer_id FROM orders ORDER BY customer_id")) {
                ResultSet rs = ps.executeQuery();
                while (rs.next()) {
                    customerIds.add(rs.getString("customer_id"));
                }
            }
            
            int totalElements = customerIds.size();
            int totalPages = (int) Math.ceil((double) totalElements / size);
            
            // 分頁處理
            int startIndex = page * size;
            int endIndex = Math.min(startIndex + size, totalElements);
            
            for (int i = startIndex; i < endIndex; i++) {
                String customerId = customerIds.get(i);
                Map<String, Object> customer = new HashMap<>();
                
                customer.put("id", customerId);
                customer.put("name", generateCustomerName(customerId));
                customer.put("email", generateEmail(customerId));
                customer.put("phone", generatePhone(customerId));
                customer.put("address", generateAddress(customerId));
                customer.put("membershipLevel", generateMembershipLevel(customerId));
                
                customers.add(customer);
            }
            
            // 構建分頁響應
            Map<String, Object> pageInfo = new HashMap<>();
            pageInfo.put("content", customers);
            pageInfo.put("totalElements", totalElements);
            pageInfo.put("totalPages", totalPages);
            pageInfo.put("size", size);
            pageInfo.put("number", page);
            pageInfo.put("first", page == 0);
            pageInfo.put("last", page >= totalPages - 1);
            
            response.put("success", true);
            response.put("data", pageInfo);
            
        } catch (Exception e) {
            response.put("success", false);
            response.put("message", "獲取客戶列表時發生錯誤: " + e.getMessage());
        }
        
        return ResponseEntity.ok(response);
    }

    /**
     * 獲取單個客戶
     */
    @GetMapping("/{customerId}")
    public ResponseEntity<Map<String, Object>> getCustomer(@PathVariable String customerId) {
        Map<String, Object> response = new HashMap<>();
        
        try (Connection conn = dataSource.getConnection()) {
            // 檢查客戶是否存在
            boolean customerExists = false;
            try (PreparedStatement ps = conn.prepareStatement("SELECT COUNT(*) FROM orders WHERE customer_id = ?")) {
                ps.setString(1, customerId);
                ResultSet rs = ps.executeQuery();
                if (rs.next() && rs.getInt(1) > 0) {
                    customerExists = true;
                }
            }
            
            if (customerExists) {
                Map<String, Object> customer = new HashMap<>();
                customer.put("id", customerId);
                customer.put("name", generateCustomerName(customerId));
                customer.put("email", generateEmail(customerId));
                customer.put("phone", generatePhone(customerId));
                customer.put("address", generateAddress(customerId));
                customer.put("membershipLevel", generateMembershipLevel(customerId));
                
                response.put("success", true);
                response.put("data", customer);
            } else {
                response.put("success", false);
                response.put("message", "客戶不存在");
                return ResponseEntity.notFound().build();
            }
            
        } catch (Exception e) {
            response.put("success", false);
            response.put("message", "獲取客戶時發生錯誤: " + e.getMessage());
        }
        
        return ResponseEntity.ok(response);
    }

    // 輔助方法：根據客戶ID生成客戶姓名
    private String generateCustomerName(String customerId) {
        String[] surnames = {"張", "李", "王", "陳", "林", "黃", "吳", "劉", "蔡", "楊"};
        String[] givenNames = {"小明", "小華", "大明", "美麗", "志偉", "淑芬", "建國", "雅婷", "俊傑", "怡君"};
        
        int surnameIndex = Math.abs(customerId.hashCode()) % surnames.length;
        int givenNameIndex = Math.abs((customerId + "name").hashCode()) % givenNames.length;
        
        return surnames[surnameIndex] + givenNames[givenNameIndex];
    }

    // 輔助方法：根據客戶ID生成Email
    private String generateEmail(String customerId) {
        String name = generateCustomerName(customerId);
        String[] domains = {"gmail.com", "yahoo.com", "hotmail.com", "email.com"};
        int domainIndex = Math.abs(customerId.hashCode()) % domains.length;
        
        // 簡化的拼音轉換
        String pinyin = convertToPinyin(name);
        return pinyin.toLowerCase() + "@" + domains[domainIndex];
    }

    // 輔助方法：根據客戶ID生成電話
    private String generatePhone(String customerId) {
        int phoneNumber = Math.abs(customerId.hashCode()) % 100000000;
        return String.format("09%08d", phoneNumber);
    }

    // 輔助方法：根據客戶ID生成地址
    private String generateAddress(String customerId) {
        String[] cities = {
            "台北市信義區信義路五段7號",
            "新北市板橋區中山路一段161號",
            "桃園市中壢區中正路123號",
            "台中市西屯區台灣大道三段99號",
            "高雄市前金區中正四路211號",
            "台南市東區東門路二段89號"
        };
        return cities[Math.abs(customerId.hashCode()) % cities.length];
    }

    // 輔助方法：根據客戶ID生成會員等級
    private String generateMembershipLevel(String customerId) {
        String[] levels = {"Bronze", "Silver", "Gold", "Platinum", "Diamond"};
        return levels[Math.abs(customerId.hashCode()) % levels.length];
    }

    // 簡化的中文轉拼音方法
    private String convertToPinyin(String chinese) {
        Map<String, String> pinyinMap = new HashMap<>();
        pinyinMap.put("張", "zhang");
        pinyinMap.put("李", "li");
        pinyinMap.put("王", "wang");
        pinyinMap.put("陳", "chen");
        pinyinMap.put("林", "lin");
        pinyinMap.put("黃", "huang");
        pinyinMap.put("吳", "wu");
        pinyinMap.put("劉", "liu");
        pinyinMap.put("蔡", "cai");
        pinyinMap.put("楊", "yang");
        pinyinMap.put("小明", "xiaoming");
        pinyinMap.put("小華", "xiaohua");
        pinyinMap.put("大明", "daming");
        pinyinMap.put("美麗", "meili");
        pinyinMap.put("志偉", "zhiwei");
        pinyinMap.put("淑芬", "shufen");
        pinyinMap.put("建國", "jianguo");
        pinyinMap.put("雅婷", "yating");
        pinyinMap.put("俊傑", "junjie");
        pinyinMap.put("怡君", "yijun");
        
        StringBuilder result = new StringBuilder();
        for (char c : chinese.toCharArray()) {
            String pinyin = pinyinMap.get(String.valueOf(c));
            if (pinyin != null) {
                result.append(pinyin);
            } else {
                result.append(c);
            }
        }
        return result.toString();
    }
}
