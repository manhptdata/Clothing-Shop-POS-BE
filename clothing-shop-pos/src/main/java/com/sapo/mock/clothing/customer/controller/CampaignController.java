package com.sapo.mock.clothing.customer.controller;


import com.sapo.mock.clothing.common.dto.response.RestResponse;
import com.sapo.mock.clothing.customer.dto.request.campaigns.CareLogRequest;
import com.sapo.mock.clothing.customer.dto.response.CareLogListResponse;
import com.sapo.mock.clothing.customer.dto.response.CareLogResponse;
import com.sapo.mock.clothing.customer.dto.response.CustomerResponse;
import com.sapo.mock.clothing.customer.service.CampaignService;
import com.sapo.mock.clothing.util.constant.CampaignType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import jakarta.validation.Valid;
import java.time.Instant;
import com.sapo.mock.clothing.util.SecurityUtil;

@RestController
@RequestMapping("/api/crm/campaigns")
@CrossOrigin(origins = "*")
public class CampaignController {

    @Autowired
    private CampaignService campaignService;

    @Autowired
    private com.sapo.mock.clothing.customer.service.AiAnalysisService aiAnalysisService;

    /**
     * API: Gợi ý kịch bản gọi điện và SMS bằng AI
     * Endpoint: GET /api/crm/campaigns/{campaignId}/customers/{customerId}/ai-suggest
     */
    @GetMapping("/{campaignId}/customers/{customerId}/ai-suggest")
    public ResponseEntity<RestResponse<com.sapo.mock.clothing.customer.dto.response.AiSuggestionResponseDto>> suggestAiScript(
            @PathVariable Integer campaignId,
            @PathVariable Integer customerId) {
        
        com.sapo.mock.clothing.customer.dto.response.AiSuggestionResponseDto result = aiAnalysisService.suggestScriptAndSms(customerId, campaignId);
        
        RestResponse<com.sapo.mock.clothing.customer.dto.response.AiSuggestionResponseDto> response = new RestResponse<>(
                HttpStatus.OK.value(), null, "Gợi ý kịch bản bằng AI thành công", result
        );
        return ResponseEntity.ok(response);
    }

    /**
     * API dùng chung quét danh sách khách hàng theo từng loại chiến dịch
     */
    @GetMapping("/pending-customers")
    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN', 'VIEW_CAMPAIGN', 'MANAGE_CAMPAIGN')")
    public ResponseEntity<RestResponse<Page<CustomerResponse>>> getPendingCustomers(
            @RequestParam(name = "type") CampaignType type,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {

        Pageable pageable = PageRequest.of(page, size);

        // Gọi hàm gộp duy nhất
        Page<CustomerResponse> result = campaignService.getPendingCustomers(type, pageable);

        // Sinh message động dựa theo loại chiến dịch
        String message = "Lấy danh sách khách hàng thành công";
        if (type == CampaignType.AFTER_7_DAYS) {
            message = "Lấy danh sách khách hàng sau mua thành công";
        } else if (type == CampaignType.LONG_TIME_NO_BUY) {
            message = "Lấy danh sách khách hàng quá hạn chưa phát sinh đơn mới thành công";
        } else if (type == CampaignType.RECALL_SCHEDULE) {
            message = "Lấy danh sách khách hàng hẹn gọi lại ngày hôm nay thành công";
        }else if (type == CampaignType.HAPPY_BIRTHDAY) {
            message = "Lấy danh sách khách hàng có sinh nhật trong tháng hiện tại thành công";
        }

        RestResponse<Page<CustomerResponse>> response = new RestResponse<>(
                HttpStatus.OK.value(), null, message, result
        );
        return ResponseEntity.ok(response);
    }



    /**
     * API: Xem TOÀN BỘ danh sách lịch sử chăm sóc khách hàng của cửa hàng
     * Endpoint: GET /api/crm/campaigns/care-logs
     */
    @GetMapping("/care-logs")
    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN', 'VIEW_CUSTOMER', 'MANAGE_CUSTOMER')")
    public ResponseEntity<RestResponse<Page<CareLogListResponse>>> getAllCareLogs(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {

        Pageable pageable = PageRequest.of(page, size, org.springframework.data.domain.Sort.by("id").descending());
        Page<CareLogListResponse> result = campaignService.getAllCareLogs(pageable);

        RestResponse<Page<CareLogListResponse>> response = new RestResponse<>(
                HttpStatus.OK.value(), null, "Lấy danh sách nhật ký chăm sóc thành công", result
        );
        return ResponseEntity.ok(response);
    }

    /**
     * API: Xem danh sách lịch sử chăm sóc của RIÊNG 1 khách hàng (Dùng cho màn hình chi tiết khách)
     * Endpoint: GET /api/crm/campaigns/customers/{customerId}/care-logs
     */
    @GetMapping("/customers/{customerId}/care-logs")
    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN', 'VIEW_CUSTOMER', 'MANAGE_CUSTOMER')")
    public ResponseEntity<RestResponse<Page<CareLogResponse>>> getCustomerCareLogs(
            @PathVariable Integer customerId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {

        Pageable pageable = PageRequest.of(page, size, org.springframework.data.domain.Sort.by("id").descending());
        Page<CareLogResponse> result = campaignService.getCareLogsByCustomerId(customerId, pageable);

        RestResponse<Page<CareLogResponse>> response = new RestResponse<>(
                HttpStatus.OK.value(), null, "Lấy lịch sử chăm sóc của khách hàng thành công", result
        );
        return ResponseEntity.ok(response);
    }

    /**
     * API: Ghi nhận lịch sử sau mỗi lần liên hệ khách hàng (Tạo mới nhật ký)
     * Endpoint: POST /api/crm/campaigns/care-logs
     */
    @PostMapping("/care-logs")
    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN', 'MANAGE_CUSTOMER')")
    public ResponseEntity<RestResponse<Void>> createCareLog(
            @Valid @RequestBody CareLogRequest request) {

        // Lấy ID nhân viên từ Context của Spring Security
        Integer currentUserId = SecurityUtil.getCurrentUserId();
        if (currentUserId == null) {
            currentUserId = 1; // Fallback an toàn nếu test API không có token
        }

        campaignService.saveCareLog(request, currentUserId);

        RestResponse<Void> response = new RestResponse<>(
                HttpStatus.CREATED.value(),
                null,
                "Ghi nhận lịch sử chăm sóc khách hàng thành công",
                null
        );

        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }

    /**
     * API: Cập nhật nội dung chăm sóc (Chỉnh sửa nhật ký cũ)
     * Endpoint: PUT /api/crm/campaigns/care-logs/{id}
     */
    @PutMapping("/care-logs/{id}")
    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN', 'MANAGE_CUSTOMER')")
    public ResponseEntity<RestResponse<Void>> updateCareLog(
            @PathVariable Integer id,
            @Valid @RequestBody CareLogRequest request) {

        campaignService.updateCareLog(id, request);

        RestResponse<Void> response = new RestResponse<>(
                HttpStatus.OK.value(),
                null,
                "Cập nhật nhật ký chăm sóc khách hàng thành công",
                null
        );

        return ResponseEntity.ok(response);
    }


    /**
     * API: Xóa lịch sử chăm sóc (Xóa nhật ký cũ)
     * Endpoint: DELETE /api/crm/campaigns/care-logs/{id}
     */
    @DeleteMapping("/care-logs/{id}")
    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN', 'MANAGE_CUSTOMER')")
    public ResponseEntity<RestResponse<Void>> deleteCareLog(@PathVariable Integer id) {

        campaignService.deleteCareLog(id);

        RestResponse<Void> response = new RestResponse<>(
                HttpStatus.OK.value(),
                null,
                "Xóa nhật ký chăm sóc khách hàng thành công",
                null
        );

        return ResponseEntity.ok(response);
    }

    /**
     * API: Tìm kiếm và lọc nâng cao lịch sử chăm sóc khách hàng
     * Endpoint: GET /api/crm/campaigns/care-logs/search
     */
    @GetMapping("/care-logs/search")
    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN', 'VIEW_CUSTOMER', 'MANAGE_CUSTOMER')")
    public ResponseEntity<RestResponse<Page<CareLogListResponse>>> searchCareLogs(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String result,
            @RequestParam(required = false) String potentialStatus,
            @RequestParam(required = false) Instant fromDate,
            @RequestParam(required = false) Instant toDate,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "calledAt"));
        Page<CareLogListResponse> searchResult = campaignService.searchCareLogs(keyword, result, potentialStatus, fromDate, toDate, pageable);

        RestResponse<Page<CareLogListResponse>> response = new RestResponse<>(
                HttpStatus.OK.value(),
                null,
                "Tìm kiếm lịch sử chăm sóc thành công",
                searchResult
        );
        return ResponseEntity.ok(response);
    }



    /**
     * API: Xem CHI TIẾT thông tin một bản ghi lịch sử chăm sóc
     * Endpoint: GET /api/crm/campaigns/care-logs/{id}
     */
    @GetMapping("/care-logs/{id}")
    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN', 'VIEW_CUSTOMER', 'MANAGE_CUSTOMER')")
    public ResponseEntity<RestResponse<CareLogResponse>> getCareLogDetail(@PathVariable Integer id) {

        CareLogResponse result = campaignService.getCareLogDetail(id);

        RestResponse<CareLogResponse> response = new RestResponse<>(
                HttpStatus.OK.value(),
                null,
                "Lấy chi tiết nhật ký chăm sóc thành công",
                result
        );

        return ResponseEntity.ok(response);
    }
}