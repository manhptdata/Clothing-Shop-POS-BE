package com.sapo.mock.clothing.payment.repository;

import com.sapo.mock.clothing.entity.PaymentLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface PaymentLogRepository extends JpaRepository<PaymentLog, Integer> {
    boolean existsByReferenceCode(String referenceCode);
}
