package com.sapo.mock.clothing.customer.service;

import com.sapo.mock.clothing.customer.dto.request.campaigns.CareLogRequest;
import com.sapo.mock.clothing.customer.dto.response.CareLogListResponse;
import com.sapo.mock.clothing.customer.dto.response.CareLogResponse;
import com.sapo.mock.clothing.customer.dto.response.CustomerResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface CampaignService {
    // AFTER_7_DAYS
    Page<CustomerResponse> getPendingCustomersAfter7Days(Pageable pageable);

    // LONG_TIME_NO_BUY
    Page<CustomerResponse> getPendingCustomersLongTimeNoBuy(Pageable pageable);

    // RECALL_SCHEDULE
    Page<CustomerResponse> getPendingCustomersRecallSchedule(Pageable pageable);

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
}