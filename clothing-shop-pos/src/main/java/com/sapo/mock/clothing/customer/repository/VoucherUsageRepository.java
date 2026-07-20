package com.sapo.mock.clothing.customer.repository;

import com.sapo.mock.clothing.entity.VoucherUsage;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface VoucherUsageRepository extends JpaRepository<VoucherUsage, Integer> {

    Optional<VoucherUsage> findByCustomerIdAndVoucherCode(Integer customerId, String voucherCode);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT v FROM VoucherUsage v WHERE v.customerId = :customerId AND v.voucherCode = :voucherCode")
    Optional<VoucherUsage> findByCustomerIdAndVoucherCodeWithPessimisticLock(@Param("customerId") Integer customerId, @Param("voucherCode") String voucherCode);
}
