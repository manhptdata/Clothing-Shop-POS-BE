package com.sapo.mock.clothing.voucher.service.impl;

import com.sapo.mock.clothing.customer.dto.request.VoucherRequest;
import com.sapo.mock.clothing.customer.dto.response.VoucherResponse;
import com.sapo.mock.clothing.customer.repository.VoucherRepository;
import com.sapo.mock.clothing.entity.Voucher;
import com.sapo.mock.clothing.exception.BadRequestException;
import com.sapo.mock.clothing.exception.ResourceNotFoundException;
import com.sapo.mock.clothing.util.constant.VoucherCampaignStatusEnum;
import com.sapo.mock.clothing.voucher.service.VoucherService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class VoucherServiceImpl implements VoucherService {

    private final VoucherRepository voucherRepository;

    @Override
    public List<VoucherResponse> getAllVouchers(VoucherCampaignStatusEnum status) {
        return voucherRepository.findAll().stream()
                .filter(v -> status == null || status.equals(v.getStatus()))
                .map(VoucherResponse::fromEntity)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public VoucherResponse createVoucher(VoucherRequest request) {
        if (voucherRepository.existsByCode(request.getCode())) {
            throw new BadRequestException("Mã voucher đã tồn tại trong hệ thống, vui lòng chọn mã khác!");
        }

        Voucher voucher = new Voucher();
        voucher.setName(request.getName());
        voucher.setCode(request.getCode());
        voucher.setDiscountAmount(request.getDiscountAmount());
        voucher.setDiscountType(request.getDiscountType() != null ? request.getDiscountType() : com.sapo.mock.clothing.util.constant.VoucherDiscountType.FIXED_AMOUNT);
        voucher.setMaxDiscountAmount(request.getMaxDiscountAmount());
        voucher.setMinOrderValue(request.getMinOrderValue());
        voucher.setStatus(VoucherCampaignStatusEnum.ACTIVE);

        voucher.setStartDate(request.getStartDate());
        voucher.setEndDate(request.getEndDate());
        voucher.setTotalQuantity(request.getTotalQuantity());
        voucher.setUsedQuantity(0);
        voucher.setMaxUsagePerUser(request.getMaxUsagePerUser() != null ? request.getMaxUsagePerUser() : 1);
        voucher.setIsPublic(request.getIsPublic() != null ? request.getIsPublic() : true);
        voucher.setTargetCustomerGroupId(request.getTargetCustomerGroupId());
        voucher.setApplyType(request.getApplyType() != null ? request.getApplyType() : "ALL");

        voucher = voucherRepository.save(voucher);
        return VoucherResponse.fromEntity(voucher);
    }

    @Override
    @Transactional
    public VoucherResponse updateVoucher(Integer id, VoucherRequest request) {
        Voucher voucher = voucherRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy voucher ID: " + id));

        if (!voucher.getCode().equalsIgnoreCase(request.getCode()) && voucherRepository.existsByCode(request.getCode())) {
            throw new BadRequestException("Mã voucher đã tồn tại trong hệ thống, vui lòng chọn mã khác!");
        }

        if (request.getTotalQuantity() != null && voucher.getUsedQuantity() != null && request.getTotalQuantity() < voucher.getUsedQuantity()) {
            throw new BadRequestException("Tổng số lượng voucher (" + request.getTotalQuantity()
                    + ") không được nhỏ hơn số lượng đã sử dụng (" + voucher.getUsedQuantity() + ")");
        }

        voucher.setName(request.getName());
        voucher.setCode(request.getCode());
        voucher.setDiscountAmount(request.getDiscountAmount());
        if (request.getDiscountType() != null) {
            voucher.setDiscountType(request.getDiscountType());
        }
        voucher.setMaxDiscountAmount(request.getMaxDiscountAmount());
        voucher.setMinOrderValue(request.getMinOrderValue());

        voucher.setStartDate(request.getStartDate());
        voucher.setEndDate(request.getEndDate());
        voucher.setTotalQuantity(request.getTotalQuantity());
        if (request.getMaxUsagePerUser() != null) voucher.setMaxUsagePerUser(request.getMaxUsagePerUser());
        if (request.getIsPublic() != null) voucher.setIsPublic(request.getIsPublic());
        voucher.setTargetCustomerGroupId(request.getTargetCustomerGroupId());
        if (request.getTotalQuantity() != null) voucher.setTotalQuantity(request.getTotalQuantity());
        if (request.getMaxUsagePerUser() != null) voucher.setMaxUsagePerUser(request.getMaxUsagePerUser());
        if (request.getIsPublic() != null) voucher.setIsPublic(request.getIsPublic());
        if (request.getApplyType() != null) voucher.setApplyType(request.getApplyType());

        voucher = voucherRepository.save(voucher);
        return VoucherResponse.fromEntity(voucher);
    }

    @Override
    @Transactional
    public VoucherResponse toggleVoucherStatus(Integer id) {
        Voucher voucher = voucherRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy voucher ID: " + id));

        if (VoucherCampaignStatusEnum.ACTIVE.equals(voucher.getStatus())) {
            voucher.setStatus(VoucherCampaignStatusEnum.INACTIVE);
        } else {
            voucher.setStatus(VoucherCampaignStatusEnum.ACTIVE);
        }

        voucher = voucherRepository.save(voucher);
        return VoucherResponse.fromEntity(voucher);
    }

    @Override
    public VoucherResponse getVoucherById(Integer id) {
        Voucher voucher = voucherRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy voucher ID: " + id));
        return VoucherResponse.fromEntity(voucher);
    }
}
