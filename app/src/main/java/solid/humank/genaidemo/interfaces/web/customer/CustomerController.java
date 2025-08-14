package solid.humank.genaidemo.interfaces.web.customer;

import solid.humank.genaidemo.application.customer.service.CustomerApplicationService;
import solid.humank.genaidemo.application.customer.CustomerDto;
import solid.humank.genaidemo.application.customer.CustomerPageDto;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * 客戶控制器
 * 提供客戶相關的 API 端點
 */
@RestController
@RequestMapping("/api/customers")
public class CustomerController {

    private final CustomerApplicationService customerApplicationService;
    
    public CustomerController(CustomerApplicationService customerApplicationService) {
        this.customerApplicationService = customerApplicationService;
    }

    /**
     * 獲取客戶列表
     */
    @GetMapping
    public ResponseEntity<Map<String, Object>> getCustomers(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        
        Map<String, Object> response = new HashMap<>();
        
        try {
            CustomerPageDto customerPage = customerApplicationService.getCustomers(page, size);
            
            response.put("success", true);
            response.put("data", customerPage);
            
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
        
        try {
            Optional<CustomerDto> customer = customerApplicationService.getCustomer(customerId);
            
            if (customer.isPresent()) {
                response.put("success", true);
                response.put("data", customer.get());
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

}
