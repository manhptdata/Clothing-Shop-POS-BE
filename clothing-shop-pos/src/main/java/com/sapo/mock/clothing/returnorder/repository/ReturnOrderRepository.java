package com.sapo.mock.clothing.returnorder.repository;

import com.sapo.mock.clothing.entity.ReturnOrder;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;

@Repository
public interface ReturnOrderRepository extends JpaRepository<ReturnOrder, Integer>, JpaSpecificationExecutor<ReturnOrder> {

    List<ReturnOrder> findByOrderId(Integer orderId);

    long countByCreatedAtAfter(Instant startOfDay);

    @Query("SELECT SUM(ro.totalRefundAmount) FROM ReturnOrder ro WHERE ro.order.id = :orderId")
    java.math.BigDecimal getTotalRefundedByOrderId(@Param("orderId") Integer orderId);

    @Query("SELECT COALESCE(SUM(ro.totalRefundAmount), 0) FROM ReturnOrder ro WHERE ro.createdByUsername = :username AND ro.refundMethod = 'CASH' AND ro.createdAt >= :start")
    java.math.BigDecimal calculateUserCashRefundToday(@Param("username") String username, @Param("start") Instant start);

    @Query("SELECT COALESCE(SUM(roli.quantity * oli.costPrice), 0) FROM ReturnOrderLineItem roli " +
           "JOIN roli.returnOrder ro JOIN ro.order o JOIN OrderLineItem oli ON oli.order.id = o.id AND oli.variantId = roli.variantId " +
           "WHERE ro.createdAt BETWEEN :start AND :end AND roli.isRestocked = true")
    java.math.BigDecimal calculateRestockedCogsBetween(@Param("start") Instant start, @Param("end") Instant end);
}
