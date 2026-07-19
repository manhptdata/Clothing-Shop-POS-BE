package com.sapo.mock.clothing.customer.repository;

import com.sapo.mock.clothing.entity.CustomerVoucher;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.springframework.data.repository.query.Param;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import com.sapo.mock.clothing.customer.dto.response.CustomerVoucherHistoryResponse;

public interface CustomerVoucherRepository extends JpaRepository<CustomerVoucher, Integer> {

    // Kiểm tra tính từ mốc checkTimeLimit đến nay khách đã nhận voucher này chưa
    boolean existsByCustomerIdAndVoucherIdAndReceivedAtAfter(Integer customerId, Integer voucherId,
            Instant checkTimeLimit);

    // Kiểm tra xem khách hàng ĐÃ TỪNG nhận voucher này chưa (bất kể trạng thái hay thời gian)
    boolean existsByCustomerIdAndVoucherId(Integer customerId, Integer voucherId);

    // Lấy toàn bộ voucher của 1 khách hàng, sắp xếp mới nhất trước (dùng cho trang
    // hồ sơ chi tiết)
    List<CustomerVoucher> findByCustomerIdOrderByReceivedAtDesc(Integer customerId);

    // Tìm voucher chưa sử dụng và CÒN HẠN của khách hàng theo mã code
    @org.springframework.data.jpa.repository.Lock(jakarta.persistence.LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT cv FROM CustomerVoucher cv JOIN cv.voucher v WHERE cv.customer.id = :customerId AND v.code = :voucherCode AND cv.status = 'UNUSED' AND cv.expiredAt > CURRENT_TIMESTAMP")
    Optional<CustomerVoucher> findUnusedVoucherByCustomerAndCodeForUpdate(
            @Param("customerId") Integer customerId,
            @Param("voucherCode") String voucherCode);

    // Tìm voucher đã được áp dụng cho một đơn hàng cụ thể
    Optional<CustomerVoucher> findByOrderId(Integer orderId);

    @Query("SELECT cv FROM CustomerVoucher cv JOIN FETCH cv.voucher v WHERE cv.orderId = :orderId")
    Optional<CustomerVoucher> findByOrderIdWithVoucher(@Param("orderId") Integer orderId);

    @Query("SELECT new com.sapo.mock.clothing.customer.dto.response.CustomerVoucherHistoryResponse(" +
           "cv.id, c.id, c.fullName, c.phone, v.name, v.code, cv.receivedAt, cv.expiredAt, cv.usedAt, cast(cv.status as string)) " +
           "FROM CustomerVoucher cv " +
           "JOIN cv.customer c " +
           "JOIN cv.voucher v " +
           "WHERE (:keyword IS NULL OR :keyword = '' " +
           "OR LOWER(c.fullName) LIKE LOWER(CONCAT('%', :keyword, '%')) " +
           "OR c.phone LIKE CONCAT('%', :keyword, '%') " +
           "OR LOWER(v.code) LIKE LOWER(CONCAT('%', :keyword, '%')) " +
           "OR LOWER(v.name) LIKE LOWER(CONCAT('%', :keyword, '%')))")
    Page<CustomerVoucherHistoryResponse> searchHistory(
            @Param("keyword") String keyword,
            Pageable pageable);
    @org.springframework.data.jpa.repository.Lock(jakarta.persistence.LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT cv FROM CustomerVoucher cv WHERE cv.id = :id")
    Optional<CustomerVoucher> findByIdWithPessimisticLock(@Param("id") Integer id);
}