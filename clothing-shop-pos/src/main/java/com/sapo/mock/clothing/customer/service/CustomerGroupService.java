package com.sapo.mock.clothing.customer.service;

import com.sapo.mock.clothing.customer.dto.request.groupcustomer.CustomerGroupRequest;
import com.sapo.mock.clothing.customer.dto.response.CustomerGroupResponse;
import com.sapo.mock.clothing.customer.dto.response.CustomerResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface CustomerGroupService {
    // Retrieve all customer groups.
    Page<CustomerGroupResponse> getGroupsWithPage(Pageable pageable);

    //  tìm kiếm nhóm ACTIVE
    Page<CustomerGroupResponse> searchGroups(String keyword, Pageable pageable);

    // Lấy chi tiết 1
    CustomerGroupResponse getGroupById(Integer id);

    CustomerGroupResponse createGroup(CustomerGroupRequest request);
    CustomerGroupResponse updateGroup(Integer id, CustomerGroupRequest request);
    void deleteGroup(Integer id);

    // Đồng bộ hạng cho tất cả khách hàng
    void syncAllCustomerRanks();

    Page<com.sapo.mock.clothing.customer.dto.response.CustomerVoucherHistoryResponse> getVoucherHistory(String keyword, Pageable pageable);
}