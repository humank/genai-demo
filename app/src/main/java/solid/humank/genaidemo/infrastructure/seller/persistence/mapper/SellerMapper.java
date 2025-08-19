package solid.humank.genaidemo.infrastructure.seller.persistence.mapper;

import org.springframework.stereotype.Component;

import solid.humank.genaidemo.domain.seller.model.aggregate.Seller;
import solid.humank.genaidemo.domain.seller.model.valueobject.SellerId;
import solid.humank.genaidemo.infrastructure.seller.persistence.entity.JpaSellerEntity;

/** 賣家映射器 */
@Component
public class SellerMapper {

    public JpaSellerEntity toJpaEntity(Seller seller) {
        JpaSellerEntity entity = new JpaSellerEntity();
        entity.setSellerId(seller.getSellerId().getId());
        entity.setName(seller.getName());
        entity.setEmail(seller.getEmail());
        entity.setPhone(seller.getPhone());
        entity.setActive(seller.isActive());
        return entity;
    }

    public Seller toDomainModel(JpaSellerEntity entity) {
        SellerId sellerId = new SellerId(entity.getSellerId());
        Seller seller = new Seller(sellerId, entity.getName(), entity.getEmail(), entity.getPhone());
        if (!entity.isActive()) {
            seller.deactivate();
        }
        return seller;
    }
}