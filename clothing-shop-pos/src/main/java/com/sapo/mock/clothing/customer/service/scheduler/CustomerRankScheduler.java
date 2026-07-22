package com.sapo.mock.clothing.customer.service.scheduler;

import com.sapo.mock.clothing.customer.repository.CustomerGroupRepository;
import com.sapo.mock.clothing.customer.repository.CustomerRepository;
import com.sapo.mock.clothing.entity.Customer;
import com.sapo.mock.clothing.entity.CustomerGroup;
import com.sapo.mock.clothing.util.constant.CustomerStatusEnum;
import com.sapo.mock.clothing.util.constant.RankCodeEnum;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;

@Slf4j
@Service
public class CustomerRankScheduler {

    @Autowired
    private CustomerRepository customerRepository;

    @Autowired
    private CustomerGroupRepository customerGroupRepository;

    /**
     * Tự động chạy quét ngầm rà soát doanh số và hạ hạng khách hàng khi hết chu kỳ 12 tháng.
     * fixedRate = 60000 (1 phút quét một lần) để Đức chạy chạy thử nghiệm log in ra màn hình Console luôn.
     * Khi mang đi nộp bài đồ án, Đức đổi cấu hình thành: cron = "0 0 0 * * ?" (Chạy đúng 00:00:00 nửa đêm).
     */
//    @Scheduled(fixedRate = 30000)
    @Scheduled(cron = "0 0 0 * * ?")
    @Transactional
    public void scanAndDowngradeCustomerRanks() {
        log.info(">>> [CRM RANK] Bắt đầu tiến trình tự động rà soát hạ hạng khách hàng định kỳ...");

        // Thiết lập mốc thời gian kiểm toán: Quay ngược lại 365 ngày trước kể từ bây giờ
        Instant oneYearAgo = Instant.now().minus(365, ChronoUnit.DAYS);
        //  hỉ quét các hóa đơn được tạo trong vòng 20 giây vừa qua
//        Instant oneYearAgo = Instant.now().minus(20, ChronoUnit.SECONDS);

        // Tải danh sách nhóm cấu hình và danh sách khách hàng đang hoạt động lên RAM
        List<CustomerGroup> activeGroups = customerGroupRepository.findByStatus(CustomerStatusEnum.ACTIVE);
        List<Customer> activeCustomers = customerRepository.findByStatus(CustomerStatusEnum.ACTIVE);

        for (Customer customer : activeCustomers) {
            try {
                // Kiểm tra an toàn bảo vệ hệ thống tránh lỗi dữ liệu null cấu hình hạng của khách
                if (customer.getCustomerGroup() == null || customer.getCustomerGroup().getCode() == null) {
                    continue;
                }

                CustomerGroup currentGroup = customer.getCustomerGroup();
                RankCodeEnum currentRank = currentGroup.getCode();

                // Tính tổng chi tiêu tích lũy của khách hàng này trong 12 tháng qua bằng hàm ở Bước 1
                BigDecimal spendingInLastYear = customerRepository.calculateSpendingInTimeRange(customer.getId(), oneYearAgo);
                RankCodeEnum targetRank = currentRank;

                // Lấy ngưỡng minSpending cấu hình trong DB cho từng hạng
                BigDecimal goldMinSpend = activeGroups.stream()
                        .filter(g -> g.getCode() == RankCodeEnum.GOLD)
                        .map(CustomerGroup::getMinSpending)
                        .filter(java.util.Objects::nonNull)
                        .findFirst().orElse(new BigDecimal("20000000"));

                BigDecimal silverMinSpend = activeGroups.stream()
                        .filter(g -> g.getCode() == RankCodeEnum.SILVER)
                        .map(CustomerGroup::getMinSpending)
                        .filter(java.util.Objects::nonNull)
                        .findFirst().orElse(new BigDecimal("5000000"));

                if (currentRank == RankCodeEnum.GOLD) {
                    if (spendingInLastYear.compareTo(goldMinSpend) >= 0) {
                        targetRank = RankCodeEnum.GOLD;
                    } else if (spendingInLastYear.compareTo(silverMinSpend) >= 0) {
                        targetRank = RankCodeEnum.SILVER;
                    } else {
                        targetRank = RankCodeEnum.BRONZE;
                    }
                } else if (currentRank == RankCodeEnum.SILVER) {
                    if (spendingInLastYear.compareTo(silverMinSpend) >= 0) {
                        targetRank = RankCodeEnum.SILVER;
                    } else {
                        targetRank = RankCodeEnum.BRONZE;
                    }
                }

                // Nếu tính toán phát hiện thứ hạng mới bị suy giảm, tiến hành ghi nhận xuống DB
                if (targetRank != currentRank) {
                    RankCodeEnum finalTargetRank = targetRank;
                    CustomerGroup matchedGroup = activeGroups.stream()
                            .filter(g -> g.getCode() == finalTargetRank)
                            .findFirst()
                            .orElse(currentGroup); // Dự phòng nếu DB thiếu nhóm cấu hình thì giữ nhóm cũ để tránh sập app

                    log.warn(">> [CRM DOWNGRADE ALERT]: Khách hàng [{}] bị HẠ HẠNG! Chi tiêu 12 tháng qua: {}đ. Từ hạng [{}] xuống hạng [{}]",
                            customer.getFullName(), spendingInLastYear, currentRank, targetRank);

                    // Re-fetch với Pessimistic Lock trước khi cập nhật để tránh Lost Update
                    Customer lockedCustomer = customerRepository.findByIdWithPessimisticLock(customer.getId()).orElse(null);
                    if (lockedCustomer != null) {
                        // Cập nhật nhóm mới và lưu lại
                        lockedCustomer.setCustomerGroup(matchedGroup);
                        customerRepository.save(lockedCustomer);
                    }
                } else {
                    log.info(">> [CRM RANK MAINTENANCE]: Khách hàng [{}] giữ vững hạng [{}]. Chi tiêu 12 tháng qua: {}đ",
                            customer.getFullName(), currentRank, spendingInLastYear);
                }

            } catch (Exception e) {
                log.error("Lỗi xảy ra khi rà soát hạng của khách hàng ID {}: {}", customer.getId(), e.getMessage());
            }
        }
        log.info(">>> [CRM RANK] Tiến trình tự động quét kiểm tra kết thúc thành công.");
    }
}