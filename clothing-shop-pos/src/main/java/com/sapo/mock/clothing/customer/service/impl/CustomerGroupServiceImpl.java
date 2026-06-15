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


    // Retrieve all customer groups.
    @Override
    public Page<CustomerGroupResponse> getGroupsWithPage(Pageable pageable) {
        return groupRepository.findAllByOrderByIdAsc(pageable).map(this::convertToResponse);
    }

    private CustomerGroupResponse convertToResponse(CustomerGroup group) {
        CustomerGroupResponse res = new CustomerGroupResponse();
        res.setId(group.getId());
        res.setName(group.getName());
        res.setDescription(group.getDescription());
        res.setStatus(group.getStatus());
        res.setNote(group.getNote());
        res.setCreatedAt(group.getCreatedAt());
        return res;
    }


    //  tìm kiếm nhóm ACTIVE
    @Override
    public Page<CustomerGroupResponse> searchGroups(String keyword, Pageable pageable) {
        return groupRepository.searchGroups(keyword, pageable);
    }


    // Lấy chi tiết 1 nhóm
    @Override
    public CustomerGroupResponse getGroupById(Integer id) {
        CustomerGroupResponse response = groupRepository.getGroupDetailById(id);
        if (response == null) {
            throw new RuntimeException("Không tìm thấy nhóm khách hàng có ID: " + id);
        }
        return response;
    }

    @Override
    public CustomerGroupResponse createGroup(CustomerGroupRequest request) {
        // 1. Chuyển đổi từ Request DTO sang Entity để lưu vào DB
        CustomerGroup group = new CustomerGroup();
        group.setName(request.getName().trim());
        group.setDescription(request.getDescription());
        group.setNote(request.getNote());
        // Trường status, createdAt, updatedAt đã được tự động xử lý ở @PrePersist trong Entity rồi nha bạn

        // 2. Lưu xuống database
        CustomerGroup savedGroup = groupRepository.save(group);

        // 3. Chuyển Entity vừa lưu thành Response DTO để trả về cho Client
        return convertToResponse(savedGroup);
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
}
