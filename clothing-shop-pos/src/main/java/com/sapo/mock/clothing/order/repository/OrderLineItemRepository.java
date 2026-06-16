package com.sapo.mock.clothing.order.repository;

import com.sapo.mock.clothing.entity.OrderLineItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface OrderLineItemRepository extends JpaRepository<OrderLineItem, Integer>, JpaSpecificationExecutor<OrderLineItem> {

    // Lấy tất cả line items của 1 đơn hàng
    List<OrderLineItem> findByOrderId(Integer orderId);

    // Tối ưu N+1: lấy hàng loạt line items cho nhiều đơn hàng cùng lúc
    List<OrderLineItem> findByOrderIdIn(List<Integer> orderIds);
}
