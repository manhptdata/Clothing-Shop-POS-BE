package com.sapo.mock.clothing.customer.service.impl;


import com.sapo.mock.clothing.customer.dto.request.customer.CustomerCreateRequest;
import com.sapo.mock.clothing.customer.dto.request.customer.CustomerUpdateRequest;
import com.sapo.mock.clothing.customer.dto.response.CustomerResponse;
import com.sapo.mock.clothing.customer.repository.CustomerRepository;
import com.sapo.mock.clothing.customer.service.CustomerService;
import com.sapo.mock.clothing.entity.Customer;
import com.sapo.mock.clothing.util.constant.CustomerStatusEnum;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CustomerServiceImpl implements CustomerService {

    @Autowired
    private CustomerRepository customerRepository;

    // Tìm kiếm khách hàng ACTIVE theo tên hoặc số điện thoại, trả về kết quả dưới dạng Page<CustomerResponse>.
    @Override
    public Page<CustomerResponse> searchCustomers(String keyword, Pageable pageable) {
        Page<Customer> customers = customerRepository.searchActiveCustomers(keyword, pageable);
        return customers.map(this::convertToResponse);
    }

    private CustomerResponse convertToResponse(Customer customer) {
        CustomerResponse response = new CustomerResponse();
        response.setId(customer.getId());
        response.setFullName(customer.getFullName());
        response.setPhone(customer.getPhone());
        response.setDateOfBirth(customer.getDateOfBirth());
        response.setGender(customer.getGender());
        response.setAddress(customer.getAddress());
        response.setNote(customer.getNote());
        response.setStatus(customer.getStatus());
        response.setCreatedAt(customer.getCreatedAt());
        return response;
    }


    // Lấy thông tin chi tiết khách hàng theo ID, nếu không tìm thấy thì ném lỗi.
    @Override
    public CustomerResponse getCustomerById(Integer id) {
        Customer customer = customerRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy khách hàng với ID: " + id));

        return convertToResponse(customer);
    }


    // Handle customer creation.
    @Override
    public CustomerResponse createCustomer(CustomerCreateRequest request) {
        if (customerRepository.existsByPhone(request.getPhone())) {
            throw new RuntimeException("Số điện thoại này đã tồn tại trên hệ thống!");
        }

        Customer customer = new Customer();
        customer.setFullName(request.getFullName());
        customer.setPhone(request.getPhone());
        customer.setDateOfBirth(request.getDateOfBirth());
        customer.setGender(request.getGender());
        customer.setAddress(request.getAddress());
        customer.setNote(request.getNote());
        customer.setStatus(CustomerStatusEnum.ACTIVE);

        Customer savedCustomer = customerRepository.save(customer);
        return convertToResponse(savedCustomer);
    }

    @Override
    @Transactional
    public CustomerResponse updateCustomer(Integer id, CustomerUpdateRequest request) {
        Customer customer = customerRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy khách hàng với ID: " + id));

        if (customerRepository.existsByPhoneAndIdNot(request.getPhone(), id)) {
            throw new RuntimeException("Số điện thoại này đã được sử dụng bởi một khách hàng khác!");
        }

        // Map updated data from the request to the existing entity.
        customer.setFullName(request.getFullName());
        customer.setPhone(request.getPhone());
        customer.setDateOfBirth(request.getDateOfBirth());
        customer.setGender(request.getGender());
        customer.setAddress(request.getAddress());
        customer.setNote(request.getNote());
        customer.setStatus(request.getStatus());

        Customer updatedCustomer = customerRepository.save(customer);

        // Return the result as a response DTO.
        return convertToResponse(updatedCustomer);
    }

    // Soft delete a customer.
    @Override
    @Transactional
    public void deactivateCustomer(Integer id) {
        Customer customer = customerRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy khách hàng với ID: " + id));

        customer.setStatus(CustomerStatusEnum.INACTIVE);
        // 3. Lưu lại vào DB (Kích hoạt @PreUpdate cập nhật thời gian chỉnh sửa)
        customerRepository.save(customer);
    }

    // Unlock client
    @Override
    @Transactional
    public void activateCustomer(Integer id) {
        // 1. Tìm khách hàng (Kể cả đang INACTIVE vẫn phải tìm ra để mở khóa)
        Customer customer = customerRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy khách hàng với ID: " + id));

        // 2. Chuyển trạng thái hoạt động trở lại thành ACTIVE
        customer.setStatus(CustomerStatusEnum.ACTIVE);

        // 3. Lưu lại vào DB (Tự động kích hoạt @PreUpdate lưu thời gian mở khóa)
        customerRepository.save(customer);
    }

    // Get customers by group ID, only ACTIVE ones.
    @Override
    public Page<CustomerResponse> getCustomersByGroupId(Integer groupId, Pageable pageable) {
        Page<Customer> customers = customerRepository.findCustomersByGroupId(groupId, pageable);

        return customers.map(this::convertToResponse);
    }
}

