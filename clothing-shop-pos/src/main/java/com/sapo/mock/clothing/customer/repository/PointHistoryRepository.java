package com.sapo.mock.clothing.customer.repository;

import com.sapo.mock.clothing.entity.PointHistory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PointHistoryRepository extends JpaRepository<PointHistory, Integer> {
    Page<PointHistory> findByCustomerIdOrderByCreatedAtDesc(Integer customerId, Pageable pageable);
    List<PointHistory> findByOrderId(Integer orderId);

    @Query("SELECT SUM(ph.pointsChange) FROM PointHistory ph WHERE ph.orderId = :orderId AND ph.type = 'REFUND' AND ph.pointsChange < 0")
    Integer sumPointsDeductedByOrderId(@Param("orderId") Integer orderId);
}
