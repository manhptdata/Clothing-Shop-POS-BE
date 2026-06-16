package com.sapo.mock.clothing.customer.controller;


import com.sapo.mock.clothing.common.dto.response.RestResponse;
import com.sapo.mock.clothing.customer.dto.request.campaigns.CareLogRequest;
import com.sapo.mock.clothing.customer.dto.response.CareLogListResponse;
import com.sapo.mock.clothing.customer.dto.response.CareLogResponse;
import com.sapo.mock.clothing.customer.dto.response.CustomerResponse;
import com.sapo.mock.clothing.customer.service.CampaignService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/crm/campaigns")
@CrossOrigin(origins = "*")
public class CampaignController {

    @Autowired
    private CampaignService campaignService;

    /**
     * API dùng chung quét danh sách khách hàng theo từng loại chiến dịch
     */
    @GetMapping("/pending-customers")
    public ResponseEntity<RestResponse<Page<CustomerResponse>>> getPendingCustomers(
            @RequestParam(name = "type") String type,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {

        Pageable pageable = PageRequest.of(page, size);
        Page<CustomerResponse> result;

        // Nhánh 1: Sau mua 7 ngày
        if ("AFTER_7_DAYS".equalsIgnoreCase(type)) {
            result = campaignService.getPendingCustomersAfter7Days(pageable);
            RestResponse<Page<CustomerResponse>> response = new RestResponse<>(
                    HttpStatus.OK.value(), null, "Lấy danh sách khách hàng sau mua 7 ngày thành công", result
            );
            return ResponseEntity.ok(response);
        }

        // Nhánh 2: Quá 30 ngày chưa mua
        else if ("LONG_TIME_NO_BUY".equalsIgnoreCase(type)) {
            result = campaignService.getPendingCustomersLongTimeNoBuy(pageable);
            RestResponse<Page<CustomerResponse>> response = new RestResponse<>(
                    HttpStatus.OK.value(), null, "Lấy danh sách khách hàng quá 30 ngày chưa phát sinh đơn mới thành công", result
            );
            return ResponseEntity.ok(response);
        }

        // Lịch hẹn gọi lại ngày hôm nay
        else if ("RECALL_SCHEDULE".equalsIgnoreCase(type)) {
            result = campaignService.getPendingCustomersRecallSchedule(pageable);
            RestResponse<Page<CustomerResponse>> response = new RestResponse<>(
                    HttpStatus.OK.value(), null, "Lấy danh sách khách hàng hẹn gọi lại ngày hôm nay thành công", result
            );
            return ResponseEntity.ok(response);
        }

        // Trường hợp truyền bậy bạ bạ lên URL
        else {
            throw new IllegalArgumentException("Loại chiến dịch chăm sóc không hợp lệ!");
        }
    }



    /**
     * API: Xem TOÀN BỘ danh sách lịch sử chăm sóc khách hàng của cửa hàng
     * Endpoint: GET /api/v1/crm/campaigns/care-logs
     */
    @GetMapping("/care-logs")
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
     * Endpoint: GET /api/v1/crm/campaigns/customers/{customerId}/care-logs
     */
    @GetMapping("/customers/{customerId}/care-logs")
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
     * Endpoint: POST /api/v1/crm/campaigns/care-logs
     */
    @PostMapping("/care-logs")
    public ResponseEntity<RestResponse<Void>> createCareLog(
            @jakarta.validation.Valid @RequestBody CareLogRequest request) {

        // Tạm thời gán cứng ID nhân viên bằng 1 do đang chạy Mock dự án chưa làm Security hoàn thiện.
        // Sau này khi tích hợp Token, bạn chỉ cần lấy ID từ Context của Spring Security ra là xong.
        Integer currentUserId = 1;

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
     * Endpoint: PUT /api/v1/crm/campaigns/care-logs/{id}
     */
    @PutMapping("/care-logs/{id}")
    public ResponseEntity<RestResponse<Void>> updateCareLog(
            @PathVariable Integer id,
            @jakarta.validation.Valid @RequestBody CareLogRequest request) {

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
     * Endpoint: DELETE /api/v1/crm/campaigns/care-logs/{id}
     */
    @DeleteMapping("/care-logs/{id}")
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
}