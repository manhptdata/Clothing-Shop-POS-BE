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
        log.info(">>> [CRM VOUCHER] Bắt đầu tiến trình quét và phát hành Voucher sinh nhật tự động...");

        LocalDate today = LocalDate.now();
        int currentMonth = today.getMonthValue();
        int currentDay = today.getDayOfMonth();

        // Mốc đầu tháng hiện tại để check trùng luồng Hạng Bạc
        Instant startOfMonth = today.with(TemporalAdjusters.firstDayOfMonth())
                .atStartOfDay(ZoneId.systemDefault())
                .toInstant();

        // Mốc đầu ngày hôm nay để check trùng luồng Hạng Vàng
        Instant startOfToday = today.atStartOfDay(ZoneId.systemDefault())
                .toInstant();

        // Lấy dữ liệu 2 bản mẫu chiến dịch voucher từ DB
        Voucher silverVoucher = voucherRepository.findByCode("CMSNBAC50K").orElse(null);
        Voucher goldVoucher = voucherRepository.findByCode("CMSNVANG100K").orElse(null);

        // Lấy danh sách khách hàng ACTIVE sinh nhật trong tháng hiện tại
        List<Customer> birthdayCustomers = customerRepository.findActiveCustomersByBirthMonth(currentMonth, CustomerStatusEnum.ACTIVE);

        for (Customer customer : birthdayCustomers) {
            if (customer.getCustomerGroup() == null) continue;

            RankCodeEnum rank = customer.getCustomerGroup().getCode();

            // 1. LUỒNG HẠNG BẠC (SILVER): Quét liên tục mỗi ngày trong tháng sinh nhật (tự động phát bù/thăng hạng muộn)
            if (rank == RankCodeEnum.SILVER && silverVoucher != null) {
                issueVoucherIfNotExist(customer, silverVoucher, startOfMonth, getEndOfMonthInstant(today));
            }

            // 2. LUỒNG HẠNG VÀNG (GOLD): Nhận và dùng cả tháng sinh nhật (Giống Hạng Bạc)
            if (rank == RankCodeEnum.GOLD && goldVoucher != null) {
                issueVoucherIfNotExist(customer, goldVoucher, startOfMonth, getEndOfMonthInstant(today));
            }
        }
        log.info(">>> [CRM VOUCHER] Kết thúc tiến trình tự động quét phát hành Voucher an toàn.");
    }

    /**
     * Hàm phụ trợ kiểm tra trùng lặp theo mốc thời gian động và lưu vào DB
     */
    private void issueVoucherIfNotExist(Customer customer, Voucher voucher, Instant checkTimeLimit, Instant expiredAt) {
        boolean alreadyIssued = customerVoucherRepository.existsByCustomerIdAndVoucherIdAndReceivedAtAfter(
                customer.getId(), voucher.getId(), checkTimeLimit);

        if (!alreadyIssued) {
            CustomerVoucher cv = new CustomerVoucher();
            cv.setCustomer(customer);
            cv.setVoucher(voucher);
            cv.setStatus(CustomerVoucherStatusEnum.UNUSED);
            cv.setReceivedAt(Instant.now());
            cv.setExpiredAt(expiredAt);

            customerVoucherRepository.save(cv);
            log.info(">> CRM PRO SUCCESS: Đã chuyển Voucher [{}] vào ví của khách hàng: {}",
                    voucher.getName(), customer.getFullName());

            String testEmail = (customer.getEmail() != null) ? customer.getEmail() : "manhwakunchi@gmail.com";
            emailNotificationService.sendBirthdayVoucherEmail(testEmail, customer.getFullName(), voucher.getCode());
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