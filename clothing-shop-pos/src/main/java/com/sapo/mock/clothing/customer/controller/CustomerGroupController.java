package com.sapo.mock.clothing.customer.controller;

import com.sapo.mock.clothing.common.dto.response.RestResponse;
import com.sapo.mock.clothing.customer.dto.event.OrderCompletedEvent;
import com.sapo.mock.clothing.customer.dto.request.groupcustomer.CustomerGroupRequest;
import com.sapo.mock.clothing.customer.dto.request.groupcustomer.MockOrderRequest;
import com.sapo.mock.clothing.customer.dto.request.VoucherRequest;
import com.sapo.mock.clothing.entity.Voucher;
import com.sapo.mock.clothing.customer.dto.response.CustomerGroupResponse;
import com.sapo.mock.clothing.customer.dto.response.CustomerResponse;
import com.sapo.mock.clothing.customer.dto.response.VoucherResponse;
import com.sapo.mock.clothing.customer.repository.VoucherRepository;
import com.sapo.mock.clothing.customer.service.CustomerGroupService;
import com.sapo.mock.clothing.customer.service.CustomerService;
import com.sapo.mock.clothing.customer.service.scheduler.BirthdayVoucherScheduler;
import com.sapo.mock.clothing.util.constant.VoucherCampaignStatusEnum;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/crm/customer-groups")
@CrossOrigin(origins = "*")
@PreAuthorize("hasAnyAuthority('ROLE_ADMIN', 'MANAGE_CUSTOMER')")
public class CustomerGroupController {

    @Autowired
    private CustomerGroupService groupService;

    @Autowired
    private CustomerService customerService;

    @Autowired
    private ApplicationEventPublisher eventPublisher;

    @Autowired
    private BirthdayVoucherScheduler birthdayVoucherScheduler;

    @Autowired
    private VoucherRepository voucherRepository;



    /**
     * API: Lấy TẤT CẢ các nhóm khách hàng hiện có (Không phân trang)
     * Endpoint: GET /api/crm/customer-groups/all
     */
    @GetMapping("")
    public ResponseEntity<RestResponse<Page<CustomerGroupResponse>>> getGroupsWithPage(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {

        Pageable pageable = PageRequest.of(page, size, Sort.by("id").ascending());

        Page<CustomerGroupResponse> result = groupService.getGroupsWithPage(pageable);

        RestResponse<Page<CustomerGroupResponse>> response = new RestResponse<>();
        response.setStatusCode(HttpStatus.OK.value());
        response.setError(null);
        response.setMessage("Lấy danh sách phân trang nhóm khách hàng thành công");
        response.setData(result);

        return ResponseEntity.ok(response);
    }

    /**
     * API 1: Xem danh sách phân trang và tìm kiếm nhóm khách hàng
     * Endpoint: GET /api/crm/customer-groups/search?keyword=Vùng A
     */
    @GetMapping("/search")
    public ResponseEntity<RestResponse<Page<CustomerGroupResponse>>> searchGroups(
            @RequestParam(required = false, defaultValue = "") String keyword,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {

        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        Page<CustomerGroupResponse> result = groupService.searchGroups(keyword, pageable);

        RestResponse<Page<CustomerGroupResponse>> response = new RestResponse<>();
        response.setStatusCode(HttpStatus.OK.value());
        response.setError(null);
        response.setMessage("Tra cứu danh sách nhóm khách hàng thành công");
        response.setData(result);

        return ResponseEntity.ok(response);
    }

    /**
     * API: Xem chi tiết thông tin cấu hình một nhóm khách hàng
     * Endpoint: GET /api/crm/customer-groups/{id}
     */
    @GetMapping("/{id}")
    public ResponseEntity<RestResponse<CustomerGroupResponse>> getGroupDetail(@PathVariable Integer id) {

        // Gọi service lấy dữ liệu chi tiết nhóm
        CustomerGroupResponse result = groupService.getGroupById(id);

        RestResponse<CustomerGroupResponse> response = new RestResponse<>(
                HttpStatus.OK.value(),
                null,
                "Lấy chi tiết nhóm khách hàng thành công",
                result
        );

        return ResponseEntity.ok(response);
    }





    /**
     * API: Lấy danh sách + Tìm kiếm thành viên trong từng nhóm cụ thể
     * URL TEST POSTMAN:
     * - Chỉ lọc theo nhóm: GET http://localhost:8080/api/v1/crm/customers/1/members?page=0&size=10
     * - Tìm kiếm trong nhóm: GET http://localhost:8080/api/v1/crm/customers/1/members?keyword=Đinh&page=0&size=10
     */
    @GetMapping("/{groupId}/members")
    public ResponseEntity<RestResponse<Page<CustomerResponse>>> getGroupMembers(
            @PathVariable Integer groupId,
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {

        // Sắp xếp danh sách khách hàng mới gia nhập hạng lên đầu tiên
        Pageable pageable = PageRequest.of(page, size, Sort.by("id").descending());

        // Truyền thêm param keyword vào service
        Page<CustomerResponse> result = customerService.getCustomersByGroupId(groupId, keyword, pageable);

        RestResponse<Page<CustomerResponse>> response = new RestResponse<>();
        response.setStatusCode(HttpStatus.OK.value());
        response.setError(null);
        response.setMessage("Lấy danh sách và tìm kiếm thành viên của nhóm thành công");
        response.setData(result);

        return ResponseEntity.ok(response);
    }




    /**
     * API Giả lập đơn hàng hoàn thành phục vụ test luồng tự động CRM
     * Endpoint: POST /api/crm/complete
     */
    @PostMapping("/complete")
    public ResponseEntity<String> mockCompleteOrder(@RequestBody MockOrderRequest request) {

        // Phát sự kiện ngầm vào hệ thống Spring Context
        eventPublisher.publishEvent(new OrderCompletedEvent(
                request.getCustomerId(),
                request.getOrderAmount()
        ));

        return ResponseEntity.ok("Giả lập hoàn thành đơn hàng thành công! Đã phát sự kiện nhảy hạng tự động.");
    }




    @PostMapping
    public ResponseEntity<RestResponse<CustomerGroupResponse>> createGroup(@Valid @RequestBody CustomerGroupRequest request) {
        CustomerGroupResponse result = groupService.createGroup(request);
        return ResponseEntity.ok(new RestResponse<>(HttpStatus.CREATED.value(), null, "Tạo nhóm thành công", result));
    }

    @PutMapping("/{id}")
    public ResponseEntity<RestResponse<CustomerGroupResponse>> updateGroup(
            @PathVariable Integer id,
            @Valid @RequestBody CustomerGroupRequest request) {
        CustomerGroupResponse result = groupService.updateGroup(id, request);
        return ResponseEntity.ok(new RestResponse<>(HttpStatus.OK.value(), null, "Cập nhật nhóm thành công", result));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<RestResponse<Object>> deleteGroup(@PathVariable Integer id) {
        groupService.deleteGroup(id);
        return ResponseEntity.ok(new RestResponse<>(HttpStatus.OK.value(), null, "Xóa nhóm thành công", null));
    }

    @PostMapping("/sync-ranks")
    public ResponseEntity<RestResponse<Object>> syncAllCustomerRanks() {
        groupService.syncAllCustomerRanks();
        return ResponseEntity.ok(new RestResponse<>(HttpStatus.OK.value(), null, "Đồng bộ phân hạng tất cả khách hàng thành công", null));
    }

    /**
     * API kích hoạt thủ công luồng gửi voucher sinh nhật (dùng để test, không cần chờ 12h đêm)
     * POST /api/crm/customer-groups/trigger-birthday-vouchers
     */
    @PostMapping("/trigger-birthday-vouchers")
    public ResponseEntity<RestResponse<Object>> triggerBirthdayVouchers() {
        birthdayVoucherScheduler.runNow();
        return ResponseEntity.ok(new RestResponse<>(HttpStatus.OK.value(), null, "Kích hoạt gửi Voucher sinh nhật thành công! Kiểm tra Console để xem log chi tiết.", null));
    }

    @GetMapping("/vouchers")
    public ResponseEntity<RestResponse<java.util.List<VoucherResponse>>> getAllVouchers(@RequestParam(required = false) VoucherCampaignStatusEnum status) {
        java.util.List<VoucherResponse> vouchers = voucherRepository.findAll().stream()
                .filter(v -> status == null || status.equals(v.getStatus()))
                .map(v -> new VoucherResponse(v.getId(), v.getName(), v.getCode(), v.getDiscountAmount(), v.getMinOrderValue(), v.getStatus()))
                .collect(java.util.stream.Collectors.toList());
        return ResponseEntity.ok(new RestResponse<>(HttpStatus.OK.value(), null, "Lấy danh sách voucher thành công", vouchers));
    }

    @PostMapping("/vouchers")
    public ResponseEntity<RestResponse<VoucherResponse>> createVoucher(@Valid @RequestBody VoucherRequest request) {
        if (voucherRepository.existsByCode(request.getCode())) {
            return ResponseEntity.badRequest().body(new RestResponse<>(HttpStatus.BAD_REQUEST.value(), null, "Mã voucher đã tồn tại trong hệ thống, vui lòng chọn mã khác!", null));
        }

        Voucher voucher = new Voucher();
        voucher.setName(request.getName());
        voucher.setCode(request.getCode());
        voucher.setDiscountAmount(request.getDiscountAmount());
        voucher.setMinOrderValue(request.getMinOrderValue());
        voucher.setStatus(VoucherCampaignStatusEnum.ACTIVE);
        
        voucher = voucherRepository.save(voucher);
        
        VoucherResponse response = new VoucherResponse(voucher.getId(), voucher.getName(), voucher.getCode(), voucher.getDiscountAmount(), voucher.getMinOrderValue(), voucher.getStatus());
        return ResponseEntity.ok(new RestResponse<>(HttpStatus.OK.value(), null, "Tạo voucher thành công", response));
    }
    @PutMapping("/vouchers/{id}")
    public ResponseEntity<RestResponse<VoucherResponse>> updateVoucher(@PathVariable Integer id, @Valid @RequestBody VoucherRequest request) {
        Voucher voucher = voucherRepository.findById(id).orElse(null);
        if (voucher == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(new RestResponse<>(HttpStatus.NOT_FOUND.value(), null, "Không tìm thấy voucher này", null));
        }

        if (!voucher.getCode().equals(request.getCode()) && voucherRepository.existsByCode(request.getCode())) {
            return ResponseEntity.badRequest().body(new RestResponse<>(HttpStatus.BAD_REQUEST.value(), null, "Mã voucher đã tồn tại trong hệ thống, vui lòng chọn mã khác!", null));
        }

        voucher.setName(request.getName());
        voucher.setCode(request.getCode());
        voucher.setDiscountAmount(request.getDiscountAmount());
        voucher.setMinOrderValue(request.getMinOrderValue());
        
        voucher = voucherRepository.save(voucher);
        
        VoucherResponse response = new VoucherResponse(voucher.getId(), voucher.getName(), voucher.getCode(), voucher.getDiscountAmount(), voucher.getMinOrderValue(), voucher.getStatus());
        return ResponseEntity.ok(new RestResponse<>(HttpStatus.OK.value(), null, "Cập nhật voucher thành công", response));
    }

    @PatchMapping("/vouchers/{id}/toggle")
    public ResponseEntity<RestResponse<VoucherResponse>> toggleVoucherStatus(@PathVariable Integer id) {
        Voucher voucher = voucherRepository.findById(id).orElse(null);
        if (voucher == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(new RestResponse<>(HttpStatus.NOT_FOUND.value(), null, "Không tìm thấy voucher này", null));
        }

        if (VoucherCampaignStatusEnum.ACTIVE.equals(voucher.getStatus())) {
            voucher.setStatus(VoucherCampaignStatusEnum.INACTIVE);
        } else {
            voucher.setStatus(VoucherCampaignStatusEnum.ACTIVE);
        }
        voucher = voucherRepository.save(voucher);

        VoucherResponse response = new VoucherResponse(voucher.getId(), voucher.getName(), voucher.getCode(), voucher.getDiscountAmount(), voucher.getMinOrderValue(), voucher.getStatus());
        String msg = VoucherCampaignStatusEnum.ACTIVE.equals(voucher.getStatus()) ? "Đã bật phát hành voucher" : "Đã tạm dừng phát hành voucher";
        return ResponseEntity.ok(new RestResponse<>(HttpStatus.OK.value(), null, msg, response));
    }

    @GetMapping("/vouchers/history")
    public ResponseEntity<RestResponse<Page<com.sapo.mock.clothing.customer.dto.response.CustomerVoucherHistoryResponse>>> getVoucherHistory(
            @RequestParam(required = false, defaultValue = "") String keyword,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {

        Pageable pageable = PageRequest.of(page, size, Sort.by("receivedAt").descending());
        Page<com.sapo.mock.clothing.customer.dto.response.CustomerVoucherHistoryResponse> result = groupService.getVoucherHistory(keyword, pageable);

        RestResponse<Page<com.sapo.mock.clothing.customer.dto.response.CustomerVoucherHistoryResponse>> response = new RestResponse<>();
        response.setStatusCode(HttpStatus.OK.value());
        response.setError(null);
        response.setMessage("Tra cứu lịch sử phát voucher thành công");
        response.setData(result);

        return ResponseEntity.ok(response);
    }

}