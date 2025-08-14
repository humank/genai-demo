package solid.humank.genaidemo.application.product.port.incoming;

import solid.humank.genaidemo.application.product.ProductDto;
import solid.humank.genaidemo.application.product.dto.command.UpdateProductCommand;

/** 產品管理用例接口 */
public interface ProductManagementUseCase {

    /** 更新產品信息 */
    ProductDto updateProduct(UpdateProductCommand command);

    /** 刪除產品 */
    void deleteProduct(String productId);
}
