package com.sapo.mock.clothing.order.repository;

import com.sapo.mock.clothing.entity.Order;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

@Repository
public interface OrderRepository extends JpaRepository<Order, Integer>, JpaSpecificationExecutor<Order> {

    long countByCreatedAtAfter(Instant startOfDay);

    long countByCreatedAtBetween(Instant start, Instant end);

    Optional<Order> findByOrderNumber(String orderNumber);

    @Query("SELECT COALESCE(SUM(o.totalAmount), 0) FROM Order o WHERE o.status = OrderStatus.COMPLETED AND o.createdByUsername = :username AND o.createdAt >= :start")
    BigDecimal calculateUserRevenueToday(@org.springframework.data.repository.query.Param("username") String username,
            @org.springframework.data.repository.query.Param("start") Instant start);

    @Query("SELECT COALESCE(SUM(o.totalAmount), 0) FROM Order o WHERE o.status = OrderStatus.COMPLETED AND o.createdAt BETWEEN :start AND :end")
    BigDecimal calculateRevenueBetween(@org.springframework.data.repository.query.Param("start") Instant start,
            @org.springframework.data.repository.query.Param("end") Instant end);

    List<Order> findTop3ByCustomerIdOrderByCreatedAtDesc(Integer customerId);
}
