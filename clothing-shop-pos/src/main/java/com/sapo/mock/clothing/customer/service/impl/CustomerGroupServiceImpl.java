package com.sapo.mock.clothing.customer.service.impl;


import com.sapo.mock.clothing.customer.dto.request.groupcustomer.CustomerGroupRequest;
import com.sapo.mock.clothing.customer.dto.request.groupcustomer.CustomerRequest;
import com.sapo.mock.clothing.customer.dto.response.CustomerGroupResponse;
import com.sapo.mock.clothing.customer.dto.response.CustomerResponse;
import com.sapo.mock.clothing.customer.repository.CustomerGroupRepository;
import com.sapo.mock.clothing.customer.repository.CustomerRepository;
import com.sapo.mock.clothing.customer.service.CustomerGroupService;
import com.sapo.mock.clothing.entity.Customer;
import com.sapo.mock.clothing.entity.CustomerGroup;
import com.sapo.mock.clothing.util.constant.CustomerStatusEnum;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CustomerGroupServiceImpl implements CustomerGroupService {

    @Autowired
    private CustomerGroupRepository groupRepository;

    @Autowired
    private CustomerRepository customerRepository;

    @Autowired
    private CustomerGroupRepository customerGroupRepository;


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
        CustomerGroup group = new CustomerGroup();
        group.setName(request.getName());
        group.setDescription(request.getDescription());
        group.setStatus(request.getStatus());
        group.setNote(request.getNote());
        group.setMinSpending(request.getMinSpending());
        group.setMaxSpending(request.getMaxSpending());

        group.setCode(request.getCode());
        group = customerGroupRepository.save(group);
        return convertToResponses(group);
    }

    // Hàm Helper chuyển dữ liệu từ Entity sang Response DTO của riêng API này
    private CustomerGroupResponse convertToResponses(CustomerGroup group) {
        CustomerGroupResponse res = new CustomerGroupResponse();
        res.setId(group.getId());
        res.setName(group.getName());
        res.setDescription(group.getDescription());
        res.setStatus(group.getStatus());
        res.setNote(group.getNote());
        res.setMinSpending(group.getMinSpending());
        res.setMaxSpending(group.getMaxSpending());

        res.setCode(group.getCode());

        res.setTotalCustomers(0L);
        res.setCreatedAt(group.getCreatedAt());
        return res;
    }

    // create nhóm khách hàng mới
    @Override
    @Transactional
    public CustomerResponse assignGroupToCustomer(Integer customerId, CustomerRequest request) {
        // 1. Kiểm tra khách hàng có tồn tại không
        Customer customer = customerRepository.findById(customerId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy khách hàng có ID: " + customerId));

        // 2. Map dữ liệu cơ bản từ DTO sang Entity (Các trường Enum nhận trực tiếp cực sạch)
        customer.setFullName(request.getFullName().trim());
        customer.setPhone(request.getPhone().trim());
        customer.setDateOfBirth(request.getDateOfBirth());
        customer.setAddress(request.getAddress());
        customer.setNote(request.getNote());
        customer.setGender(request.getGender()); // Nhận thẳng GenderEnum
        customer.setStatus(request.getStatus()); // Nhận thẳng CustomerStatusEnum

        // 3. XỬ LÝ GÁN NHÓM (Khóa ngoại group_id)
        if (request.getCustomerGroupId() != null) {
            CustomerGroup group = groupRepository.findById(request.getCustomerGroupId())
                    .orElseThrow(() -> new RuntimeException("Nhóm khách hàng có ID " + request.getCustomerGroupId() + " không tồn tại"));
            customer.setCustomerGroup(group); // Gán thực thể nhóm vào bảng khách
        } else {
            customer.setCustomerGroup(null); // Nếu truyền null nghĩa là rút khách hàng ra khỏi nhóm
        }

        // 4. Lưu cập nhật xuống MySQL
        Customer updatedCustomer = customerRepository.save(customer);

        // 5. Chuyển đổi thành Response DTO trả về cho Controller
        return convertToResponse(updatedCustomer);
    }

    // Hàm phụ trợ convert từ Entity sang Response DTO gọn gàng
    private CustomerResponse convertToResponse(Customer customer) {
        CustomerResponse res = new CustomerResponse();
        res.setId(customer.getId());
        res.setFullName(customer.getFullName());
        res.setPhone(customer.getPhone());
        res.setDateOfBirth(customer.getDateOfBirth());
        res.setGender(customer.getGender());
        res.setAddress(customer.getAddress());
        res.setNote(customer.getNote());
        res.setStatus(customer.getStatus());
        res.setCreatedAt(customer.getCreatedAt());

        // Bọc thông tin nhóm đi kèm để Frontend lấy được tên nhóm hiển thị lên màn hình
        if (customer.getCustomerGroup() != null) {
            CustomerResponse.GroupInfo groupInfo = new CustomerResponse.GroupInfo();
            groupInfo.setId(customer.getCustomerGroup().getId());
            groupInfo.setName(customer.getCustomerGroup().getName());
            groupInfo.setCode(customer.getCustomerGroup().getCode());
            res.setCustomerGroup(groupInfo);
        }
        return res;
    }

    // Chỉ gán/đổi/rút nhóm nhanh — không cần gửi lại toàn bộ thông tin khách hàng
    @Override
    @Transactional
    public CustomerResponse assignOnlyGroup(Integer customerId, Integer customerGroupId) {
        // 1. Kiểm tra khách hàng có tồn tại không
        Customer customer = customerRepository.findById(customerId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy khách hàng có ID: " + customerId));

        // 2. Gán hoặc rút nhóm
        if (customerGroupId != null) {
            CustomerGroup group = groupRepository.findById(customerGroupId)
                    .orElseThrow(() -> new RuntimeException("Nhóm khách hàng có ID " + customerGroupId + " không tồn tại"));
            customer.setCustomerGroup(group);
        } else {
            customer.setCustomerGroup(null); // null = rút khỏi nhóm
        }

        // 3. Lưu và trả về
        Customer updated = customerRepository.save(customer);
        return convertToResponse(updated);
    }

    @Override
    @Transactional
    public void updateCustomerGroup(Integer id, CustomerGroupRequest request) {
        CustomerGroup group = customerGroupRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy nhóm khách hàng với ID: " + id));

        group.setName(request.getName());
        group.setDescription(request.getDescription());
        group.setStatus(request.getStatus());
        group.setNote(request.getNote());

        group.setMinSpending(request.getMinSpending());
        group.setMaxSpending(request.getMaxSpending());

        customerGroupRepository.save(group);
    }
    @Override
    @Transactional
    public void softDeleteCustomerGroup(Integer id) {
        CustomerGroup group = customerGroupRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy nhóm khách hàng với ID: " + id));

        group.setStatus(CustomerStatusEnum.INACTIVE);

        customerGroupRepository.save(group);
    }
}
