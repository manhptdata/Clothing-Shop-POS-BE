package com.sapo.mock.clothing.customer.repository;

import com.sapo.mock.clothing.entity.CareLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.Optional;

@Repository
public interface CareLogRepository extends JpaRepository<CareLog, Integer> {

    /**
     * Lấy toàn bộ danh sách lịch sử chăm sóc (Phân trang)
     * Thường sẽ sắp xếp theo ngày gọi mới nhất lên đầu
     */
    @Query("SELECT cl FROM CareLog cl " +
            "JOIN FETCH cl.customer " +
            "JOIN FETCH cl.calledBy " +
            "LEFT JOIN FETCH cl.campaign " +
            "LEFT JOIN FETCH cl.order")
    Page<CareLog> findAllCareLogs(Pageable pageable);

    /**
     * Tìm lịch sử chăm sóc riêng của một khách hàng (Dùng khi click vào chi tiết 1 khách)
     */
    @Query("SELECT cl FROM CareLog cl " +
            "JOIN FETCH cl.customer " +
            "JOIN FETCH cl.calledBy " +
            "LEFT JOIN FETCH cl.campaign " +
            "LEFT JOIN FETCH cl.order " +
            "WHERE cl.customer.id = :customerId")
    Page<CareLog> findByCustomerId(@Param("customerId") Integer customerId, Pageable pageable);

    /**
     * API Tìm kiếm nâng cao kết hợp phân trang
     */
    @Query("SELECT cl FROM CareLog cl " +
            "JOIN FETCH cl.customer c " +
            "JOIN FETCH cl.calledBy u " +
            "WHERE (:searchKeyword IS NULL OR c.phone LIKE CONCAT('%', :searchKeyword, '%') " +
            "OR LOWER(c.fullName) LIKE LOWER(CONCAT('%', :searchKeyword, '%'))) " + // <-- Thêm tìm theo tên không phân biệt hoa thường
            "AND (:result IS NULL OR cl.result = :result) " +
            "AND (:potentialStatus IS NULL OR cl.potentialStatus = :potentialStatus) " +
            "AND (:fromDate IS NULL OR cl.calledAt >= :fromDate) " +
            "AND (:toDate IS NULL OR cl.calledAt <= :toDate)")
    Page<CareLog> searchCareLogs(
            @Param("searchKeyword") String searchKeyword, // Đổi tên từ phone thành searchKeyword cho đúng bản chất
            @Param("result") String result,
            @Param("potentialStatus") String potentialStatus,
            @Param("fromDate") Instant fromDate,
            @Param("toDate") Instant toDate,
            Pageable pageable
    );

    /**
     * Truy vấn chi tiết một bản ghi CareLog kèm toàn bộ mối quan hệ liên quan
     */
    @Query("SELECT cl FROM CareLog cl " +
            "LEFT JOIN FETCH cl.customer " +
            "LEFT JOIN FETCH cl.campaign " +
            "LEFT JOIN FETCH cl.calledBy " +
            "LEFT JOIN FETCH cl.order " +
            "WHERE cl.id = :id")
    Optional<CareLog> findDetailById(@Param("id") Integer id);
}