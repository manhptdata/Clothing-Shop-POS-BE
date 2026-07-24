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
import com.sapo.mock.clothing.entity.Notification;
import com.sapo.mock.clothing.entity.Order;
import com.sapo.mock.clothing.exception.BadRequestException;
import com.sapo.mock.clothing.exception.ResourceNotFoundException;
import com.sapo.mock.clothing.notification.service.NotificationService;
import com.sapo.mock.clothing.order.repository.OrderRepository;
import com.sapo.mock.clothing.util.constant.CustomerStatusEnum;
import com.sapo.mock.clothing.util.constant.VoucherCampaignStatusEnum;
import java.time.Instant;
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

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private NotificationService notificationService;

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
        response.setTotalSpent(customer.getTotalSpent());

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
            Instant now = Instant.now();
            List<CustomerResponse.VoucherInfo> voucherInfos = vouchers.stream()
                    .filter(cv -> {
                        if (cv.getVoucher() == null) return false;
                        if (cv.getVoucher().getStatus() != VoucherCampaignStatusEnum.ACTIVE) return false;
                        if (cv.getVoucher().getEndDate() != null && now.isAfter(cv.getVoucher().getEndDate())) return false;
                        return true;
                    })
                    .map(cv -> {
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
                if (cv.getOrderId() != null) {
                    vi.setUsedOrderId(cv.getOrderId());
                    orderRepository.findById(cv.getOrderId()).ifPresent(order -> {
                        vi.setUsedOrderCode(order.getOrderNumber());
                    });
                }
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
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy khách hàng với ID: " + id));
        return convertToResponse(customer);
    }


    // Handle customer creation.
    @Override
    public CustomerResponse createCustomer(CustomerCreateRequest request) {
        if (customerRepository.existsByPhone(request.getPhone())) {
            throw new BadRequestException("Số điện thoại này đã tồn tại trên hệ thống!");
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

        try {
            Notification notification = new Notification();
            notification.setTitle("Khách hàng mới");
            notification.setMessage("Khách hàng '" + savedCustomer.getFullName() + "' (" + savedCustomer.getPhone() + ") vừa được đăng ký thành công.");
            notification.setType("NEW_CUSTOMER");
            notification.setTargetRole("ROLE_ADMIN,ROLE_MANAGER,ROLE_CASHIER");
            notification.setMetadata("{\"customerId\":" + savedCustomer.getId() + "}");
            notificationService.sendNotification(notification);
        } catch (Exception e) {
            System.err.println("Lỗi gửi thông báo khách hàng mới: " + e.getMessage());
        }

        return convertToResponse(savedCustomer);
    }

    @Override
    @Transactional
    public CustomerResponse updateCustomer(Integer id, CustomerUpdateRequest request) {
        Customer customer = customerRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy khách hàng với ID: " + id));

        if (customerRepository.existsByPhoneAndIdNot(request.getPhone(), id)) {
            throw new BadRequestException("Số điện thoại này đã được sử dụng bởi một khách hàng khác!");
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
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy khách hàng với ID: " + id));

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
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy khách hàng với ID: " + id));

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
    public Page<OrderHistoryResponse> getCustomerOrders(Integer customerId, String keyword, Pageable pageable) {
        // Kiểm tra xem khách hàng có tồn tại không trước khi lấy đơn
        if (!customerRepository.existsById(customerId)) {
            throw new ResourceNotFoundException("Không tìm thấy khách hàng với ID: " + customerId);
        }

        // Gọi câu Query lấy Order vừa khai báo trong CustomerRepository
        Page<Order> orders = customerRepository.findOrdersByCustomerId(customerId, keyword, pageable);

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

    @Autowired
    private com.sapo.mock.clothing.customer.repository.VoucherRepository voucherRepository;

    @Override
    @Transactional
    public void revokeCustomerVoucher(Integer customerVoucherId) {
        CustomerVoucher cv = customerVoucherRepository.findById(customerVoucherId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy voucher trong ví khách hàng"));
        
        com.sapo.mock.clothing.entity.Voucher voucher = voucherRepository.findByIdWithPessimisticLock(cv.getVoucher().getId())
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy voucher gốc"));
        
        if (voucher.getIssuedQuantity() > 0) {
            voucher.setIssuedQuantity(voucher.getIssuedQuantity() - 1);
            voucherRepository.save(voucher);
        }

        customerVoucherRepository.delete(cv);
    }

    @Override
    @Transactional
    public void giveCustomerVoucher(Integer customerId, Integer voucherId) {
        Customer customer = customerRepository.findById(customerId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy khách hàng"));
        com.sapo.mock.clothing.entity.Voucher voucher = voucherRepository.findByIdWithPessimisticLock(voucherId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy chương trình voucher"));

        // 1. Kiểm tra trạng thái chương trình
        if (voucher.getStatus() != com.sapo.mock.clothing.util.constant.VoucherCampaignStatusEnum.ACTIVE) {
            throw new BadRequestException("Chương trình voucher này đang tạm dừng hoặc đã kết thúc!");
        }

        // 2. Kiểm tra tổng số lượng phát hành (Ngân sách)
        if (voucher.getTotalQuantity() != null && voucher.getIssuedQuantity() != null
                && voucher.getIssuedQuantity() >= voucher.getTotalQuantity()) {
            throw new BadRequestException("Chương trình voucher này đã hết số lượng phát hành!");
        }

        // 3. Kiểm tra số lần phát tối đa cho 1 khách hàng
        int maxUsagePerUser = voucher.getMaxUsagePerUser() != null ? voucher.getMaxUsagePerUser() : 1;
        long issuedCount = customerVoucherRepository.countByCustomerIdAndVoucherId(customerId, voucherId);
        if (issuedCount >= maxUsagePerUser) {
            throw new BadRequestException("Khách hàng này đã nhận tối đa " + maxUsagePerUser + " lượt cho chương trình voucher này!");
        }

        // 4. Tính hạn sử dụng
        Instant expiredAt = voucher.getEndDate();
        if (expiredAt == null) {
            // Hạn sử dụng mặc định là 30 ngày nếu chương trình voucher không cài ngày kết thúc
            expiredAt = Instant.now().plus(30, java.time.temporal.ChronoUnit.DAYS);
        }

        try {
            CustomerVoucher cv = new CustomerVoucher();
            cv.setCustomer(customer);
            cv.setVoucher(voucher);
            cv.setStatus(com.sapo.mock.clothing.util.constant.CustomerVoucherStatusEnum.UNUSED);
            cv.setReceivedAt(Instant.now());
            cv.setExpiredAt(expiredAt);
            customerVoucherRepository.save(cv);

            // Cập nhật số lượng đã phát (issuedQuantity) của voucher
            voucher.setIssuedQuantity((voucher.getIssuedQuantity() != null ? voucher.getIssuedQuantity() : 0) + 1);
            voucherRepository.save(voucher);
        } catch (org.springframework.dao.DataIntegrityViolationException e) {
            throw new BadRequestException("Không thể tặng voucher do dữ liệu không hợp lệ.");
        }
    }
}

