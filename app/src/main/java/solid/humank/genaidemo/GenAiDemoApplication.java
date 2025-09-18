package solid.humank.genaidemo;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

/** GenAI Demo 應用程序入口點 */
@SpringBootApplication
@EnableJpaRepositories(basePackages = "solid.humank.genaidemo.infrastructure")
@EntityScan(basePackages = {
        "solid.humank.genaidemo.infrastructure.inventory.persistence.entity",
        "solid.humank.genaidemo.infrastructure.shoppingcart.persistence.entity",
        "solid.humank.genaidemo.infrastructure.order.persistence.entity",
        "solid.humank.genaidemo.infrastructure.payment.persistence.entity",
        "solid.humank.genaidemo.infrastructure.product.persistence.entity",
        "solid.humank.genaidemo.infrastructure.customer.persistence.entity",
        "solid.humank.genaidemo.infrastructure.promotion.persistence.entity",
        "solid.humank.genaidemo.infrastructure.notification.persistence.entity",
        "solid.humank.genaidemo.infrastructure.review.persistence.entity",
        "solid.humank.genaidemo.infrastructure.seller.persistence.entity",
        "solid.humank.genaidemo.infrastructure.pricing.persistence.entity",
        "solid.humank.genaidemo.infrastructure.delivery.persistence.entity",
        "solid.humank.genaidemo.infrastructure.observability.persistence.entity",
        "solid.humank.genaidemo.infrastructure.entity"
})
public class GenAiDemoApplication {

    public static void main(String[] args) {
        SpringApplication.run(GenAiDemoApplication.class, args);
    }
}
