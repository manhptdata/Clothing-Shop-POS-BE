package com.sapo.mock.clothing.order.service;

import com.sapo.mock.clothing.customer.repository.CustomerRepository;
import com.sapo.mock.clothing.customer.repository.CustomerVoucherRepository;
import com.sapo.mock.clothing.customer.repository.PointHistoryRepository;
import com.sapo.mock.clothing.customer.repository.VoucherRepository;
import com.sapo.mock.clothing.customer.repository.VoucherUsageRepository;
import com.sapo.mock.clothing.entity.Customer;
import com.sapo.mock.clothing.entity.CustomerVoucher;
import com.sapo.mock.clothing.entity.Order;
import com.sapo.mock.clothing.entity.PointHistory;
import com.sapo.mock.clothing.entity.Voucher;
import com.sapo.mock.clothing.entity.VoucherUsage;
import com.sapo.mock.clothing.exception.BadRequestException;
import com.sapo.mock.clothing.exception.ResourceNotFoundException;
import com.sapo.mock.clothing.order.dto.ReqCreateOrderDTO;
import com.sapo.mock.clothing.order.repository.OrderRepository;
import com.sapo.mock.clothing.util.constant.CustomerVoucherStatusEnum;
import com.sapo.mock.clothing.util.constant.OrderStatus;
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
    private final VoucherRepository voucherRepository;
    private final OrderRepository orderRepository;
    private final VoucherUsageRepository voucherUsageRepository;

    public CustomerVoucher applyVoucher(ReqCreateOrderDTO dto, Order order, Customer customer, BigDecimal currentTotal) {
        return applyVoucher(dto, order, customer, currentTotal, false);
    }

    public CustomerVoucher applyVoucher(ReqCreateOrderDTO dto, Order order, Customer customer, BigDecimal currentTotal, boolean isSamePublicVoucher) {
        if (dto.getVoucherCode() == null || dto.getVoucherCode().trim().isEmpty()) {
            order.setVoucherCode(null);
            order.setDiscountFromVoucher(BigDecimal.ZERO);
            return null;
        }

        String code = dto.getVoucherCode().trim();

        // 1. Ưu tiên kiểm tra trong ví cá nhân của khách hàng (Private Wallet Voucher)
        CustomerVoucher appliedWalletVoucher = null;
        if (order.getId() != null) {
            appliedWalletVoucher = customerVoucherRepository
                    .findUnusedOrReservedVoucherByCustomerAndCodeForUpdate(customer.getId(), code, order.getId())
                    .orElse(null);
        } else {
            appliedWalletVoucher = customerVoucherRepository
                    .findUnusedVoucherByCustomerAndCodeForUpdate(customer.getId(), code)
                    .orElse(null);
        }

        Voucher voucher = null;

        if (appliedWalletVoucher != null) {
            voucher = appliedWalletVoucher.getVoucher();
            if (!isSamePublicVoucher && appliedWalletVoucher.getExpiredAt().isBefore(Instant.now())) {
                throw new BadRequestException("Voucher này trong ví của bạn đã hết hạn");
            }
        } else {
            // 2. Nếu không có trong ví, kiểm tra Mã giảm giá công khai (Public Voucher) mà KHÔNG khóa DB bi quan (Tránh nghẽn Flash Sale)
            voucher = voucherRepository.findByCode(code)
                    .orElseThrow(() -> new BadRequestException("Mã voucher không hợp lệ hoặc không tồn tại"));

            if (voucher.getStatus() != VoucherCampaignStatusEnum.ACTIVE) {
                throw new BadRequestException("Chương trình voucher này hiện đang tạm dừng");
            }

            if (Boolean.FALSE.equals(voucher.getIsPublic())) {
                if (voucher.getTargetCustomerGroupId() != null) {
                    if (customer == null || customer.getId() == 1 || customer.getCustomerGroup() == null) {
                        throw new BadRequestException("Voucher dành riêng này yêu cầu chọn thành viên có hạng phù hợp");
                    }
                    if (!voucher.getTargetCustomerGroupId().equals(customer.getCustomerGroup().getId())) {
                        throw new BadRequestException("Voucher này chỉ dành cho nhóm khách hàng khác (Khách hàng hiện tại: " + customer.getCustomerGroup().getName() + ")");
                    }
                } else {
                    throw new BadRequestException("Mã voucher này chỉ dành cho khách hàng được cấp phát riêng vào ví");
                }
            }

            Instant now = Instant.now();
            if (voucher.getStartDate() != null && now.isBefore(voucher.getStartDate())) {
                throw new BadRequestException("Chương trình voucher chưa tới thời gian áp dụng");
            }

            if (!isSamePublicVoucher && voucher.getEndDate() != null && now.isAfter(voucher.getEndDate())) {
                throw new BadRequestException("Mã voucher đã hết hạn sử dụng");
            }

            // Nếu đơn hàng đang cập nhật giữ nguyên voucher cũ, không check kiềm chế hết lượt vì đơn này đã nằm trong usedQuantity
            if (!isSamePublicVoucher) {
                if (voucher.getTotalQuantity() != null && (voucher.getUsedQuantity() == null ? 0 : voucher.getUsedQuantity()) >= voucher.getTotalQuantity()) {
                    throw new BadRequestException("Mã voucher này đã hết lượt sử dụng");
                }
            }

            VoucherUsage usage = null;
            if (voucher.getMaxUsagePerUser() != null && voucher.getMaxUsagePerUser() > 0) {
                if (customer == null || customer.getId() == 1) {
                    throw new BadRequestException("Mã voucher này giới hạn " + voucher.getMaxUsagePerUser()
                            + " lần/khách hàng, không thể áp dụng cho Khách vãng lai. Vui lòng chọn hoặc tạo tài khoản khách hàng.");
                }

                // Lock Customer để serialize các request áp dụng mã của cùng 1 user
                customerRepository.findByIdWithPessimisticLock(customer.getId());

                usage = voucherUsageRepository.findByCustomerIdAndVoucherCodeWithPessimisticLock(customer.getId(), code)
                        .orElseGet(() -> {
                            VoucherUsage vu = new VoucherUsage();
                            vu.setCustomerId(customer.getId());
                            vu.setVoucherCode(code);
                            vu.setUsageCount(0);
                            return vu;
                        });

                if (!isSamePublicVoucher && usage.getUsageCount() >= voucher.getMaxUsagePerUser()) {
                    throw new BadRequestException("Bạn đã sử dụng tối đa lượt cho phép (" + voucher.getMaxUsagePerUser() + " lần) của mã giảm giá này");
                }
            }

            // RESERVE: Chỉ tăng usedQuantity nếu đơn hàng được hoàn tất (COMPLETED) trực tiếp trong cùng luồng
            if (!isSamePublicVoucher && order.getStatus() == OrderStatus.COMPLETED) {
                int rowsUpdated = voucherRepository.incrementUsedQuantity(code);
                if (rowsUpdated == 0) {
                    throw new BadRequestException("Mã voucher này đã hết lượt sử dụng hoặc không khả dụng");
                }

                if (usage != null) {
                    usage.setUsageCount(usage.getUsageCount() + 1);
                    voucherUsageRepository.save(usage);
                }
            }
        }

        if (voucher.getMinOrderValue() != null && currentTotal.compareTo(voucher.getMinOrderValue()) < 0) {
            throw new BadRequestException("Đơn hàng chưa đạt giá trị tối thiểu (" + voucher.getMinOrderValue() + " VNĐ) để dùng voucher này");
        }

        BigDecimal discount = BigDecimal.ZERO;
        if (com.sapo.mock.clothing.util.constant.VoucherDiscountType.PERCENTAGE.equals(voucher.getDiscountType())) {
            discount = currentTotal.multiply(voucher.getDiscountAmount()).divide(BigDecimal.valueOf(100), 0, java.math.RoundingMode.HALF_UP);
            if (voucher.getMaxDiscountAmount() != null && voucher.getMaxDiscountAmount().compareTo(BigDecimal.ZERO) > 0) {
                discount = discount.min(voucher.getMaxDiscountAmount());
            }
        } else {
            discount = voucher.getDiscountAmount();
        }
        discount = discount.min(currentTotal);
        order.setVoucherCode(voucher.getCode());
        order.setDiscountFromVoucher(discount);

        return appliedWalletVoucher;
    }

    public void applyPoints(ReqCreateOrderDTO dto, Order order, Customer customer, BigDecimal currentTotal) {
        if (dto.getPointsToUse() == null || dto.getPointsToUse() <= 0 || customer.getId() == 1) {
            order.setPointsUsed(0);
            order.setDiscountFromPoints(BigDecimal.ZERO);
            return;
        }

        Customer lockedCustomer = customerRepository.findByIdWithPessimisticLock(customer.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy khách hàng ID: " + customer.getId()));

        if (lockedCustomer.getRewardPoints() < dto.getPointsToUse()) {
            throw new BadRequestException("Khách hàng không đủ điểm. Điểm hiện tại: " + lockedCustomer.getRewardPoints());
        }

        BigDecimal discount = BigDecimal.valueOf(dto.getPointsToUse()).multiply(PointConstant.REDEEM_RATE)
                .min(currentTotal);
        // Tính lại số điểm thực tế bị trừ dựa trên số tiền giảm giá thực tế (làm tròn lên để tránh nuốt tiền cửa hàng)
        int actualPointsUsed = discount.divide(PointConstant.REDEEM_RATE, 0, java.math.RoundingMode.UP).intValue();
        order.setPointsUsed(actualPointsUsed);
        order.setDiscountFromPoints(discount);

        // RESERVE: Trừ điểm ngay để ngăn double spending
        lockedCustomer.setRewardPoints(lockedCustomer.getRewardPoints() - actualPointsUsed);
        customerRepository.save(lockedCustomer);
    }

    public void processLoyaltyOnCompletion(Order savedOrder, Customer customerParam, CustomerVoucher appliedVoucherParam) {
        if (appliedVoucherParam != null) {
            CustomerVoucher lockedVoucher = customerVoucherRepository.findByIdWithPessimisticLock(appliedVoucherParam.getId())
                    .orElseThrow(() -> new BadRequestException("Voucher không tồn tại"));
            if (lockedVoucher.getStatus() == CustomerVoucherStatusEnum.USED) {
                throw new BadRequestException("Voucher này đã được sử dụng cho đơn hàng khác.");
            }
            lockedVoucher.setStatus(CustomerVoucherStatusEnum.USED);
            lockedVoucher.setUsedAt(Instant.now());
            lockedVoucher.setOrderId(savedOrder.getId());
            customerVoucherRepository.save(lockedVoucher);
        } else if (savedOrder.getVoucherCode() != null && !savedOrder.getVoucherCode().isBlank()) {
            // Tăng usedQuantity của Public Voucher khi đơn hàng hoàn thành thanh toán
            voucherRepository.incrementUsedQuantity(savedOrder.getVoucherCode());
            if (customerParam != null && customerParam.getId() != 1) {
                VoucherUsage usage = voucherUsageRepository.findByCustomerIdAndVoucherCodeWithPessimisticLock(customerParam.getId(), savedOrder.getVoucherCode())
                        .orElseGet(() -> {
                            VoucherUsage vu = new VoucherUsage();
                            vu.setCustomerId(customerParam.getId());
                            vu.setVoucherCode(savedOrder.getVoucherCode());
                            vu.setUsageCount(0);
                            return vu;
                        });
                usage.setUsageCount(usage.getUsageCount() + 1);
                voucherUsageRepository.save(usage);
            }
        }

        if (customerParam.getId() != 1) {
            // Re-fetch với Pessimistic Lock để đảm bảo không bị Lost Update
            // khi 2 đơn hàng của cùng 1 khách được xử lý cùng lúc
            Customer customer = customerRepository.findByIdWithPessimisticLock(customerParam.getId())
                    .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy khách hàng ID: " + customerParam.getId()));

            if (savedOrder.getPointsUsed() > 0) {
                savePointHistory(customer.getId(), savedOrder.getId(), -savedOrder.getPointsUsed(),
                        PointConstant.TYPE_REDEEM, "Xác nhận sử dụng điểm cho đơn hàng " + savedOrder.getOrderNumber());
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

    public void revertLoyaltyOnUpdate(Order savedOrder, Customer customerParam, String newVoucherCode) {
        String oldCode = savedOrder.getVoucherCode();
        boolean isSameVoucher = oldCode != null && !oldCode.isBlank()
                && oldCode.trim().equalsIgnoreCase(newVoucherCode != null ? newVoucherCode.trim() : "");

        if (isSameVoucher) {
            // Chỉ hoàn lại điểm tích lũy/sử dụng, giữ nguyên voucher slot vì đơn hàng vẫn tiếp tục dùng mã này
            if (customerParam != null && customerParam.getId() != 1) {
                Customer customer = customerRepository.findByIdWithPessimisticLock(customerParam.getId())
                        .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy khách hàng ID: " + customerParam.getId()));

                if (savedOrder.getPointsUsed() > 0) {
                    customer.setRewardPoints(customer.getRewardPoints() + savedOrder.getPointsUsed());
                    savePointHistory(customer.getId(), savedOrder.getId(), savedOrder.getPointsUsed(),
                            PointConstant.TYPE_REFUND, "Hoàn điểm do cập nhật đơn hàng " + savedOrder.getOrderNumber());
                }
                if (savedOrder.getPointsEarned() > 0) {
                    customer.setRewardPoints(customer.getRewardPoints() - savedOrder.getPointsEarned());
                    savePointHistory(customer.getId(), savedOrder.getId(), -savedOrder.getPointsEarned(),
                            PointConstant.TYPE_REFUND, "Trừ điểm tích lũy do cập nhật đơn hàng " + savedOrder.getOrderNumber());
                }
                customerRepository.save(customer);
            }
        } else {
            revertLoyaltyOnCancel(savedOrder, customerParam);
        }
    }

    public void revertVoucherOnlyOnCancel(Order savedOrder, Customer customerParam) {
        CustomerVoucher appliedWalletVoucher = customerVoucherRepository.findByOrderId(savedOrder.getId()).orElse(null);

        if (appliedWalletVoucher != null) {
            if (appliedWalletVoucher.getStatus() == CustomerVoucherStatusEnum.USED || appliedWalletVoucher.getStatus() == CustomerVoucherStatusEnum.RESERVED) {
                if (appliedWalletVoucher.getExpiredAt().isBefore(Instant.now())) {
                    // Grace Period: ân hạn gia hạn 60 phút cho Voucher cá nhân bị trôi giờ khi hủy đơn PENDING
                    appliedWalletVoucher.setExpiredAt(Instant.now().plus(1, java.time.temporal.ChronoUnit.HOURS));
                    appliedWalletVoucher.setStatus(CustomerVoucherStatusEnum.UNUSED);
                } else {
                    appliedWalletVoucher.setStatus(CustomerVoucherStatusEnum.UNUSED);
                }
            }
            appliedWalletVoucher.setUsedAt(null);
            appliedWalletVoucher.setOrderId(null);
            customerVoucherRepository.save(appliedWalletVoucher);
        } else if (savedOrder.getVoucherCode() != null && !savedOrder.getVoucherCode().isBlank()) {
            if (savedOrder.getStatus() == OrderStatus.COMPLETED) {
                // Hoàn lại số lượng đã sử dụng (usedQuantity) cho Public Voucher khi hủy đơn đã COMPLETED bằng Atomic SQL
                voucherRepository.decrementUsedQuantity(savedOrder.getVoucherCode());

                // Hoàn lại usageCount trong VoucherUsage cho khách hàng
                if (customerParam != null && customerParam.getId() != 1) {
                    voucherUsageRepository.findByCustomerIdAndVoucherCodeWithPessimisticLock(customerParam.getId(), savedOrder.getVoucherCode())
                            .ifPresent(usage -> {
                                if (usage.getUsageCount() != null && usage.getUsageCount() > 0) {
                                    usage.setUsageCount(usage.getUsageCount() - 1);
                                    voucherUsageRepository.save(usage);
                                }
                            });
                }
            }
        }
    }

    public void revertLoyaltyOnCancel(Order savedOrder, Customer customerParam) {
        revertVoucherOnlyOnCancel(savedOrder, customerParam);

        if (customerParam.getId() != 1) {
            // Re-fetch với Pessimistic Lock để đảm bảo không bị Lost Update
            Customer customer = customerRepository.findByIdWithPessimisticLock(customerParam.getId())
                    .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy khách hàng ID: " + customerParam.getId()));

            if (savedOrder.getPointsUsed() > 0) {
                customer.setRewardPoints(customer.getRewardPoints() + savedOrder.getPointsUsed());
                savePointHistory(customer.getId(), savedOrder.getId(), savedOrder.getPointsUsed(),
                        PointConstant.TYPE_REFUND, "Hoàn điểm do hủy đơn hàng " + savedOrder.getOrderNumber());
            }
            if (savedOrder.getPointsEarned() > 0) {
                int currentPoints = customer.getRewardPoints() != null ? customer.getRewardPoints() : 0;
                int pointsToDeduct = savedOrder.getPointsEarned();
                if (currentPoints < pointsToDeduct) {
                    customer.setRewardPoints(0);
                    savePointHistory(customer.getId(), savedOrder.getId(), -currentPoints,
                            PointConstant.TYPE_REFUND, "Khấu trừ toàn bộ " + currentPoints + " điểm hiện có do hủy đơn " + savedOrder.getOrderNumber() + " (Thiếu " + (pointsToDeduct - currentPoints) + " điểm)");
                } else {
                    customer.setRewardPoints(currentPoints - pointsToDeduct);
                    savePointHistory(customer.getId(), savedOrder.getId(), -pointsToDeduct,
                            PointConstant.TYPE_REFUND, "Trừ điểm tích lũy do hủy đơn hàng " + savedOrder.getOrderNumber());
                }
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

    public void reservePoints(Order savedOrder, Customer customer) {
        if (savedOrder.getPointsUsed() > 0 && customer.getId() != 1) {
            savePointHistory(customer.getId(), savedOrder.getId(), -savedOrder.getPointsUsed(),
                    PointConstant.TYPE_RESERVED, "Tạm giữ điểm cho đơn chờ thanh toán " + savedOrder.getOrderNumber());
        }
    }
}
