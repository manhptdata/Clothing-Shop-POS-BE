package com.sapo.mock.clothing.customer.service.impl;

import com.sapo.mock.clothing.customer.dto.request.campaigns.CareLogRequest;
import com.sapo.mock.clothing.customer.dto.response.CareLogListResponse;
import com.sapo.mock.clothing.customer.dto.response.CareLogResponse;
import com.sapo.mock.clothing.customer.dto.response.CustomerResponse;
import com.sapo.mock.clothing.customer.repository.CampaignRepository;
import com.sapo.mock.clothing.customer.repository.CareLogRepository;
import com.sapo.mock.clothing.customer.repository.CustomerRepository;
import com.sapo.mock.clothing.customer.service.CampaignService;
import com.sapo.mock.clothing.entity.CareLog;
import com.sapo.mock.clothing.entity.Customer;
import com.sapo.mock.clothing.entity.User;
import com.sapo.mock.clothing.entity.CareCampaign; // Đảm bảo import đúng entity nếu có
import com.sapo.mock.clothing.entity.Order;        // Đảm bảo import đúng entity nếu có
import com.sapo.mock.clothing.user.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.ZoneId;
import java.time.Instant;

@Service
public class CampaignServiceImpl implements CampaignService {

    @Autowired
    private CampaignRepository campaignRepository;
    @Autowired
    private CareLogRepository careLogRepository;
    @Autowired
    private CustomerRepository customerRepository;
    @Autowired
    private UserRepository userRepository;

    @Override
    public Page<CustomerResponse> getPendingCustomersAfter7Days(Pageable pageable) {
        LocalDate targetDate = LocalDate.now().minusDays(7);
        Instant startTime = targetDate.atStartOfDay(ZoneId.systemDefault()).toInstant();
        Instant endTime = targetDate.atTime(23, 59, 59).atZone(ZoneId.systemDefault()).toInstant();

        Page<Customer> customers = campaignRepository.findCustomersAfter7DaysBuy(startTime, endTime, pageable);
        return customers.map(this::convertToResponse);
    }

    @Override
    public Page<CustomerResponse> getPendingCustomersLongTimeNoBuy(Pageable pageable) {
        LocalDate thirtyDaysAgoDate = LocalDate.now().minusDays(30);
        Instant thirtyDaysAgo = thirtyDaysAgoDate.atStartOfDay(ZoneId.systemDefault()).toInstant();

        Page<Customer> customers = campaignRepository.findCustomersLongTimeNoBuy(thirtyDaysAgo, pageable);
        return customers.map(this::convertToResponse);
    }

    @Override
    public Page<CustomerResponse> getPendingCustomersRecallSchedule(Pageable pageable) {
        LocalDate today = LocalDate.now();
        Instant startTime = today.atStartOfDay(ZoneId.systemDefault()).toInstant();
        Instant endTime = today.atTime(23, 59, 59).atZone(ZoneId.systemDefault()).toInstant();

        Page<Customer> customers = campaignRepository.findCustomersRecallSchedule(startTime, endTime, pageable);
        return customers.map(this::convertToResponse);
    }

    @Override
    public Page<CareLogListResponse> getAllCareLogs(Pageable pageable) {
        Page<CareLog> logs = careLogRepository.findAllCareLogs(pageable);
        return logs.map(this::convertToCareLogListResponse);
    }

    @Override
    public Page<CareLogResponse> getCareLogsByCustomerId(Integer customerId, Pageable pageable) {
        Page<CareLog> logs = careLogRepository.findByCustomerId(customerId, pageable);
        return logs.map(this::convertToCareLogResponse);
    }

    // Hàm xem chi tiết 1 bản ghi CareLog cụ thể theo yêu cầu
    @Override
    public CareLogResponse getCareLogDetail(Integer id) {
        CareLog careLog = careLogRepository.findDetailById(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy nhật ký chăm sóc với ID: " + id));
        return convertToCareLogResponse(careLog);
    }

    @Override
    @Transactional
    public void saveCareLog(CareLogRequest request, Integer userId) {
        Customer customer = customerRepository.findById(request.getCustomerId())
                .orElseThrow(() -> new RuntimeException("Không tìm thấy khách hàng với ID: " + request.getCustomerId()));
        User staff = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy nhân viên hệ thống với ID: " + userId));

        CareLog careLog = new CareLog();
        careLog.setCustomer(customer);
        careLog.setCalledBy(staff);
        careLog.setResult(request.getResult());
        careLog.setNote(request.getNote());
        careLog.setNextRetryAt(request.getNextRetryAt());
        careLog.setCalledAt(Instant.now());

        careLogRepository.save(careLog);
    }

    @Override
    @Transactional
    public void updateCareLog(Integer id, CareLogRequest request) {
        CareLog careLog = careLogRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy nhật ký chăm sóc với ID: " + id));

        careLog.setResult(request.getResult());
        careLog.setNote(request.getNote());
        careLog.setNextRetryAt(request.getNextRetryAt());

        careLogRepository.save(careLog);
    }

    @Override
    @Transactional
    public void deleteCareLog(Integer id) {
        CareLog careLog = careLogRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy nhật ký chăm sóc với ID: " + id));
        careLogRepository.delete(careLog);
    }

    @Override
    public Page<CareLogListResponse> searchCareLogsByPhone(String phone, String result, Instant fromDate, Instant toDate, Pageable pageable) {
        String searchPhone = (phone != null && !phone.trim().isEmpty()) ? phone.trim() : null;
        String searchResult = (result != null && !result.trim().isEmpty()) ? result.trim() : null;

        Page<CareLog> logs = careLogRepository.searchCareLogsByPhone(searchPhone, searchResult, fromDate, toDate, pageable);
        return logs.map(this::convertToCareLogListResponse);
    }

    // --- MAPPING HELPER METHODS (Đã làm sạch, không trùng lặp) ---

    private CustomerResponse convertToResponse(Customer customer) {
        CustomerResponse res = new CustomerResponse();
        res.setId(customer.getId());
        res.setFullName(customer.getFullName());
        res.setPhone(customer.getPhone());
        res.setDateOfBirth(customer.getDateOfBirth());
        res.setGender(customer.getGender());
        res.setStatus(customer.getStatus());
        res.setAddress(customer.getAddress());
        res.setNote(customer.getNote());
        res.setCreatedAt(customer.getCreatedAt());

        if (customer.getCustomerGroup() != null) {
            CustomerResponse.GroupInfo groupInfo = new CustomerResponse.GroupInfo();
            groupInfo.setId(customer.getCustomerGroup().getId());
            groupInfo.setName(customer.getCustomerGroup().getName());
            groupInfo.setCode(customer.getCustomerGroup().getCode());
            res.setCustomerGroup(groupInfo);
        }
        return res;
    }

    private CareLogListResponse convertToCareLogListResponse(CareLog careLog) {
        CareLogListResponse res = new CareLogListResponse();
        res.setId(careLog.getId());
        res.setResult(careLog.getResult());
        res.setCalledAt(careLog.getCalledAt());
        res.setCreatedAt(careLog.getCreatedAt());

        if (careLog.getCustomer() != null) {
            CareLogListResponse.CustomerInfo info = new CareLogListResponse.CustomerInfo();
            info.setId(careLog.getCustomer().getId());
            info.setFullName(careLog.getCustomer().getFullName());
            info.setPhone(careLog.getCustomer().getPhone());
            res.setCustomer(info);
        }

        if (careLog.getCalledBy() != null) {
            CareLogListResponse.UserInfo info = new CareLogListResponse.UserInfo();
            info.setId(careLog.getCalledBy().getId());
            info.setUsername(careLog.getCalledBy().getUsername());
            info.setFullName(careLog.getCalledBy().getFullName());
            res.setCalledBy(info);
        }
        return res;
    }

    // Đã giữ lại duy nhất 1 hàm map đầy đủ chuẩn theo DTO mới nhất của bạn
    private CareLogResponse convertToCareLogResponse(CareLog careLog) {
        CareLogResponse res = new CareLogResponse();
        res.setId(careLog.getId());
        res.setResult(careLog.getResult());
        res.setNote(careLog.getNote());
        res.setScheduledAt(careLog.getScheduledAt());
        res.setCalledAt(careLog.getCalledAt());
        res.setNextRetryAt(careLog.getNextRetryAt());
        res.setCreatedAt(careLog.getCreatedAt());

        if (careLog.getCustomer() != null) {
            CareLogResponse.CustomerInfo info = new CareLogResponse.CustomerInfo();
            info.setId(careLog.getCustomer().getId());
            info.setFullName(careLog.getCustomer().getFullName());
            info.setPhone(careLog.getCustomer().getPhone());
            res.setCustomer(info);
        }

        if (careLog.getCampaign() != null) {
            CareLogResponse.CampaignInfo info = new CareLogResponse.CampaignInfo();
            info.setId(careLog.getCampaign().getId());
            info.setName(careLog.getCampaign().getName());
            res.setCampaign(info);
        }

        if (careLog.getCalledBy() != null) {
            CareLogResponse.UserInfo info = new CareLogResponse.UserInfo();
            info.setId(careLog.getCalledBy().getId());
            info.setUsername(careLog.getCalledBy().getUsername());
            info.setFullName(careLog.getCalledBy().getFullName());
            res.setCalledBy(info);
        }

        if (careLog.getOrder() != null) {
            CareLogResponse.OrderInfo info = new CareLogResponse.OrderInfo();
            info.setId(careLog.getOrder().getId());
            info.setOrderNumber(careLog.getOrder().getOrderNumber());
            res.setOrder(info);
        }

        return res;
    }
}