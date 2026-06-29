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
}
