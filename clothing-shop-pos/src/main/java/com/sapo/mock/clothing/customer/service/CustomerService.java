package com.sapo.mock.clothing.customer.service;

import com.sapo.mock.clothing.customer.dto.request.customer.CustomerCreateRequest;
import com.sapo.mock.clothing.customer.dto.request.customer.CustomerUpdateRequest;
import com.sapo.mock.clothing.customer.dto.response.CustomerResponse;
import com.sapo.mock.clothing.customer.dto.response.OrderHistoryResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface CustomerService {
    // Search ACTIVE customers by name or phone number.
    Page<CustomerResponse> searchCustomers(String keyword, Pageable pageable);

    // Search ACTIVE customers by birth month (format: "MM").
    Page<CustomerResponse> searchByBirthMonth(String month, Pageable pageable);

    // Retrieve customer details by ID.
    CustomerResponse getCustomerById(Integer id);

    // Handle customer creation.
    CustomerResponse createCustomer(CustomerCreateRequest request);

    // Update customer details.
    CustomerResponse updateCustomer(Integer id, CustomerUpdateRequest request);

    // Soft delete a customer.
    void deactivateCustomer(Integer id);
    // Unlock client
    void activateCustomer(Integer id);

    // Get customers by group ID, only ACTIVE ones.
    Page<CustomerResponse> getCustomersByGroupId(Integer groupId, String keyword, Pageable pageable);



    Page<OrderHistoryResponse> getCustomerOrders(Integer customerId, String keyword, Pageable pageable);

    void revokeCustomerVoucher(Integer customerVoucherId);

    void giveCustomerVoucher(Integer customerId, Integer voucherId);
}

