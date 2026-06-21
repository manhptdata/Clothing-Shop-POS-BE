package com.sapo.mock.clothing.customer.controller;

import com.sapo.mock.clothing.common.dto.response.RestResponse;
import com.sapo.mock.clothing.customer.dto.request.customer.CustomerCreateRequest;
import com.sapo.mock.clothing.customer.dto.request.customer.CustomerUpdateRequest;
import com.sapo.mock.clothing.customer.dto.response.CustomerResponse;
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
@RequestMapping("/api/v1/crm/customers")
@CrossOrigin(origins = "*") // Enable CORS for frontend requests.
public class CustomerController {

    @Autowired
    private CustomerService customerService;

    /**
     * Customer lookup API using the shared RestResponse format.
     * Endpoint: GET /api/v1/crm/customers/search?keyword=0987
     */
    @GetMapping("/search")
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
     * Endpoint: GET /api/v1/crm/customers/birthday?month=06
     */
    @GetMapping("/birthday")
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
     * Endpoint: GET /api/v1/crm/customers/1
     */
    @GetMapping("/{id}")
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
     * Endpoint: POST /api/v1/crm/customers
     */
    @PostMapping
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
     * Endpoint: PUT /api/v1/crm/customers/11
     */
    @PutMapping("/{id}")
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
     * Endpoint: PATCH /api/v1/crm/customers/11/deactivate
     */
    @PatchMapping("/{id}/deactivate")
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
     * Endpoint: PATCH /api/v1/crm/customers/11/activate
     */
    @PatchMapping("/{id}/activate")
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
}