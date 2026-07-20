package com.sapo.mock.clothing.customer.controller;

import com.sapo.mock.clothing.common.dto.response.RestResponse;
import com.sapo.mock.clothing.customer.dto.request.customer.CustomerCreateRequest;
import com.sapo.mock.clothing.customer.dto.request.customer.CustomerUpdateRequest;
import com.sapo.mock.clothing.customer.dto.response.CustomerResponse;
import com.sapo.mock.clothing.customer.dto.response.OrderHistoryResponse;
import com.sapo.mock.clothing.customer.service.CustomerService;
import com.sapo.mock.clothing.customer.service.file.CustomerFileService;
import com.sapo.mock.clothing.customer.service.file.ExcelHelper;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import com.sapo.mock.clothing.entity.PointHistory;
import com.sapo.mock.clothing.customer.repository.PointHistoryRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;

@RestController
@RequestMapping("/api/crm/customers")
@CrossOrigin(origins = "*") // Enable CORS for frontend requests.
@RequiredArgsConstructor
public class CustomerController {

    @Autowired
    private CustomerService customerService;
    @Autowired
    private PointHistoryRepository pointHistoryRepository;

    private final CustomerFileService customerFileService;

    @GetMapping("/{customerId}/point-history")
    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN', 'VIEW_CUSTOMER')")
    public ResponseEntity<RestResponse<Page<PointHistory>>> getPointHistory(
            @PathVariable Integer customerId,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size) {
        Page<PointHistory> result = pointHistoryRepository.findByCustomerIdOrderByCreatedAtDesc(
                customerId, 
                PageRequest.of(page > 0 ? page - 1 : 0, size)
        );
        RestResponse<Page<PointHistory>> response = new RestResponse<>();
        response.setStatusCode(200);
        response.setMessage("Tải lịch sử điểm thưởng thành công");
        response.setData(result);
        return ResponseEntity.ok(response);
    }

    /**
     * Customer lookup API using the shared RestResponse format.
     * Endpoint: GET /api/crm/customers/search?keyword=0987
     */
    @GetMapping("/search")
    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN', 'VIEW_CUSTOMER', 'MANAGE_CUSTOMER', 'CREATE_ORDER')")
    public ResponseEntity<RestResponse<Page<CustomerResponse>>> searchCustomers(
            @RequestParam(required = false, defaultValue = "") String keyword,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {

        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        Page<CustomerResponse> result = customerService.searchCustomers(keyword, pageable);

        // Khởi tạo và gán data vào RestResponse theo đúng cấu trúc của bạn
        RestResponse<Page<CustomerResponse>> response = new RestResponse<>();
        response.setStatusCode(HttpStatus.OK.value());
        response.setError(null);
        response.setMessage("Tra cứu danh sách khách hàng thành công");
        response.setData(result);

        return ResponseEntity.ok(response);
    }

    /**
     *Lọc danh sách khách hàng chuyên dụng duy nhất theo Tháng sinh
     * Endpoint: GET /api/crm/customers/birthday?month=06
     */
    @GetMapping("/birthday")
    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN', 'VIEW_CUSTOMER', 'MANAGE_CUSTOMER', 'CREATE_ORDER')")
    public ResponseEntity<RestResponse<Page<CustomerResponse>>> getBirthdayCustomers(
            @RequestParam(required = false, defaultValue = "") String month,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size) {

        int targetPage = (page <= 0) ? 1 : page;
        Pageable pageable = PageRequest.of(targetPage - 1, size, Sort.by("created_at").descending());

        Page<CustomerResponse> result = customerService.searchByBirthMonth(month, pageable);

        RestResponse<Page<CustomerResponse>> response = new RestResponse<>();
        response.setStatusCode(HttpStatus.OK.value());
        response.setError(null);
        response.setMessage("Lọc danh sách khách hàng theo tháng sinh nhật thành công");
        response.setData(result);

        return ResponseEntity.ok(response);
    }

    /**
     * API Xem chi tiết khách hàng
     * Endpoint: GET /api/crm/customers/1
     */
    @GetMapping("/{id}")
    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN', 'VIEW_CUSTOMER', 'MANAGE_CUSTOMER', 'CREATE_ORDER')")
    public ResponseEntity<RestResponse<CustomerResponse>> getCustomerDetail(@PathVariable Integer id) {

        // Gọi service lấy thông tin chi tiết
        CustomerResponse result = customerService.getCustomerById(id);

        // Bọc dữ liệu vào RestResponse của nhóm
        RestResponse<CustomerResponse> response = new RestResponse<>();
        response.setStatusCode(HttpStatus.OK.value());
        response.setError(null);
        response.setMessage("Lấy thông tin chi tiết khách hàng thành công");
        response.setData(result);

        return ResponseEntity.ok(response);
    }

    /**
     * API Thêm mới khách hàng tại quầy
     * Endpoint: POST /api/crm/customers
     */
    @PostMapping
    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN', 'MANAGE_CUSTOMER', 'CREATE_ORDER')")
    public ResponseEntity<RestResponse<CustomerResponse>> createCustomer(
            @Valid @RequestBody CustomerCreateRequest request) {

        // Gọi tầng service xử lý lưu thông tin
        CustomerResponse result = customerService.createCustomer(request);

        // Đóng gói dữ liệu vào cấu trúc RestResponse chung của nhóm
        RestResponse<CustomerResponse> response = new RestResponse<>();
        response.setStatusCode(HttpStatus.CREATED.value()); // Trả về mã thành công 201 Created
        response.setError(null);
        response.setMessage("Tạo mới tài khoản khách hàng thành công");
        response.setData(result);

        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }

    /**
     * API Cập nhật thông tin khách hàng
     * Endpoint: PUT /api/crm/customers/11
     */
    @PutMapping("/{id}")
    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN', 'MANAGE_CUSTOMER')")
    public ResponseEntity<RestResponse<CustomerResponse>> updateCustomer(
            @PathVariable Integer id,
            @Valid @RequestBody CustomerUpdateRequest request) {

        // Gọi tầng service thực hiện cập nhật thông tin
        CustomerResponse result = customerService.updateCustomer(id, request);

        // Đóng gói trả về RestResponse theo cấu trúc của nhóm bạn
        RestResponse<CustomerResponse> response = new RestResponse<>();
        response.setStatusCode(HttpStatus.OK.value()); // Mã 200 OK thành công
        response.setError(null);
        response.setMessage("Cập nhật thông tin khách hàng thành công");
        response.setData(result);

        return ResponseEntity.ok(response);
    }

    /**
     * API Khóa tài khoản khách hàng (Xóa mềm)
     * Endpoint: PATCH /api/crm/customers/11/deactivate
     */
    @PatchMapping("/{id}/deactivate")
    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN', 'MANAGE_CUSTOMER')")
    public ResponseEntity<RestResponse<Void>> deactivateCustomer(@PathVariable Integer id) {

        // Gọi tầng service xử lý chuyển trạng thái sang INACTIVE
        customerService.deactivateCustomer(id);

        // Đóng gói trả về RestResponse không kèm data (Void)
        RestResponse<Void> response = new RestResponse<>();
        response.setStatusCode(HttpStatus.OK.value()); // Mã 200 OK thành công
        response.setError(null);
        response.setMessage("Khóa tài khoản khách hàng thành công");
        response.setData(null);

        return ResponseEntity.ok(response);
    }
    /**
     * API Mở khóa tài khoản khách hàng (Kích hoạt lại)
     * Endpoint: PATCH /api/crm/customers/11/activate
     */
    @PatchMapping("/{id}/activate")
    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN', 'MANAGE_CUSTOMER')")
    public ResponseEntity<RestResponse<Void>> activateCustomer(@PathVariable Integer id) {

        // Gọi tầng service xử lý chuyển trạng thái sang ACTIVE
        customerService.activateCustomer(id);

        RestResponse<Void> response = new RestResponse<>();
        response.setStatusCode(HttpStatus.OK.value());
        response.setError(null);
        response.setMessage("Kích hoạt lại tài khoản khách hàng thành công");
        response.setData(null);

        return ResponseEntity.ok(response);
    }


    /**
     * API: Xem lịch sử đơn hàng của 1 khách hàng cụ thể
     * URL: GET /api/crm/customers/{customerId}/orders?page=1&size=5
     */
    @GetMapping("/{customerId}/orders")
    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN', 'VIEW_CUSTOMER', 'MANAGE_CUSTOMER', 'CREATE_ORDER')")
    public ResponseEntity<RestResponse<Page<OrderHistoryResponse>>> getCustomerOrders(
            @PathVariable Integer customerId,
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "5") int size) {

        int targetPage = (page <= 0) ? 1 : page;
        Pageable pageable = PageRequest.of(targetPage - 1, size, org.springframework.data.domain.Sort.by("createdAt").descending());

        Page<OrderHistoryResponse> result = customerService.getCustomerOrders(customerId, keyword, pageable);

        RestResponse<Page<OrderHistoryResponse>> response = new RestResponse<>();
        response.setStatusCode(HttpStatus.OK.value());
        response.setMessage("Tải lịch sử đơn hàng của khách thành công!");
        response.setData(result);

        return ResponseEntity.ok(response);
    }

    @PostMapping("/import")
    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN', 'MANAGE_CUSTOMER')")
    public ResponseEntity<?> importCustomersExcel(@RequestParam("file") MultipartFile file) {
        // 1. Kiểm tra file trống
        if (file.isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("message", "Vui lòng chọn một file Excel trước khi gửi!"));
        }

        // 2. Kiểm tra định dạng file có phải .xlsx không
        if (!ExcelHelper.hasExcelFormat(file)) {
            return ResponseEntity.status(HttpStatus.UNSUPPORTED_MEDIA_TYPE)
                    .body(Map.of("message", "Định dạng file không hợp lệ! Vui lòng upload file .xlsx"));
        }

        try {
            // 3. Thực hiện import dữ liệu
            customerFileService.saveCustomersFromExcel(file);
            return ResponseEntity.ok(Map.of("message", "Import danh sách khách hàng bằng Excel thành công!"));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("message", "Import thất bại: " + e.getMessage()));
        }
    }

    @DeleteMapping("/vouchers/{customerVoucherId}")
    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN', 'MANAGE_CUSTOMER')")
    public ResponseEntity<RestResponse<Void>> revokeCustomerVoucher(@PathVariable Integer customerVoucherId) {
        customerService.revokeCustomerVoucher(customerVoucherId);
        RestResponse<Void> response = new RestResponse<>(HttpStatus.OK.value(), null, "Thu hồi voucher khỏi ví thành công", null);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/{customerId}/vouchers/{voucherId}")
    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN', 'MANAGE_CUSTOMER')")
    public ResponseEntity<RestResponse<Void>> giveCustomerVoucher(@PathVariable Integer customerId, @PathVariable Integer voucherId) {
        customerService.giveCustomerVoucher(customerId, voucherId);
        RestResponse<Void> response = new RestResponse<>(HttpStatus.OK.value(), null, "Tặng voucher cho khách hàng thành công", null);
        return ResponseEntity.ok(response);
    }
}