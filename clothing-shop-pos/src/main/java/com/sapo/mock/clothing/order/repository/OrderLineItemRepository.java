package com.sapo.mock.clothing.order.repository;

import com.sapo.mock.clothing.entity.OrderLineItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import org.springframework.data.domain.Pageable;

import java.time.Instant;
import java.util.List;

@Repository
public interface OrderLineItemRepository extends JpaRepository<OrderLineItem, Integer>, JpaSpecificationExecutor<OrderLineItem> {

    // Lấy tất cả line items của 1 đơn hàng
    List<OrderLineItem> findByOrderId(Integer orderId);

    // Tối ưu N+1: lấy hàng loạt line items cho nhiều đơn hàng cùng lúc
    List<OrderLineItem> findByOrderIdIn(List<Integer> orderIds);

    /**
     * [AI Recommendation] Lấy tất cả cặp (order_id, product_id) trong N tháng gần nhất.
     * JOIN qua product_variant vì OrderLineItem chỉ có variantId, không có productId.
     * GROUP BY để đảm bảo 1 đơn không đếm trùng cùng 1 product (VD: mua 2 size khác nhau).
     */
    @Query(value = """
            SELECT oli.order_id, pv.product_id
            FROM order_line_item oli
            JOIN product_variant pv ON oli.variant_id = pv.id
            JOIN orders o ON oli.order_id = o.id
            WHERE o.created_at >= :cutoffDate
              AND o.status = 'COMPLETED'
            GROUP BY oli.order_id, pv.product_id
            """, nativeQuery = true)
    List<Object[]> findOrderProductPairsSince(@Param("cutoffDate") Instant cutoffDate);

    /**
     * Tính tổng số lượng bán được của từng Variant trong khoảng thời gian
     */
    @Query("""
            SELECT oli.productSku, SUM(oli.quantity)
            FROM OrderLineItem oli
            JOIN oli.order o
            WHERE o.createdAt >= :cutoffDate
              AND o.status = com.sapo.mock.clothing.util.constant.OrderStatus.COMPLETED
            GROUP BY oli.productSku
            """)
    List<Object[]> getVariantSoldQuantitySinceBySku(@Param("cutoffDate") Instant cutoffDate);

    /**
     * Lấy Top sản phẩm bán chạy nhất trong khoảng thời gian
     */
    @Query("""
            SELECT oli.productName, oli.productSku, SUM(oli.quantity), SUM(oli.subtotal)
            FROM OrderLineItem oli
            JOIN oli.order o
            WHERE o.createdAt BETWEEN :start AND :end
              AND o.status IN (com.sapo.mock.clothing.util.constant.OrderStatus.COMPLETED, com.sapo.mock.clothing.util.constant.OrderStatus.PARTIALLY_RETURNED)
            GROUP BY oli.productName, oli.productSku
            ORDER BY SUM(oli.quantity) DESC
            """)
    List<Object[]> findTopProductsBetween(@Param("start") Instant start, @Param("end") Instant end, Pageable pageable);
}
