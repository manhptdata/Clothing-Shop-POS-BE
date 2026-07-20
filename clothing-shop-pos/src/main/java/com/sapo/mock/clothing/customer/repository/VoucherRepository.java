package com.sapo.mock.clothing.customer.repository;


import com.sapo.mock.clothing.entity.Voucher;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import jakarta.persistence.LockModeType;

public interface VoucherRepository extends JpaRepository<Voucher, Integer> {
    Optional<Voucher> findByCode(String code);
    boolean existsByCode(String code);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT v FROM Voucher v WHERE v.code = :code")
    Optional<Voucher> findByCodeWithPessimisticLock(@Param("code") String code);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT v FROM Voucher v WHERE v.id = :id")
    Optional<Voucher> findByIdWithPessimisticLock(@Param("id") Integer id);

    @org.springframework.data.jpa.repository.Modifying
    @Query("UPDATE Voucher v SET v.usedQuantity = COALESCE(v.usedQuantity, 0) + 1 WHERE v.code = :code AND (v.totalQuantity IS NULL OR COALESCE(v.usedQuantity, 0) < v.totalQuantity) AND v.status = com.sapo.mock.clothing.util.constant.VoucherCampaignStatusEnum.ACTIVE")
    int incrementUsedQuantity(@Param("code") String code);

    @org.springframework.data.jpa.repository.Modifying
    @Query("UPDATE Voucher v SET v.usedQuantity = CASE WHEN v.usedQuantity > 0 THEN v.usedQuantity - 1 ELSE 0 END WHERE v.code = :code")
    int decrementUsedQuantity(@Param("code") String code);
}