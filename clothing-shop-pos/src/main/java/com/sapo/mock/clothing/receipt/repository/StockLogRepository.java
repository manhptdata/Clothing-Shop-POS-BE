package com.sapo.mock.clothing.receipt.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import com.sapo.mock.clothing.entity.StockLog;
import com.sapo.mock.clothing.util.constant.StockLogSource;

import java.time.Instant;
import java.util.List;
import org.springframework.data.repository.query.Param;
import com.sapo.mock.clothing.util.constant.StockLogReferenceType;

@Repository
public interface StockLogRepository extends JpaRepository<StockLog, Integer>,
        JpaSpecificationExecutor<StockLog> {

    // Lấy log theo variant (dùng cho trang chi tiết sản phẩm)
    Page<StockLog> findByVariantIdOrderByCreatedAtDesc(Integer variantId, Pageable pageable);

    // Lấy log theo nguồn thay đổi
    Page<StockLog> findBySourceOrderByCreatedAtDesc(StockLogSource source, Pageable pageable);

    // Lấy tất cả log trong khoảng thời gian
    @Query("SELECT s FROM StockLog s WHERE s.createdAt BETWEEN :from AND :to ORDER BY s.createdAt DESC")
    Page<StockLog> findByDateRange(
            @Param("from") Instant from,
            @Param("to") Instant to,
            Pageable pageable);

    // Lấy log theo referenceId + type (ví dụ: tất cả log của 1 đơn hàng)
    List<StockLog> findByReferenceIdAndReferenceTypeOrderByCreatedAtDesc(
            Integer referenceId,
            StockLogReferenceType referenceType);
}