package com.sapo.mock.clothing.customer.controller;

import com.sapo.mock.clothing.common.dto.response.RestResponse;
import com.sapo.mock.clothing.customer.dto.request.groupcustomer.AssignGroupRequest;
import com.sapo.mock.clothing.customer.dto.request.groupcustomer.CustomerGroupRequest;
import com.sapo.mock.clothing.customer.dto.request.groupcustomer.CustomerRequest;
import com.sapo.mock.clothing.customer.dto.response.CustomerGroupResponse;
import com.sapo.mock.clothing.customer.dto.response.CustomerResponse;
import com.sapo.mock.clothing.customer.service.CustomerGroupService;
import com.sapo.mock.clothing.customer.service.CustomerService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
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

    /**
     * API: Lấy TẤT CẢ các nhóm khách hàng hiện có (Không phân trang)
     * Endpoint: GET /api/v1/crm/customer-groups/all
     */
    @GetMapping("")
    public ResponseEntity<RestResponse<Page<CustomerGroupResponse>>> getGroupsWithPage(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {

        // Tạo đối tượng Pageable (ở đây Repo đã sắp xếp theo ID tăng dần qua tên hàm rồi)
        Pageable pageable = PageRequest.of(page, size);

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
     * API 2: Xem chi tiết một nhóm khách hàng cụ thể
     * Endpoint: GET /api/v1/crm/customer-groups/1
     */
    @GetMapping("/{id}")
    public ResponseEntity<RestResponse<CustomerGroupResponse>> getGroupDetail(@PathVariable Integer id) {

        CustomerGroupResponse result = groupService.getGroupById(id);

        RestResponse<CustomerGroupResponse> response = new RestResponse<>();
        response.setStatusCode(HttpStatus.OK.value());
        response.setError(null);
        response.setMessage("Lấy chi tiết nhóm khách hàng thành công");
        response.setData(result);

        return ResponseEntity.ok(response);
    }

    /**
     * API: Tạo mới nhóm khách hàng
     * Endpoint: POST /api/v1/crm/customer-groups
     */
    @PostMapping
    public ResponseEntity<RestResponse<CustomerGroupResponse>> createGroup(
            @Valid @RequestBody CustomerGroupRequest request) {

        CustomerGroupResponse result = groupService.createGroup(request);

        RestResponse<CustomerGroupResponse> response = new RestResponse<>();
        response.setStatusCode(HttpStatus.CREATED.value()); // Trả về mã 201 Created
        response.setError(null);
        response.setMessage("Tạo mới nhóm khách hàng thành công");
        response.setData(result);

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
     * Endpoint: GET /api/v1/crm/customer-groups/{groupId}/members
     */
    @GetMapping("/{groupId}/members")
    public ResponseEntity<RestResponse<Page<CustomerResponse>>> getGroupMembers(
            @PathVariable Integer groupId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {

        Pageable pageable = PageRequest.of(page, size);

        // Gọi sang CustomerService để lấy danh sách thành viên
        Page<CustomerResponse> result = customerService.getCustomersByGroupId(groupId, pageable);

        RestResponse<Page<CustomerResponse>> response = new RestResponse<>();
        response.setStatusCode(HttpStatus.OK.value());
        response.setError(null);
        response.setMessage("Lấy danh sách thành viên của nhóm thành công");
        response.setData(result);

        return ResponseEntity.ok(response);
    }
}