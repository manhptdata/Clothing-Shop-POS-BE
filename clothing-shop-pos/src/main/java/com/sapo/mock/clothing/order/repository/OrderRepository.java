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

    @Query("SELECT COALESCE(SUM(COALESCE(o.paidAmount, 0) - COALESCE(o.changeAmount, 0)), 0) FROM Order o " +
           "WHERE o.status IN (com.sapo.mock.clothing.util.constant.OrderStatus.COMPLETED, com.sapo.mock.clothing.util.constant.OrderStatus.PARTIALLY_RETURNED, com.sapo.mock.clothing.util.constant.OrderStatus.RETURNED) " +
           "AND o.paymentMethod = com.sapo.mock.clothing.util.constant.PaymentMethod.CASH " +
           "AND COALESCE(o.cashierUsername, o.createdByUsername) = :username AND o.createdAt BETWEEN :start AND :end")
    BigDecimal calculateUserCashSalesBetween(@Param("username") String username, @Param("start") Instant start, @Param("end") Instant end);

    @Query("SELECT COALESCE(SUM(ro.totalRefundAmount), 0) FROM ReturnOrder ro " +
           "WHERE ro.createdByUsername = :username AND ro.createdAt BETWEEN :start AND :end AND (ro.refundMethod = 'CASH' OR ro.refundMethod IS NULL)")
    BigDecimal calculateUserCashRefundsBetween(@Param("username") String username, @Param("start") Instant start, @Param("end") Instant end);

    default BigDecimal calculateUserRevenueBetween(String username, Instant start, Instant end) {
        BigDecimal sales = calculateUserCashSalesBetween(username, start, end);
        BigDecimal cashRefunds = calculateUserCashRefundsBetween(username, start, end);
        return (sales != null ? sales : BigDecimal.ZERO).subtract(cashRefunds != null ? cashRefunds : BigDecimal.ZERO);
    }

    @Query("SELECT COALESCE(SUM(COALESCE(o.paidAmount, 0) - COALESCE(o.changeAmount, 0)), 0) FROM Order o " +
           "WHERE o.status IN (com.sapo.mock.clothing.util.constant.OrderStatus.COMPLETED, com.sapo.mock.clothing.util.constant.OrderStatus.PARTIALLY_RETURNED, com.sapo.mock.clothing.util.constant.OrderStatus.RETURNED) " +
           "AND o.paymentMethod = com.sapo.mock.clothing.util.constant.PaymentMethod.QR_SEPAY " +
           "AND COALESCE(o.cashierUsername, o.createdByUsername) = :username AND o.createdAt BETWEEN :start AND :end")
    BigDecimal calculateUserTransferSalesBetween(@Param("username") String username, @Param("start") Instant start, @Param("end") Instant end);

    @Query("SELECT COALESCE(SUM(ro.totalRefundAmount), 0) FROM ReturnOrder ro " +
           "WHERE ro.createdByUsername = :username AND ro.createdAt BETWEEN :start AND :end AND ro.refundMethod = 'TRANSFER'")
    BigDecimal calculateUserTransferRefundsBetween(@Param("username") String username, @Param("start") Instant start, @Param("end") Instant end);

    default BigDecimal calculateUserTransferRevenueBetween(String username, Instant start, Instant end) {
        BigDecimal sales = calculateUserTransferSalesBetween(username, start, end);
        BigDecimal transferRefunds = calculateUserTransferRefundsBetween(username, start, end);
        return (sales != null ? sales : BigDecimal.ZERO).subtract(transferRefunds != null ? transferRefunds : BigDecimal.ZERO);
    }

    default BigDecimal calculateUserRevenueToday(String username, Instant start) {
        return calculateUserRevenueBetween(username, start, Instant.now());
    }

    @Query("SELECT COALESCE(SUM(o.totalAmount), 0) FROM Order o " +
           "WHERE o.status IN (com.sapo.mock.clothing.util.constant.OrderStatus.COMPLETED, com.sapo.mock.clothing.util.constant.OrderStatus.PARTIALLY_RETURNED, com.sapo.mock.clothing.util.constant.OrderStatus.RETURNED) " +
           "AND o.createdAt BETWEEN :start AND :end")
    BigDecimal calculateTotalSalesBetween(@Param("start") Instant start, @Param("end") Instant end);

    @Query("SELECT COALESCE(SUM(oli.costPrice * oli.quantity), 0) FROM OrderLineItem oli " +
           "WHERE oli.order.status IN (com.sapo.mock.clothing.util.constant.OrderStatus.COMPLETED, com.sapo.mock.clothing.util.constant.OrderStatus.PARTIALLY_RETURNED, com.sapo.mock.clothing.util.constant.OrderStatus.RETURNED) " +
           "AND oli.order.createdAt BETWEEN :start AND :end")
    BigDecimal calculateTotalCogsBetween(@Param("start") Instant start, @Param("end") Instant end);

    @Query("SELECT COALESCE(SUM(ro.totalRefundAmount), 0) FROM ReturnOrder ro " +
           "WHERE ro.createdAt BETWEEN :start AND :end")
    BigDecimal calculateTotalRefundsBetween(@Param("start") Instant start, @Param("end") Instant end);

    default BigDecimal calculateRevenueBetween(Instant start, Instant end) {
        BigDecimal sales = calculateTotalSalesBetween(start, end);
        BigDecimal refunds = calculateTotalRefundsBetween(start, end);
        return (sales != null ? sales : BigDecimal.ZERO).subtract(refunds != null ? refunds : BigDecimal.ZERO);
    }

    List<Order> findTop3ByCustomerIdOrderByCreatedAtDesc(Integer customerId);

    long countByCustomerIdAndVoucherCodeAndStatusNot(Integer customerId, String voucherCode, com.sapo.mock.clothing.util.constant.OrderStatus status);

    long countByCustomerIdAndStatus(Integer customerId, com.sapo.mock.clothing.util.constant.OrderStatus status);

    List<Order> findByStatusAndCreatedAtBefore(com.sapo.mock.clothing.util.constant.OrderStatus status, Instant createdAt);
}
