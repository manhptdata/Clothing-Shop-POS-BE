package com.sapo.mock.clothing.customer.service.impl;

import com.sapo.mock.clothing.customer.dto.request.campaigns.CareLogRequest;
import com.sapo.mock.clothing.customer.dto.response.AiResultDto;
import com.sapo.mock.clothing.customer.dto.response.CareLogListResponse;
import com.sapo.mock.clothing.customer.dto.response.CareLogResponse;
import com.sapo.mock.clothing.customer.dto.response.CustomerResponse;
import com.sapo.mock.clothing.customer.repository.*;
import com.sapo.mock.clothing.customer.service.AiAnalysisService;
import com.sapo.mock.clothing.customer.service.CampaignService;
import com.sapo.mock.clothing.entity.CareLog;
import com.sapo.mock.clothing.entity.Customer;
import com.sapo.mock.clothing.entity.CustomerVoucher;
import com.sapo.mock.clothing.entity.User;
import com.sapo.mock.clothing.entity.CareCampaign;
import com.sapo.mock.clothing.entity.Order;
import com.sapo.mock.clothing.user.repository.UserRepository;
import com.sapo.mock.clothing.util.constant.CampaignType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.ZoneId;
import java.time.Instant;
import java.util.List;
import java.util.stream.Collectors;

import static com.sapo.mock.clothing.util.constant.CampaignType.*;

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
    @Autowired
    private CustomerVoucherRepository customerVoucherRepository;

    @Autowired
    private CareCampaignRepository careCampaignRepository;

    @Autowired
    private AiAnalysisService aiAnalysisService; // Thêm dòng này


    @Value("${campaign.config.after-days:7}")
    private int afterDays;

    @Value("${campaign.config.no-buy-days:30}")
    private int noBuyDays;

    @Override
    public Page<CustomerResponse> getPendingCustomers(CampaignType type, Pageable pageable) {
        Page<Customer> customers;
        LocalDate today = LocalDate.now();

        // Mốc tính đầu ngày hôm nay (00:00:00) để làm mốc chặn loại trừ
        Instant todayStart = today.atStartOfDay(ZoneId.systemDefault()).toInstant();

        switch (type) {
            case AFTER_7_DAYS:
                LocalDate targetDate = today.minusDays(afterDays);
                Instant end7Days = targetDate.atTime(23, 59, 59).atZone(ZoneId.systemDefault()).toInstant();
                LocalDate thirtyDaysAgoDateFor7Days = today.minusDays(noBuyDays);
                Instant thirtyDaysAgoFor7Days = thirtyDaysAgoDateFor7Days.atStartOfDay(ZoneId.systemDefault()).toInstant();
                customers = campaignRepository.findCustomersAfter7DaysBuy(end7Days, thirtyDaysAgoFor7Days, pageable);
                break;

            case LONG_TIME_NO_BUY:
                LocalDate thirtyDaysAgoDate = today.minusDays(noBuyDays);
                Instant thirtyDaysAgo = thirtyDaysAgoDate.atStartOfDay(ZoneId.systemDefault()).toInstant();
                // Truyền thêm biến todayStart vào hàm
                customers = campaignRepository.findCustomersLongTimeNoBuy(thirtyDaysAgo, todayStart, pageable);
                break;

            case RECALL_SCHEDULE:
                Instant startToday = today.atStartOfDay(ZoneId.systemDefault()).toInstant();
                Instant endToday = today.atTime(23, 59, 59).atZone(ZoneId.systemDefault()).toInstant();
                customers = campaignRepository.findCustomersRecallSchedule(startToday, endToday, pageable);
                break;

            case HAPPY_BIRTHDAY:
                int currentMonth = today.getMonthValue();
                // Truyền thêm biến todayStart vào hàm
                customers = campaignRepository.findCustomersByBirthdayMonth(currentMonth, todayStart, pageable);
                break;

            default:
                throw new IllegalArgumentException("Loại chiến dịch chăm sóc không hợp lệ!");
        }

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

 /*   @Override
    @Transactional
    public void saveCareLog(CareLogRequest request, Integer userId) {
        Customer customer = customerRepository.findById(request.getCustomerId())
                .orElseThrow(() -> new RuntimeException("Không tìm thấy khách hàng với ID: " + request.getCustomerId()));
        User staff = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy nhân viên hệ thống với ID: " + userId));
        CareCampaign campaign = careCampaignRepository.findById(request.getCampaignId())
                .orElseThrow(() -> new RuntimeException("Không tìm thấy chiến dịch với ID: " + request.getCampaignId()));
        CareLog careLog = new CareLog();
        careLog.setCustomer(customer);
        careLog.setCalledBy(staff);
        careLog.setCampaign(campaign);
        careLog.setResult(request.getResult());
        careLog.setNote(request.getNote());
        careLog.setNextRetryAt(request.getNextRetryAt());
        careLog.setCalledAt(Instant.now());

        careLogRepository.save(careLog);
    }*/
 @Override
 @Transactional
 public void saveCareLog(CareLogRequest request, Integer userId) {
     Customer customer = customerRepository.findById(request.getCustomerId())
             .orElseThrow(() -> new RuntimeException("Không tìm thấy khách hàng với ID: " + request.getCustomerId()));
     User staff = userRepository.findById(userId)
             .orElseThrow(() -> new RuntimeException("Không tìm thấy nhân viên hệ thống với ID: " + userId));
     CareCampaign campaign = careCampaignRepository.findById(request.getCampaignId())
             .orElseThrow(() -> new RuntimeException("Không tìm thấy chiến dịch với ID: " + request.getCampaignId()));

     CareLog careLog = new CareLog();
     careLog.setCustomer(customer);
     careLog.setCalledBy(staff);
     careLog.setCampaign(campaign);
     careLog.setNote(request.getNote());
     careLog.setNextRetryAt(request.getNextRetryAt());
     careLog.setCalledAt(Instant.now());

     // --- ĐOẠN XỬ LÝ AI ---
     if (request.getResult() == null || "BO_TRONG".equals(request.getResult())) {
         try {
             AiResultDto aiResult = aiAnalysisService.analyzeNote(request.getNote());
             
             careLog.setResult(aiResult.getResult());

             // RIÊNG với trạng thái Tiềm năng/Không tiềm năng thì LUÔN LUÔN để AI tự phân tích và gán
             careLog.setPotentialStatus(aiResult.getPotentialStatus()); 
             
             // Nếu AI đọc được thời gian gọi lại và nhân viên chưa tự chọn tay
             if (aiResult.getNextRetryTime() != null && request.getNextRetryAt() == null) {
                 try {
                     careLog.setNextRetryAt(Instant.parse(aiResult.getNextRetryTime()));
                 } catch (Exception e) {
                     System.err.println("Lỗi parse ngày tháng từ AI: " + e.getMessage());
                 }
             }
             
             // Dịch ngầm sang True/False để không làm hỏng giao diện cũ của Customer
             if ("TIEM_NANG".equals(aiResult.getPotentialStatus())) {
                 customer.setIsPotential(true);
             } else {
                 customer.setIsPotential(false);
             }
         } catch (Exception e) {
             System.err.println("Gặp lỗi gọi AI: " + e.getMessage());
             careLog.setResult("KHONG_XAC_DINH");
             careLog.setPotentialStatus("KHONG_XAC_DINH");
         }
     } else {
         // KHI KHÔNG DÙNG AI (TỰ CHỌN KẾT QUẢ)
         // KHÔNG MANG GHI CHÚ ĐI PHÂN TÍCH
         careLog.setResult(request.getResult());
         careLog.setPotentialStatus("KHONG_XAC_DINH");
     }
     // ---------------------

     careLogRepository.save(careLog);
     customerRepository.save(customer); // Lưu lại customer
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
    public Page<CareLogListResponse> searchCareLogs(String keyword, String result, String potentialStatus, Instant fromDate, Instant toDate, Pageable pageable) {
        String searchKeyword = (keyword != null && !keyword.trim().isEmpty()) ? keyword.trim() : null;
        String searchResult = (result != null && !result.trim().isEmpty()) ? result : null;
        String searchPotentialStatus = (potentialStatus != null && !potentialStatus.trim().isEmpty()) ? potentialStatus : null;

        Page<CareLog> logs = careLogRepository.searchCareLogs(searchKeyword, searchResult, searchPotentialStatus, fromDate, toDate, pageable);
        return logs.map(this::convertToCareLogListResponse);
    }

    // --- MAPPING HELPER METHODS ---

    private CustomerResponse convertToResponse(Customer customer) {
        CustomerResponse res = new CustomerResponse();
        res.setId(customer.getId());
        res.setFullName(customer.getFullName());
        res.setPhone(customer.getPhone());
        res.setEmail(customer.getEmail());
        res.setDateOfBirth(customer.getDateOfBirth());
        res.setGender(customer.getGender());
        res.setStatus(customer.getStatus());
        res.setAddress(customer.getAddress());
        res.setNote(customer.getNote());
        res.setCreatedAt(customer.getCreatedAt());
        res.setRewardPoints(customer.getRewardPoints());

        if (customer.getCustomerGroup() != null) {
            CustomerResponse.GroupInfo groupInfo = new CustomerResponse.GroupInfo();
            groupInfo.setId(customer.getCustomerGroup().getId());
            groupInfo.setName(customer.getCustomerGroup().getName());
            groupInfo.setCode(customer.getCustomerGroup().getCode());
            res.setCustomerGroup(groupInfo);
        }

        // Fetch voucher — có thì set, không có thì để null
        List<CustomerVoucher> vouchers =
                customerVoucherRepository.findByCustomerIdOrderByReceivedAtDesc(customer.getId());
        if (vouchers != null && !vouchers.isEmpty()) {
            List<CustomerResponse.VoucherInfo> voucherInfos = vouchers.stream().map(cv -> {
                CustomerResponse.VoucherInfo vi = new CustomerResponse.VoucherInfo();
                vi.setId(cv.getId());
                vi.setVoucherCode(cv.getVoucher().getCode());
                vi.setVoucherName(cv.getVoucher().getName());
                vi.setDiscountAmount(cv.getVoucher().getDiscountAmount());
                vi.setMinOrderValue(cv.getVoucher().getMinOrderValue());
                vi.setStatus(cv.getStatus());
                vi.setReceivedAt(cv.getReceivedAt());
                vi.setExpiredAt(cv.getExpiredAt());
                vi.setUsedAt(cv.getUsedAt());
                return vi;
            }).collect(Collectors.toList());
            res.setVouchers(voucherInfos);
        }
        // Không có voucher → giữ null

        return res;
    }

    private CareLogListResponse convertToCareLogListResponse(CareLog careLog) {
        CareLogListResponse res = new CareLogListResponse();
        res.setId(careLog.getId());
        res.setResult(careLog.getResult());
        res.setPotentialStatus(careLog.getPotentialStatus());
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

        if (careLog.getCampaign() != null) {
            CareLogListResponse.CampaignInfo info = new CareLogListResponse.CampaignInfo();
            info.setId(careLog.getCampaign().getId());
            info.setName(careLog.getCampaign().getName());
            info.setType(careLog.getCampaign().getType()); // Vì entity lưu String nên gọi trực tiếp thoải mái
            res.setCampaign(info);
        }
        return res;
    }

    // Đã giữ lại duy nhất 1 hàm map đầy đủ chuẩn theo DTO mới nhất của bạn
    private CareLogResponse convertToCareLogResponse(CareLog careLog) {
        CareLogResponse res = new CareLogResponse();
        res.setId(careLog.getId());
        res.setResult(careLog.getResult());
        res.setPotentialStatus(careLog.getPotentialStatus());
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