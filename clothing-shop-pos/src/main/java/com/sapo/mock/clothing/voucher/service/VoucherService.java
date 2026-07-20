package com.sapo.mock.clothing.voucher.service;

import com.sapo.mock.clothing.customer.dto.request.VoucherRequest;
import com.sapo.mock.clothing.customer.dto.response.VoucherResponse;
import com.sapo.mock.clothing.util.constant.VoucherCampaignStatusEnum;
import java.util.List;

public interface VoucherService {
    List<VoucherResponse> getAllVouchers(VoucherCampaignStatusEnum status);
    VoucherResponse createVoucher(VoucherRequest request);
    VoucherResponse updateVoucher(Integer id, VoucherRequest request);
    VoucherResponse toggleVoucherStatus(Integer id);
    VoucherResponse getVoucherById(Integer id);
}
