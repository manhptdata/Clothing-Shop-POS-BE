package com.sapo.mock.clothing.customer.repository;

import com.sapo.mock.clothing.entity.CareLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

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
            "LEFT JOIN FETCH cl.invoice")
    Page<CareLog> findAllCareLogs(Pageable pageable);

    /**
     * Tìm lịch sử chăm sóc riêng của một khách hàng (Dùng khi click vào chi tiết 1 khách)
     */
    @Query("SELECT cl FROM CareLog cl " +
            "JOIN FETCH cl.customer " +
            "JOIN FETCH cl.calledBy " +
            "LEFT JOIN FETCH cl.campaign " +
            "LEFT JOIN FETCH cl.invoice " +
            "WHERE cl.customer.id = :customerId")
    Page<CareLog> findByCustomerId(@Param("customerId") Integer customerId, Pageable pageable);
}