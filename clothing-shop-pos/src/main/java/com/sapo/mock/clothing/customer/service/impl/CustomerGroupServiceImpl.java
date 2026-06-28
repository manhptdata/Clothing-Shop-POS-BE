package com.sapo.mock.clothing.customer.service.impl;


import com.sapo.mock.clothing.customer.dto.response.CustomerGroupResponse;
import com.sapo.mock.clothing.customer.dto.response.CustomerResponse;
import com.sapo.mock.clothing.customer.repository.CustomerGroupRepository;
import com.sapo.mock.clothing.customer.repository.CustomerRepository;
import com.sapo.mock.clothing.customer.repository.CustomerVoucherRepository;
import com.sapo.mock.clothing.customer.service.CustomerGroupService;
import com.sapo.mock.clothing.entity.Customer;
import com.sapo.mock.clothing.entity.CustomerGroup;
import com.sapo.mock.clothing.entity.CustomerVoucher;
import com.sapo.mock.clothing.util.constant.CustomerStatusEnum;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class CustomerGroupServiceImpl implements CustomerGroupService {

    @Autowired
    private CustomerGroupRepository groupRepository;

    @Autowired
    private CustomerRepository customerRepository;

    @Autowired
    private CustomerGroupRepository customerGroupRepository;

    @Autowired
    private CustomerVoucherRepository customerVoucherRepository;



    // Retrieve all active customer groups.
    @Override
    public Page<CustomerGroupResponse> getGroupsWithPage(Pageable pageable) {
        return customerGroupRepository.findAllActiveGroups(pageable);
    }

    //  tìm kiếm nhóm ACTIVE
    @Override
    public Page<CustomerGroupResponse> searchGroups(String keyword, Pageable pageable) {
        String cleanKeyword = (keyword != null && !keyword.trim().isEmpty()) ? keyword.trim() : null;
        return customerGroupRepository.searchGroups(cleanKeyword, pageable);
    }

    @Override
    public CustomerGroupResponse getGroupById(Integer id) {
        return customerGroupRepository.findGroupDetailById(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy nhóm khách hàng với ID: " + id));
    }


}
