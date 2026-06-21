package com.sapo.mock.clothing.customer.repository;

import com.sapo.mock.clothing.entity.PointHistory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PointHistoryRepository extends JpaRepository<PointHistory, Integer> {
    Page<PointHistory> findByCustomerIdOrderByCreatedAtDesc(Integer customerId, Pageable pageable);
    List<PointHistory> findByOrderId(Integer orderId);
}
