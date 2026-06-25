package com.sapo.mock.clothing.customer.repository;

import com.sapo.mock.clothing.entity.Customer;
import com.sapo.mock.clothing.entity.Order;
import com.sapo.mock.clothing.util.constant.CustomerStatusEnum;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

@Repository
public interface CustomerRepository extends JpaRepository<Customer, Integer>, JpaSpecificationExecutor<Customer> {
        // Search ACTIVE customers by keyword (case-insensitive).
        @Query("SELECT c FROM Customer c WHERE c.status = 'ACTIVE' AND " +
                        "(LOWER(c.fullName) LIKE LOWER(CONCAT('%', :keyword, '%')) OR c.phone LIKE CONCAT('%', :keyword, '%'))")
        Page<Customer> searchActiveCustomers(@Param("keyword") String keyword, Pageable pageable);

        @Query(value = "SELECT c.* FROM customer c " +
                        "LEFT JOIN customer_group cg ON c.customer_group_id = cg.id " +
                        "WHERE c.status = 'ACTIVE' AND (" +
                        "LPAD(MONTH(c.date_of_birth), 2, '0') = :month " +
                        "OR CAST(MONTH(c.date_of_birth) AS CHAR) = :month)",

                        countQuery = "SELECT COUNT(*) FROM customer c WHERE c.status = 'ACTIVE' AND (" +
                                        "LPAD(MONTH(c.date_of_birth), 2, '0') = :month " +
                                        "OR CAST(MONTH(c.date_of_birth) AS CHAR) = :month)", nativeQuery = true)
        Page<Customer> searchByBirthMonth(@Param("month") String month, Pageable pageable);

        // Used for automatic phone number duplicate validation.
        boolean existsByPhone(String phone);

        // Check if the phone number is used by another customer (excluding the current
        // ID).
        boolean existsByPhoneAndIdNot(String phone, Integer id);

        // detail customer by id, only if ACTIVE
        @Query("SELECT c FROM Customer c " +
                        "LEFT JOIN FETCH c.customerGroup " +
                        "WHERE c.customerGroup.id = :groupId " +
                        "AND c.status = com.sapo.mock.clothing.util.constant.CustomerStatusEnum.ACTIVE")
        Page<Customer> findByCustomerGroupId(@Param("groupId") Integer groupId, Pageable pageable);

        // Thêm câu query này vào file CustomerRepository.java hiện tại của bạn
        @Query("SELECT c FROM Customer c WHERE c.status = :status " +
                        "AND c.dateOfBirth IS NOT NULL " +
                        "AND FUNCTION('MONTH', c.dateOfBirth) = :month")
        List<Customer> findActiveCustomersByBirthMonth(
                        @Param("month") int month,
                        @Param("status") com.sapo.mock.clothing.util.constant.CustomerStatusEnum status);

        @Query("SELECT o FROM Order o WHERE o.customerId = :customerId")
        Page<Order> findOrdersByCustomerId(@Param("customerId") Integer customerId, Pageable pageable);

        // 🌟 MỚI BỔ SUNG 1: Lấy danh sách khách hàng đang hoạt động để chạy quét hạ
        // hạng ngầm
        List<Customer> findByStatus(com.sapo.mock.clothing.util.constant.CustomerStatusEnum status);

        // 🌟 MỚI BỔ SUNG 2: Tính tổng doanh số mua hàng thành công (COMPLETED) trong
        // vòng 12 tháng (365 ngày) qua
        @Query("SELECT COALESCE(SUM(o.totalAmount), 0) FROM Order o " +
                        "WHERE o.customerId = :customerId " +
                        "AND o.status = com.sapo.mock.clothing.util.constant.OrderStatus.COMPLETED " +
                        "AND o.createdAt >= :startDate")
        java.math.BigDecimal calculateSpendingInTimeRange(@Param("customerId") Integer customerId,
                        @Param("startDate") java.time.Instant startDate);

        long countByCreatedAtBetween(java.time.Instant start, java.time.Instant end);
}
