package com.sapo.mock.clothing.customer.service.impl;


import com.sapo.mock.clothing.customer.dto.request.groupcustomer.CustomerGroupRequest;
import com.sapo.mock.clothing.customer.dto.response.CustomerGroupResponse;
import com.sapo.mock.clothing.customer.dto.response.CustomerResponse;
import com.sapo.mock.clothing.customer.repository.CustomerGroupRepository;
import com.sapo.mock.clothing.customer.repository.CustomerRepository;
import com.sapo.mock.clothing.customer.repository.CustomerVoucherRepository;
import com.sapo.mock.clothing.customer.repository.VoucherRepository;
import com.sapo.mock.clothing.customer.service.CustomerGroupService;
import com.sapo.mock.clothing.entity.Customer;
import com.sapo.mock.clothing.entity.CustomerGroup;
import com.sapo.mock.clothing.entity.CustomerVoucher;
import com.sapo.mock.clothing.entity.Voucher;
import com.sapo.mock.clothing.exception.ResourceNotFoundException;
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

    @Autowired
    private VoucherRepository voucherRepository;



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

    @Override
    @Transactional
    public CustomerGroupResponse createGroup(CustomerGroupRequest request) {
        if (request.getMaxSpending() != null && request.getMinSpending() != null && request.getMinSpending().compareTo(request.getMaxSpending()) >= 0) {
            throw new IllegalArgumentException("Chi tiêu tối đa phải lớn hơn chi tiêu tối thiểu");
        }

        CustomerGroup group = new CustomerGroup();
        group.setName(request.getName());
        group.setDescription(request.getDescription());
        group.setCode(request.getCode());
        group.setMinSpending(request.getMinSpending());
        group.setMaxSpending(request.getMaxSpending());
        group.setNote(request.getNote());
        group.setStatus(request.getStatus() != null ? request.getStatus() : CustomerStatusEnum.ACTIVE);

        // Gán voucher sinh nhật nếu chủ cửa hàng chọn
        if (request.getBirthdayVoucherId() != null) {
            Voucher voucher = voucherRepository.findById(request.getBirthdayVoucherId())
                    .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy Voucher ID: " + request.getBirthdayVoucherId()));
            group.setBirthdayVoucher(voucher);
        } else {
            group.setBirthdayVoucher(null);
        }

        group = customerGroupRepository.save(group);
        return getGroupById(group.getId());
    }

    @Override
    @Transactional
    public CustomerGroupResponse updateGroup(Integer id, CustomerGroupRequest request) {
        if (request.getMaxSpending() != null && request.getMinSpending() != null && request.getMinSpending().compareTo(request.getMaxSpending()) >= 0) {
            throw new IllegalArgumentException("Chi tiêu tối đa phải lớn hơn chi tiêu tối thiểu");
        }

        CustomerGroup group = customerGroupRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy nhóm khách hàng ID: " + id));

        group.setName(request.getName());
        group.setDescription(request.getDescription());
        group.setCode(request.getCode());
        group.setMinSpending(request.getMinSpending());
        group.setMaxSpending(request.getMaxSpending());
        group.setNote(request.getNote());

        // Cập nhật voucher sinh nhật (có thể xóa bằng cách gửi null)
        if (request.getBirthdayVoucherId() != null) {
            Voucher voucher = voucherRepository.findById(request.getBirthdayVoucherId())
                    .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy Voucher ID: " + request.getBirthdayVoucherId()));
            group.setBirthdayVoucher(voucher);
        } else {
            group.setBirthdayVoucher(null);
        }

        if (request.getStatus() != null) {
            group.setStatus(request.getStatus());
        }

        customerGroupRepository.save(group);
        return getGroupById(group.getId());
    }

    @Override
    @Transactional
    public void deleteGroup(Integer id) {
        CustomerGroup group = customerGroupRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy nhóm khách hàng ID: " + id));

        // Xóa mềm: Đổi trạng thái thành INACTIVE
        group.setStatus(CustomerStatusEnum.INACTIVE);
        customerGroupRepository.save(group);

        // Xóa tận gốc ID nhóm khỏi tất cả khách hàng đang thuộc nhóm này
        customerRepository.removeGroupFromAllCustomers(id);
    }

    @Override
    @Transactional
    public void syncAllCustomerRanks() {
        List<CustomerGroup> activeGroups = customerGroupRepository.findByStatus(CustomerStatusEnum.ACTIVE);
        // Sắp xếp nhóm giảm dần theo minSpending để ưu tiên nhóm cao nhất
        activeGroups.sort((g1, g2) -> g2.getMinSpending().compareTo(g1.getMinSpending()));

        List<Customer> customers = customerRepository.findAll();
        for (Customer customer : customers) {
            java.math.BigDecimal totalSpent = customer.getTotalSpent() != null ? customer.getTotalSpent() : java.math.BigDecimal.ZERO;

            // Bước 1: Tìm nhóm khớp chính xác theo khoảng [minSpending, maxSpending)
            CustomerGroup suitableGroup = null;
            for (CustomerGroup group : activeGroups) {
                if (group.getMinSpending() != null && totalSpent.compareTo(group.getMinSpending()) >= 0) {
                    if (group.getMaxSpending() == null || totalSpent.compareTo(group.getMaxSpending()) < 0) {
                        suitableGroup = group;
                        break;
                    }
                }
            }

            // Bước 2: Nếu không tìm được nhóm chính xác (xảy ra khi nhóm cũ bị xóa tạo ra khoảng trống),
            // fallback sang nhóm có minSpending cao nhất mà khách hàng vẫn đạt được (bỏ qua maxSpending).
            // Ví dụ: Khách tiêu 25tr, nhóm Bạc bị xóa, chỉ còn Đồng (10-20tr) → gán Đồng thay vì để NULL.
            if (suitableGroup == null) {
                for (CustomerGroup group : activeGroups) {
                    if (group.getMinSpending() != null && totalSpent.compareTo(group.getMinSpending()) >= 0) {
                        suitableGroup = group;
                        break;
                    }
                }
            }

            customer.setCustomerGroup(suitableGroup);
        }
        customerRepository.saveAll(customers);
    }    @Override
    public Page<com.sapo.mock.clothing.customer.dto.response.CustomerVoucherHistoryResponse> getVoucherHistory(String keyword, Pageable pageable) {
        String cleanKeyword = (keyword != null && !keyword.trim().isEmpty()) ? keyword.trim() : null;
        return customerVoucherRepository.searchHistory(cleanKeyword, pageable);
    }
}
