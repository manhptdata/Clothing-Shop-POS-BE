package com.sapo.mock.clothing.customer.service.scheduler;


import com.sapo.mock.clothing.customer.repository.CustomerRepository;
import com.sapo.mock.clothing.customer.repository.CustomerVoucherRepository;
import com.sapo.mock.clothing.customer.repository.VoucherRepository;
import com.sapo.mock.clothing.entity.Customer;
import com.sapo.mock.clothing.entity.CustomerVoucher;
import com.sapo.mock.clothing.entity.Voucher;
import com.sapo.mock.clothing.util.constant.CustomerStatusEnum;
import com.sapo.mock.clothing.util.constant.CustomerVoucherStatusEnum;
import com.sapo.mock.clothing.util.constant.RankCodeEnum;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.*;
import java.time.temporal.TemporalAdjusters;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class BirthdayVoucherScheduler {

    private final CustomerRepository customerRepository;
    private final VoucherRepository voucherRepository;
    private final CustomerVoucherRepository customerVoucherRepository;
    @Autowired
    private EmailNotificationService emailNotificationService;


    /**
     * Tự động kích hoạt vào 00:00:00 mỗi nửa đêm để quét và phát voucher tự động
     */
    @Scheduled(cron = "0 0 0 * * ?")
//    @Scheduled(fixedRate = 10000)
    @Transactional
    public void scanAndIssueBirthdayVouchers() {
        runNow();
    }

    /**
     * Logic thực thi chính - tách ra để có thể gọi từ API thủ công khi test
     */
    @Transactional
    public void runNow() {
        log.info(">>> [CRM VOUCHER] Bắt đầu tiến trình quét và phát hành Voucher sinh nhật tự động...");

        LocalDate today = LocalDate.now();
        int currentMonth = today.getMonthValue();

        // Mốc đầu tháng hiện tại để check trùng luồng Hạng Bạc
        Instant startOfMonth = today.with(TemporalAdjusters.firstDayOfMonth())
                .atStartOfDay(ZoneId.systemDefault())
                .toInstant();

        // Lấy dữ liệu 2 bản mẫu chiến dịch voucher từ DB
        // Lấy danh sách khách hàng ACTIVE sinh nhật trong tháng hiện tại
        List<Customer> birthdayCustomers = customerRepository.findActiveCustomersByBirthMonth(currentMonth, CustomerStatusEnum.ACTIVE);

        for (Customer customer : birthdayCustomers) {
            // Bỏ qua nếu khách chưa có nhóm hoặc nhóm chưa cài voucher sinh nhật
            if (customer.getCustomerGroup() == null) continue;

            com.sapo.mock.clothing.entity.Voucher voucher = customer.getCustomerGroup().getBirthdayVoucher();
            if (voucher == null) {
                log.info(">> [CRM VOUCHER] Nhóm [{}] chưa cài voucher sinh nhật. Bỏ qua: {}",
                        customer.getCustomerGroup().getName(), customer.getFullName());
                continue;
            }

            if (voucher.getStatus() != com.sapo.mock.clothing.util.constant.VoucherCampaignStatusEnum.ACTIVE) {
                log.info(">> [CRM VOUCHER] Voucher sinh nhật của nhóm [{}] đang TẠM DỪNG (INACTIVE). Bỏ qua phát cho khách: {}",
                        customer.getCustomerGroup().getName(), customer.getFullName());
                continue;
            }

            // Phát voucher (tự kiểm tra trùng lặp trong tháng)
            issueVoucherIfNotExist(customer, voucher, startOfMonth, getEndOfMonthInstant(today));
        }
        log.info(">>> [CRM VOUCHER] Kết thúc tiến trình tự động quét phát hành Voucher an toàn.");
    }

    /**
     * Hàm phụ trợ kiểm tra trùng lặp theo mốc thời gian động và lưu vào DB
     */
    private void issueVoucherIfNotExist(Customer customer, Voucher voucher, Instant checkTimeLimit, Instant expiredAt) {
        // Chỉ kiểm tra xem trong tháng này khách đã nhận voucher chưa (để sang năm khách vẫn nhận được tiếp)
        boolean alreadyIssuedThisMonth = customerVoucherRepository.existsByCustomerIdAndVoucherIdAndReceivedAtAfter(
                customer.getId(), voucher.getId(), checkTimeLimit);

        if (!alreadyIssuedThisMonth) {
            CustomerVoucher cv = new CustomerVoucher();
            cv.setCustomer(customer);
            cv.setVoucher(voucher);
            cv.setStatus(CustomerVoucherStatusEnum.UNUSED);
            cv.setReceivedAt(Instant.now());
            cv.setExpiredAt(expiredAt);

            customerVoucherRepository.save(cv);
            log.info(">> CRM PRO SUCCESS: Đã chuyển Voucher [{}] vào ví của khách hàng: {}",
                    voucher.getName(), customer.getFullName());

            // Bug #17 fix: Chỉ gửi email nếu khách hàng có email hợp lệ. Không fallback sang email dev.
            if (customer.getEmail() != null && !customer.getEmail().isBlank()) {
                final String recipientEmail = customer.getEmail();
                java.util.concurrent.CompletableFuture.runAsync(() -> {
                    emailNotificationService.sendBirthdayVoucherEmail(recipientEmail, customer.getFullName(), voucher.getCode());
                });
            } else {
                log.info(">> [CRM VOUCHER] Khách [{}] không có email, bỏ qua gửi mail sinh nhật.", customer.getFullName());
            }
        }
    }

    private Instant getEndOfDayInstant(LocalDate date) {
        return date.atTime(23, 59, 59).atZone(ZoneId.systemDefault()).toInstant();
    }

    private Instant getEndOfMonthInstant(LocalDate date) {
        LocalDate lastDay = date.with(TemporalAdjusters.lastDayOfMonth());
        return lastDay.atTime(23, 59, 59).atZone(ZoneId.systemDefault()).toInstant();
    }


}