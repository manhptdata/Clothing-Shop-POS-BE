package com.sapo.mock.clothing.customer.controller;

import com.sapo.mock.clothing.common.dto.response.RestResponse;
import com.sapo.mock.clothing.customer.dto.event.OrderCompletedEvent;
import com.sapo.mock.clothing.customer.dto.request.groupcustomer.AssignGroupRequest;
import com.sapo.mock.clothing.customer.dto.request.groupcustomer.CustomerGroupRequest;
import com.sapo.mock.clothing.customer.dto.request.groupcustomer.CustomerRequest;
import com.sapo.mock.clothing.customer.dto.request.groupcustomer.MockOrderRequest;
import com.sapo.mock.clothing.customer.dto.response.CustomerGroupResponse;
import com.sapo.mock.clothing.customer.dto.response.CustomerResponse;
import com.sapo.mock.clothing.customer.service.CustomerGroupService;
import com.sapo.mock.clothing.customer.service.CustomerService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/crm/customer-groups")
@CrossOrigin(origins = "*")
public class CustomerGroupController {

    @Autowired
    private CustomerGroupService groupService;

    @Autowired
    private CustomerService customerService;

    @Autowired
    private ApplicationEventPublisher eventPublisher;



    /**
     * API: Lấy TẤT CẢ các nhóm khách hàng hiện có (Không phân trang)
     * Endpoint: GET /api/v1/crm/customer-groups/all
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
     * Endpoint: GET /api/v1/crm/customer-groups/search?keyword=Vùng A
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
     * Endpoint: GET /api/v1/crm/customer-groups/{id}
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
     * API: Tạo mới nhóm khách hàng (Cấu hình hạng & code cố định)
     * Endpoint: POST /api/v1/crm/customer-groups
     */
    @PostMapping("")
    public ResponseEntity<RestResponse<CustomerGroupResponse>> createGroup(
            @jakarta.validation.Valid @RequestBody CustomerGroupRequest request) {

        CustomerGroupResponse result = groupService.createGroup(request);

        RestResponse<CustomerGroupResponse> response = new RestResponse<>(
                HttpStatus.CREATED.value(),
                null,
                "Tạo mới nhóm khách hàng thành công",
                result
        );

        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }

    /**
     * API: Gán khách hàng vào nhóm qua đường dẫn của Customer Group
     * Endpoint: PUT /api/v1/crm/customer-groups/assign/{customerId}
     */
    // PUT /api/v1/crm/customer-groups/{customerId}/assign
    @PutMapping("/{customerId}/assign")
    public ResponseEntity<RestResponse<CustomerResponse>> assignGroup(
            @PathVariable Integer customerId,
            @Valid @RequestBody CustomerRequest request) {

        CustomerResponse result = groupService.assignGroupToCustomer(customerId, request);

        RestResponse<CustomerResponse> response = new RestResponse<>();
        response.setStatusCode(HttpStatus.OK.value());
        response.setError(null);
        response.setMessage("Gán khách hàng vào nhóm thành công");
        response.setData(result);

        return ResponseEntity.ok(response);
    }

    /**
     * API: Chỉ gán / đổi / rút nhóm nhanh — không cần gửi lại toàn bộ thông tin khách hàng
     * Endpoint: PATCH /api/v1/crm/customer-groups/{customerId}/assign-group
     * Body: { "customerGroupId": 5 }  hoặc { "customerGroupId": null } để rút nhóm
     */
    @PatchMapping("/{customerId}/assign-group")
    public ResponseEntity<RestResponse<CustomerResponse>> assignOnlyGroup(
            @PathVariable Integer customerId,
            @RequestBody AssignGroupRequest request) {

        CustomerResponse result = groupService.assignOnlyGroup(customerId, request.getCustomerGroupId());

        RestResponse<CustomerResponse> response = new RestResponse<>();
        response.setStatusCode(HttpStatus.OK.value());
        response.setError(null);
        response.setMessage("Gán nhóm cho khách hàng thành công");
        response.setData(result);

        return ResponseEntity.ok(response);
    }

    /**
     * API: Xem danh sách thành viên (khách hàng) của một nhóm cụ thể
     * Endpoint: GET /api/v1/crm/customer-groups/{groupId}/members?page=0&size=10
     */
    @GetMapping("/{groupId}/members")
    public ResponseEntity<RestResponse<Page<CustomerResponse>>> getGroupMembers(
            @PathVariable Integer groupId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {

        // Sắp xếp danh sách khách hàng mới gia nhập hạng lên đầu tiên
        Pageable pageable = PageRequest.of(page, size, Sort.by("id").descending());

        // Gọi sang CustomerService đã được xử lý ở tầng trên để lấy danh sách thành viên
        Page<CustomerResponse> result = customerService.getCustomersByGroupId(groupId, pageable);

        RestResponse<Page<CustomerResponse>> response = new RestResponse<>();
        response.setStatusCode(HttpStatus.OK.value());
        response.setError(null);
        response.setMessage("Lấy danh sách thành viên của nhóm thành công");
        response.setData(result);

        return ResponseEntity.ok(response);
    }


    /**
     * API: Cập nhật thông tin nhóm khách hàng (Sửa cấu hình chi tiêu phân hạng)
     * Endpoint: PUT /api/v1/crm/customer-groups/{id}
     */
    @PutMapping("/{id}")
    public ResponseEntity<RestResponse<Void>> updateCustomerGroup(
            @PathVariable Integer id,
            @jakarta.validation.Valid @RequestBody CustomerGroupRequest request) {

        groupService.updateCustomerGroup(id, request);

        RestResponse<Void> response = new RestResponse<>(
                HttpStatus.OK.value(),
                null,
                "Cập nhật thông tin nhóm khách hàng thành công",
                null
        );

        return ResponseEntity.ok(response);
    }

    /**
     * API: Xóa mềm nhóm khách hàng (Chuyển trạng thái sang INACTIVE)
     * Endpoint: DELETE /api/v1/crm/customer-groups/{id}
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<RestResponse<Void>> softDeleteCustomerGroup(@PathVariable Integer id) {

        groupService.softDeleteCustomerGroup(id);

        RestResponse<Void> response = new RestResponse<>(
                HttpStatus.OK.value(),
                null,
                "Xóa nhóm khách hàng thành công (Xóa mềm)",
                null
        );

        return ResponseEntity.ok(response);
    }


    /**
     * API Giả lập đơn hàng hoàn thành phục vụ test luồng tự động CRM
     * Endpoint: POST /api/v1/crm/complete
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


}