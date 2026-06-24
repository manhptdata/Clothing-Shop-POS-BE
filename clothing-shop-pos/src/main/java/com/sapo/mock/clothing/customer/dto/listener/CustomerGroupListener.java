package com.sapo.mock.clothing.customer.dto.listener;


import com.sapo.mock.clothing.customer.dto.event.OrderCompletedEvent;
import com.sapo.mock.clothing.customer.repository.CustomerGroupRepository;
import com.sapo.mock.clothing.customer.repository.CustomerRepository;
import com.sapo.mock.clothing.entity.Customer;
import com.sapo.mock.clothing.entity.CustomerGroup;
import com.sapo.mock.clothing.util.constant.CustomerStatusEnum;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

@Component
public class CustomerGroupListener {

    @Autowired
    private CustomerRepository customerRepository;

    @Autowired
    private CustomerGroupRepository customerGroupRepository;

    /**
     * Tự động bắt gói tin sự kiện hoàn thành đơn để cập nhật doanh số và hạng thành viên
     */
    @EventListener
    @Transactional
    public void handleOrderCompletedEvent(OrderCompletedEvent event) {
        if (event.getCustomerId() == null || event.getCustomerId() == 1 || event.getOrderAmount() == null) return;

        // 1. Tìm thông tin khách hàng từ DB
        Customer customer = customerRepository.findById(event.getCustomerId())
                .orElseThrow(() -> new RuntimeException("Không tìm thấy khách hàng khi xử lý nâng hạng tự động"));

        // 2. Cộng dồn doanh số mua hàng mới
        BigDecimal currentSpent = customer.getTotalSpent() != null ? customer.getTotalSpent() : BigDecimal.ZERO;
        BigDecimal newTotalSpent = currentSpent.add(event.getOrderAmount());
        customer.setTotalSpent(newTotalSpent);

        // Gọi hàm bốc về List và truyền kèm trạng thái CustomerStatusEnum.ACTIVE
        List<CustomerGroup> suitableGroups = customerGroupRepository.findSuitableGroup(
                newTotalSpent,
                CustomerStatusEnum.ACTIVE
        );

        // Bốc ra phần tử đầu tiên tìm được (Hạng cao nhất thỏa mãn nhờ lệnh ORDER BY ở Repo), nếu List rỗng thì trả về null
        CustomerGroup suitableGroup = suitableGroups.isEmpty() ? null : suitableGroups.get(0);

        // 4. Cập nhật nhóm mới tự động
        customer.setCustomerGroup(suitableGroup);
        customerRepository.saveAndFlush(customer);
    }
}