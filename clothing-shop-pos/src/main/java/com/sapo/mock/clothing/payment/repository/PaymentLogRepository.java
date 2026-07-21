package com.sapo.mock.clothing.payment.repository;

import com.sapo.mock.clothing.entity.PaymentLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import java.util.Optional;

@Repository
public interface PaymentLogRepository extends JpaRepository<PaymentLog, Integer>, JpaSpecificationExecutor<PaymentLog> {
    boolean existsByReferenceCode(String referenceCode);

    boolean existsByOrderNumberAndStatus(String orderNumber, String status);
    
    Optional<PaymentLog> findByReferenceCode(String referenceCode);

    @Query("SELECT COALESCE(SUM(p.refundAmount), 0) FROM PaymentLog p WHERE p.refundedBy = :username AND p.refundedAt >= :start AND p.status = 'REFUNDED'")
    java.math.BigDecimal calculateTotalRefundByUsernameSince(@Param("username") String username, @Param("start") java.time.Instant start);

    @Modifying
    @Transactional
    @Query("UPDATE PaymentLog p SET p.status = :status, p.orderNumber = :orderNumber WHERE p.referenceCode = :referenceCode")
    void updateByReferenceCode(@Param("referenceCode") String referenceCode,
                               @Param("status") String status,
                               @Param("orderNumber") String orderNumber);

    @Modifying
    @Transactional
    @Query("UPDATE PaymentLog p SET p.status = :newStatus WHERE p.orderNumber = :orderNumber AND p.status = :oldStatus")
    void updateStatusByOrderNumberAndOldStatus(@Param("orderNumber") String orderNumber,
                                               @Param("oldStatus") String oldStatus,
                                               @Param("newStatus") String newStatus);
}
