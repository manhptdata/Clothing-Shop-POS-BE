package com.sapo.mock.clothing.receipt.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import com.sapo.mock.clothing.entity.StockReceipt;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import com.sapo.mock.clothing.util.constant.ReceiptStatus;

@Repository
public interface StockReceiptRepository extends JpaRepository<StockReceipt, Integer>, JpaSpecificationExecutor<StockReceipt> {

	Page<StockReceipt> findByStatus(ReceiptStatus status, Pageable pageable);

	boolean existsBySupplierId(Integer supplierId);

	@org.springframework.data.jpa.repository.Lock(jakarta.persistence.LockModeType.PESSIMISTIC_WRITE)
	@org.springframework.data.jpa.repository.Query("SELECT sr FROM StockReceipt sr WHERE sr.id = :id")
	java.util.Optional<StockReceipt> findByIdWithPessimisticLock(@org.springframework.data.repository.query.Param("id") Integer id);
}