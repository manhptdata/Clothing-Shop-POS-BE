package com.sapo.mock.clothing.customer.service;

import com.sapo.mock.clothing.customer.dto.request.groupcustomer.CustomerGroupRequest;
import com.sapo.mock.clothing.customer.dto.request.groupcustomer.CustomerRequest;
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
    // Hàm xử lý gán khách hàng cũ vào một nhóm (Hoặc chuyển nhóm)
    CustomerResponse assignGroupToCustomer(Integer customerId, CustomerRequest request);

    // Hàm chỉ gán/đổi/rút nhóm nhanh mà không cần gửi lại toàn bộ thông tin khách hàng
    CustomerResponse assignOnlyGroup(Integer customerId, Integer customerGroupId);
}