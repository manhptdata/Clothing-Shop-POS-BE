package com.sapo.mock.clothing.order.service;

import com.sapo.mock.clothing.customer.repository.CustomerRepository;
import com.sapo.mock.clothing.customer.repository.CustomerVoucherRepository;
import com.sapo.mock.clothing.customer.repository.PointHistoryRepository;
import com.sapo.mock.clothing.entity.Customer;
import com.sapo.mock.clothing.entity.CustomerVoucher;
import com.sapo.mock.clothing.entity.Order;
import com.sapo.mock.clothing.entity.PointHistory;
import com.sapo.mock.clothing.entity.Voucher;
import com.sapo.mock.clothing.exception.BadRequestException;
import com.sapo.mock.clothing.order.dto.ReqCreateOrderDTO;
import com.sapo.mock.clothing.util.constant.CustomerVoucherStatusEnum;
import com.sapo.mock.clothing.util.constant.PointConstant;
import com.sapo.mock.clothing.util.constant.VoucherCampaignStatusEnum;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.Instant;

@Service
@RequiredArgsConstructor
public class OrderLoyaltyService {

    private final CustomerRepository customerRepository;
    private final PointHistoryRepository pointHistoryRepository;
    private final CustomerVoucherRepository customerVoucherRepository;

    public CustomerVoucher applyVoucher(ReqCreateOrderDTO dto, Order order, Customer customer, BigDecimal currentTotal) {
        if (dto.getVoucherCode() == null || dto.getVoucherCode().trim().isEmpty()) {
            order.setVoucherCode(null);
            order.setDiscountFromVoucher(BigDecimal.ZERO);
            return null;
        }

        CustomerVoucher appliedVoucher = customerVoucherRepository
                .findUnusedVoucherByCustomerAndCode(customer.getId(), dto.getVoucherCode().trim())
                .orElseThrow(() -> new BadRequestException("Mã voucher không hợp lệ, không tồn tại hoặc đã được sử dụng"));

        Voucher voucher = appliedVoucher.getVoucher();
        // Cho phép khách hàng dùng voucher đã phát ngay cả khi chiến dịch (Voucher Campaign) bị khóa.
        // Khóa chiến dịch chỉ có ý nghĩa ngừng phát mới.
        if (appliedVoucher.getExpiredAt().isBefore(Instant.now())) {
            throw new BadRequestException("Voucher này đã hết hạn");
        }
        if (voucher.getMinOrderValue() != null && currentTotal.compareTo(voucher.getMinOrderValue()) < 0) {
            throw new BadRequestException("Đơn hàng chưa đạt giá trị tối thiểu (" + voucher.getMinOrderValue() + ") để dùng voucher này");
        }

        BigDecimal discount = BigDecimal.ZERO;
        if (com.sapo.mock.clothing.util.constant.VoucherDiscountType.PERCENTAGE.equals(voucher.getDiscountType())) {
            discount = currentTotal.multiply(voucher.getDiscountAmount()).divide(BigDecimal.valueOf(100), 2, java.math.RoundingMode.HALF_UP);
            if (voucher.getMaxDiscountAmount() != null && voucher.getMaxDiscountAmount().compareTo(BigDecimal.ZERO) > 0) {
                discount = discount.min(voucher.getMaxDiscountAmount());
            }
        } else {
            discount = voucher.getDiscountAmount();
        }
        discount = discount.min(currentTotal);
        order.setVoucherCode(voucher.getCode());
        order.setDiscountFromVoucher(discount);
        return appliedVoucher;
    }

    public void applyPoints(ReqCreateOrderDTO dto, Order order, Customer customer, BigDecimal currentTotal) {
        if (dto.getPointsToUse() == null || dto.getPointsToUse() <= 0) {
            order.setPointsUsed(0);
            order.setDiscountFromPoints(BigDecimal.ZERO);
            return;
        }

        if (customer.getRewardPoints() < dto.getPointsToUse()) {
            throw new BadRequestException("Khách hàng không đủ điểm. Điểm hiện tại: " + customer.getRewardPoints());
        }

        BigDecimal discount = BigDecimal.valueOf(dto.getPointsToUse()).multiply(PointConstant.REDEEM_RATE)
                .min(currentTotal);
        order.setPointsUsed(dto.getPointsToUse());
        order.setDiscountFromPoints(discount);
    }

    public void processLoyaltyOnCompletion(Order savedOrder, Customer customer, CustomerVoucher appliedVoucher) {
        if (appliedVoucher != null) {
            appliedVoucher.setStatus(CustomerVoucherStatusEnum.USED);
            appliedVoucher.setUsedAt(Instant.now());
            appliedVoucher.setOrderId(savedOrder.getId());
            customerVoucherRepository.save(appliedVoucher);
        }

        if (customer.getId() != 1) {
            if (savedOrder.getPointsUsed() > 0) {
                customer.setRewardPoints(customer.getRewardPoints() - savedOrder.getPointsUsed());
                savePointHistory(customer.getId(), savedOrder.getId(), -savedOrder.getPointsUsed(),
                        PointConstant.TYPE_REDEEM, "Sử dụng điểm cho đơn hàng " + savedOrder.getOrderNumber());
            }
            if (savedOrder.getPointsEarned() > 0) {
                customer.setRewardPoints(customer.getRewardPoints() + savedOrder.getPointsEarned());
                savePointHistory(customer.getId(), savedOrder.getId(), savedOrder.getPointsEarned(),
                        PointConstant.TYPE_EARN, "Tích điểm từ đơn hàng " + savedOrder.getOrderNumber());
            }
            customerRepository.save(customer);
        }
    }

    public void reserveVoucher(CustomerVoucher voucher, Integer orderId) {
        if (voucher != null) {
            voucher.setStatus(CustomerVoucherStatusEnum.RESERVED);
            voucher.setOrderId(orderId);
            customerVoucherRepository.save(voucher);
        }
    }

    public void revertLoyaltyOnCancel(Order savedOrder, Customer customer) {
        customerVoucherRepository.findByOrderId(savedOrder.getId()).ifPresent(cv -> {
            if (cv.getStatus() == CustomerVoucherStatusEnum.USED || cv.getStatus() == CustomerVoucherStatusEnum.RESERVED) {
                if (cv.getExpiredAt().isBefore(Instant.now())) {
                    cv.setStatus(CustomerVoucherStatusEnum.EXPIRED);
                } else {
                    cv.setStatus(CustomerVoucherStatusEnum.UNUSED);
                }
            }
            cv.setUsedAt(null);
            cv.setOrderId(null);
            customerVoucherRepository.save(cv);
        });

        if (customer.getId() != 1) {
            if (savedOrder.getPointsUsed() > 0) {
                customer.setRewardPoints(customer.getRewardPoints() + savedOrder.getPointsUsed());
                savePointHistory(customer.getId(), savedOrder.getId(), savedOrder.getPointsUsed(),
                        PointConstant.TYPE_REFUND, "Hoàn điểm do hủy đơn hàng " + savedOrder.getOrderNumber());
            }
            if (savedOrder.getPointsEarned() > 0) {
                customer.setRewardPoints(customer.getRewardPoints() - savedOrder.getPointsEarned());
                savePointHistory(customer.getId(), savedOrder.getId(), -savedOrder.getPointsEarned(),
                        PointConstant.TYPE_REFUND, "Trừ điểm tích lũy do hủy đơn hàng " + savedOrder.getOrderNumber());
            }
            customerRepository.save(customer);
        }
    }

    private void savePointHistory(Integer customerId, Integer orderId, int pointsChange, String type, String description) {
        PointHistory ph = new PointHistory();
        ph.setCustomerId(customerId);
        ph.setOrderId(orderId);
        ph.setPointsChange(pointsChange);
        ph.setType(type);
        ph.setDescription(description);
        pointHistoryRepository.save(ph);
    }

    public CustomerVoucher getAppliedVoucher(Integer orderId) {
        if (orderId == null) return null;
        return customerVoucherRepository.findByOrderId(orderId).orElse(null);
    }
}
