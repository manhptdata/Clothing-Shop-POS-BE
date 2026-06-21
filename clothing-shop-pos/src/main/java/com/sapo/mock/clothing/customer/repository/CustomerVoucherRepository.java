package com.sapo.mock.clothing.customer.repository;

import com.sapo.mock.clothing.entity.CustomerVoucher;
import org.springframework.data.jpa.repository.JpaRepository;
import java.time.Instant;
import java.util.List;

public interface CustomerVoucherRepository extends JpaRepository<CustomerVoucher, Integer> {

    // Kiểm tra tính từ mốc checkTimeLimit đến nay khách đã nhận voucher này chưa
    boolean existsByCustomerIdAndVoucherIdAndReceivedAtAfter(Integer customerId, Integer voucherId, Instant checkTimeLimit);

    // Lấy toàn bộ voucher của 1 khách hàng, sắp xếp mới nhất trước (dùng cho trang hồ sơ chi tiết)
    List<CustomerVoucher> findByCustomerIdOrderByReceivedAtDesc(Integer customerId);

    // Tìm voucher chưa sử dụng của khách hàng theo mã code
    @org.springframework.data.jpa.repository.Query("SELECT cv FROM CustomerVoucher cv JOIN cv.voucher v WHERE cv.customer.id = :customerId AND v.code = :voucherCode AND cv.status = 'UNUSED'")
    java.util.Optional<CustomerVoucher> findUnusedVoucherByCustomerAndCode(@org.springframework.data.repository.query.Param("customerId") Integer customerId, @org.springframework.data.repository.query.Param("voucherCode") String voucherCode);

    // Tìm voucher đã được áp dụng cho một đơn hàng cụ thể
    java.util.Optional<CustomerVoucher> findByOrderId(Integer orderId);
}