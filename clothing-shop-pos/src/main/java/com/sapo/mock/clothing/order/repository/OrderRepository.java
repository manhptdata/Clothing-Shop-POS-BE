package com.sapo.mock.clothing.order.repository;

import com.sapo.mock.clothing.entity.Order;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.stereotype.Repository;
import jakarta.persistence.LockModeType;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.springframework.data.repository.query.Param;

@Repository
public interface OrderRepository extends JpaRepository<Order, Integer>, JpaSpecificationExecutor<Order> {

    long countByCreatedAtAfter(Instant startOfDay);

    long countByCreatedAtBetween(Instant start, Instant end);

    Optional<Order> findByOrderNumber(String orderNumber);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT o FROM Order o WHERE o.orderNumber = :orderNumber")
    Optional<Order> findByOrderNumberWithPessimisticLock(@Param("orderNumber") String orderNumber);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT o FROM Order o WHERE o.id = :id")
    Optional<Order> findByIdWithPessimisticLock(@Param("id") Integer id);

    @Query("SELECT COALESCE(SUM(o.totalAmount), 0) - " +
           "(SELECT COALESCE(SUM(ro.totalRefundAmount), 0) FROM ReturnOrder ro WHERE ro.createdByUsername = :username AND ro.createdAt >= :start) " +
           "FROM Order o " +
           "WHERE o.status IN (com.sapo.mock.clothing.util.constant.OrderStatus.COMPLETED, com.sapo.mock.clothing.util.constant.OrderStatus.PARTIALLY_RETURNED) " +
           "AND o.paymentMethod = com.sapo.mock.clothing.util.constant.PaymentMethod.CASH " +
           "AND o.createdByUsername = :username AND o.createdAt >= :start")
    BigDecimal calculateUserRevenueToday(@Param("username") String username,
            @Param("start") Instant start);

    @Query("SELECT COALESCE(SUM(o.totalAmount), 0) - " +
           "(SELECT COALESCE(SUM(ro.totalRefundAmount), 0) FROM ReturnOrder ro WHERE ro.createdAt BETWEEN :start AND :end) " +
           "FROM Order o " +
           "WHERE o.status IN (com.sapo.mock.clothing.util.constant.OrderStatus.COMPLETED, com.sapo.mock.clothing.util.constant.OrderStatus.PARTIALLY_RETURNED) " +
           "AND o.createdAt BETWEEN :start AND :end")
    BigDecimal calculateRevenueBetween(@Param("start") Instant start,
            @Param("end") Instant end);

    List<Order> findTop3ByCustomerIdOrderByCreatedAtDesc(Integer customerId);

    long countByCustomerIdAndVoucherCodeAndStatusNot(Integer customerId, String voucherCode, com.sapo.mock.clothing.util.constant.OrderStatus status);

    long countByCustomerIdAndStatus(Integer customerId, com.sapo.mock.clothing.util.constant.OrderStatus status);

    List<Order> findByStatusAndCreatedAtBefore(com.sapo.mock.clothing.util.constant.OrderStatus status, Instant createdAt);
}
