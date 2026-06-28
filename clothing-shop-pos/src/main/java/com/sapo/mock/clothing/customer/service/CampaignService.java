package com.sapo.mock.clothing.customer.service;

import com.sapo.mock.clothing.customer.dto.request.campaigns.CareLogRequest;
import com.sapo.mock.clothing.customer.dto.response.CareLogListResponse;
import com.sapo.mock.clothing.customer.dto.response.CareLogResponse;
import com.sapo.mock.clothing.customer.dto.response.CustomerResponse;
import com.sapo.mock.clothing.util.constant.CampaignType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.time.Instant;

public interface CampaignService {
    // Hàm quét danh sách khách hàng theo từng loại chiến dịch
    Page<CustomerResponse> getPendingCustomers(CampaignType type, Pageable pageable);

    // Hàm xem toàn bộ danh sách lịch sử chăm sóc hệ thống
    Page<CareLogListResponse> getAllCareLogs(Pageable pageable);
    // Hàm xem lịch sử chăm sóc của riêng 1 khách hàng cụ thể
    Page<CareLogResponse> getCareLogsByCustomerId(Integer customerId, Pageable pageable);

    // save hisqtory care log
    void saveCareLog(CareLogRequest request, Integer userId);

    // update care log
    void updateCareLog(Integer id, CareLogRequest request);

    // Delete care log
    void deleteCareLog(Integer id);

    // Hàm tìm kiếm lịch sử chăm sóc
    Page<CareLogListResponse> searchCareLogs(String keyword, String result, String potentialStatus, Instant fromDate, Instant toDate, Pageable pageable);
    // Hàm lấy chi tiết 1 bản ghi CareLog
    CareLogResponse getCareLogDetail(Integer id);

}