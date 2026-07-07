package com.sapo.mock.clothing.order.repository;

import com.sapo.mock.clothing.entity.Order;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.time.Instant;

@Repository
public interface OrderRepository extends JpaRepository<Order, Integer>, JpaSpecificationExecutor<Order> {

    long countByCreatedAtAfter(Instant startOfDay);

    java.util.List<Order> findTop3ByCustomerIdOrderByCreatedAtDesc(Integer customerId);

    long countByCreatedAtBetween(Instant start, Instant end);

    java.util.Optional<Order> findByOrderNumber(String orderNumber);

    @org.springframework.data.jpa.repository.Query("SELECT COALESCE(SUM(o.totalAmount), 0) FROM Order o WHERE o.status = com.sapo.mock.clothing.util.constant.OrderStatus.COMPLETED AND o.createdByUsername = :username AND o.createdAt >= :start")
    java.math.BigDecimal calculateUserRevenueToday(@org.springframework.data.repository.query.Param("username") String username, @org.springframework.data.repository.query.Param("start") Instant start);

    @org.springframework.data.jpa.repository.Query("SELECT COALESCE(SUM(o.totalAmount), 0) FROM Order o WHERE o.status = com.sapo.mock.clothing.util.constant.OrderStatus.COMPLETED AND o.createdAt BETWEEN :start AND :end")
    java.math.BigDecimal calculateRevenueBetween(@org.springframework.data.repository.query.Param("start") Instant start, @org.springframework.data.repository.query.Param("end") Instant end);
}
