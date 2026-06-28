package com.sapo.mock.clothing.customer.service.impl;


import com.sapo.mock.clothing.customer.dto.request.customer.CustomerCreateRequest;
import com.sapo.mock.clothing.customer.dto.request.customer.CustomerUpdateRequest;
import com.sapo.mock.clothing.customer.dto.response.CustomerResponse;
import com.sapo.mock.clothing.customer.dto.response.OrderHistoryResponse;
import com.sapo.mock.clothing.customer.repository.CustomerRepository;
import com.sapo.mock.clothing.customer.repository.CustomerVoucherRepository;
import com.sapo.mock.clothing.customer.service.CustomerService;
import com.sapo.mock.clothing.entity.Customer;
import com.sapo.mock.clothing.entity.CustomerVoucher;
import com.sapo.mock.clothing.entity.Order;
import com.sapo.mock.clothing.util.constant.CustomerStatusEnum;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class CustomerServiceImpl implements CustomerService {

    @Autowired
    private CustomerRepository customerRepository;

    @Autowired
    private CustomerVoucherRepository customerVoucherRepository;

    // Tìm kiếm khách hàng ACTIVE theo tên hoặc số điện thoại, trả về kết quả dưới dạng Page<CustomerResponse>.
    @Override
    public Page<CustomerResponse> searchCustomers(String keyword, Pageable pageable) {
        Page<Customer> customers = customerRepository.searchActiveCustomers(keyword, pageable);
        return customers.map(this::convertToResponse);
    }
    @Override
    public Page<CustomerResponse> searchByBirthMonth(String month, Pageable pageable) {
        // Gọi hàm Repo số 2 chuyên chọc vào tháng của date_of_birth
        Page<Customer> customers = customerRepository.searchByBirthMonth(month, pageable);
        return customers.map(this::convertToResponse);
    }

    private CustomerResponse convertToResponse(Customer customer) {
        CustomerResponse response = new CustomerResponse();
        response.setId(customer.getId());
        response.setFullName(customer.getFullName());
        response.setPhone(customer.getPhone());
        response.setEmail(customer.getEmail());
        response.setDateOfBirth(customer.getDateOfBirth());
        response.setGender(customer.getGender());
        response.setAddress(customer.getAddress());
        response.setNote(customer.getNote());
        response.setStatus(customer.getStatus());
        response.setCreatedAt(customer.getCreatedAt());
        response.setRewardPoints(customer.getRewardPoints());

        if (customer.getCustomerGroup() != null) {
            CustomerResponse.GroupInfo groupInfo = new CustomerResponse.GroupInfo();
            groupInfo.setId(customer.getCustomerGroup().getId());
            groupInfo.setName(customer.getCustomerGroup().getName());
            groupInfo.setCode(customer.getCustomerGroup().getCode());
            response.setCustomerGroup(groupInfo);
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
            response.setVouchers(voucherInfos);
        }
        // Không có voucher → giữ nguyên null, không set gì

        return response;
    }


    // Lấy thông tin chi tiết khách hàng theo ID
    @Override
    @Transactional(readOnly = true)
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
        String emailInput = (request.getEmail() == null || request.getEmail().trim().isEmpty()) ? null : request.getEmail().trim();
        customer.setEmail(emailInput);
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
        String emailInput = (request.getEmail() == null || request.getEmail().trim().isEmpty()) ? null : request.getEmail().trim();
        customer.setEmail(emailInput);
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
    public Page<CustomerResponse> getCustomersByGroupId(Integer groupId, String keyword, Pageable pageable) {
        // Xử lý chuẩn hóa từ khóa nếu Frontend truyền chuỗi rỗng "" hoặc khoảng trắng
        String searchKey = (keyword == null || keyword.trim().isEmpty()) ? null : keyword.trim();

        Page<Customer> customers = customerRepository.searchMembersInGroup(groupId, searchKey, pageable);

        // Tái sử dụng hàm helper convertToResponse có sẵn của Đức để map sang DTO
        return customers.map(this::convertToResponse);
    }
    // Hàm Helper chuyển đổi dữ liệu Khách hàng sang DTO (dùng cho getCustomersByGroupId)
    // Tái sử dụng convertToResponse để đảm bảo vouchers cũng được set đồng nhất
    private CustomerResponse convertToCustomerResponse(Customer customer) {
        return convertToResponse(customer);
    }



    // 🌟 Override hàm lấy đơn hàng, gọi trực tiếp qua customerRepository gốc
    @Override
    @Transactional(readOnly = true)
    public Page<OrderHistoryResponse> getCustomerOrders(Integer customerId, Pageable pageable) {
        // Kiểm tra xem khách hàng có tồn tại không trước khi lấy đơn
        if (!customerRepository.existsById(customerId)) {
            throw new RuntimeException("Không tìm thấy khách hàng với ID: " + customerId);
        }

        // Gọi câu Query lấy Order vừa khai báo trong CustomerRepository
        Page<Order> orders = customerRepository.findOrdersByCustomerId(customerId, pageable);

        return orders.map(this::convertToOrderHistoryResponse);
    }

    // Hàm Helper chuyển đổi từ Entity Order sang DTO Response (Đức dán vào cuối file Impl)
    private OrderHistoryResponse convertToOrderHistoryResponse(Order order) {
        OrderHistoryResponse response = new OrderHistoryResponse();
        response.setId(order.getId());
        response.setOrderNumber(order.getOrderNumber());
        response.setCustomerId(order.getCustomerId());
        response.setCustomerName(order.getCustomerName());
        response.setCreatedById(order.getCreatedBy());
        response.setCreatedByUsername(order.getCreatedByUsername());
        response.setTotalAmount(order.getTotalAmount());
        response.setPaidAmount(order.getPaidAmount());
        response.setChangeAmount(order.getChangeAmount());
        response.setStatus(order.getStatus());
        response.setNote(order.getNote());
        response.setCreatedAt(order.getCreatedAt());
        response.setUpdatedAt(order.getUpdatedAt());
        response.setPrinted(order.isPrinted());

        if (order.getItems() != null) {
            List<OrderHistoryResponse.ItemInfo> itemDtos = order.getItems().stream().map(item -> {
                OrderHistoryResponse.ItemInfo itemDto = new OrderHistoryResponse.ItemInfo();
                itemDto.setId(item.getId());
                itemDto.setVariantId(item.getVariantId());
                itemDto.setProductName(item.getProductName());
                itemDto.setProductSku(item.getProductSku());
                itemDto.setQuantity(item.getQuantity());
                itemDto.setUnitPrice(item.getUnitPrice());
                itemDto.setSubtotal(item.getSubtotal());
                return itemDto;
            }).collect(Collectors.toList());
            response.setItems(itemDtos);
        }
        return response;
    }


}

