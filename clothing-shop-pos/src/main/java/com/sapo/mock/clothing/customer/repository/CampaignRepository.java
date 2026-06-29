package com.sapo.mock.clothing.customer.repository;

import com.sapo.mock.clothing.entity.Customer;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.time.Instant;

@Repository
public interface CampaignRepository extends JpaRepository<Customer, Integer> {

    /**
     * Quét danh sách khách hàng ACTIVE có hóa đơn COMPLETED
     * khớp trong khoảng mốc thời gian của ngày cách đây đúng 7 ngày.
     */
    @Query("SELECT DISTINCT c FROM Customer c " +
            "JOIN Order o ON c.id = o.customerId " +
            "WHERE o.status = com.sapo.mock.clothing.util.constant.OrderStatus.COMPLETED " +
            "AND o.createdAt <= :endTime " +
            "AND o.createdAt >= :thirtyDaysAgo " +
            "AND c.status = com.sapo.mock.clothing.util.constant.CustomerStatusEnum.ACTIVE " +
            "AND c.id NOT IN (" +
            "    SELECT cl.customer.id FROM CareLog cl " +
            "    WHERE cl.campaign.type = 'AFTER_7_DAYS' " +
            "    AND cl.calledAt >= o.createdAt" +
            ")")
    Page<Customer> findCustomersAfter7DaysBuy(
            @Param("endTime") Instant endTime,
            @Param("thirtyDaysAgo") Instant thirtyDaysAgo,
            Pageable pageable
    );

    /**
     * 2. Chiến dịch LONG_TIME_NO_BUY
     * Tìm khách hàng ACTIVE đã quá 30 ngày chưa có đơn hàng COMPLETED nào,
     * hoặc những khách hàng chưa từng phát sinh đơn hàng nào.
     */
    @Query("SELECT c FROM Customer c " +
            "WHERE c.status = com.sapo.mock.clothing.util.constant.CustomerStatusEnum.ACTIVE " +
            "AND c.createdAt <= :thirtyDaysAgo " +
            "AND c.id NOT IN (" +
            "    SELECT DISTINCT o.customerId FROM Order o " +
            "    WHERE o.status = com.sapo.mock.clothing.util.constant.OrderStatus.COMPLETED " +
            "    AND o.createdAt >= :thirtyDaysAgo" +
            ") " +
            "AND c.id NOT IN (" +
            "    SELECT cl.customer.id FROM CareLog cl " +
            "    WHERE cl.campaign.type = 'LONG_TIME_NO_BUY' " +
            "    AND cl.calledAt >= :todayStart" +
            ")")
    Page<Customer> findCustomersLongTimeNoBuy(
            @Param("thirtyDaysAgo") Instant thirtyDaysAgo,
            @Param("todayStart") Instant todayStart, // Thêm mốc đầu ngày hôm nay để chặn trùng
            Pageable pageable
    );
    /**
     * 3. Chiến dịch RECALL_SCHEDULE
     * Tìm khách hàng ACTIVE có lịch hẹn gọi lại nằm trong ngày hôm nay.
     */
    @Query("SELECT DISTINCT c FROM Customer c " +
            "JOIN CareLog log ON c = log.customer " +
            "WHERE log.nextRetryAt >= :startTime AND log.nextRetryAt <= :endTime " +
            "AND c.status = com.sapo.mock.clothing.util.constant.CustomerStatusEnum.ACTIVE " +
            "AND c.id NOT IN (" +
            "    SELECT cl.customer.id FROM CareLog cl " +
            "    WHERE cl.campaign.type = 'RECALL_SCHEDULE' " +
            "    AND cl.calledAt >= :startTime" +
            ")")
    Page<Customer> findCustomersRecallSchedule(
            @Param("startTime") Instant startTime,
            @Param("endTime") Instant endTime,
            Pageable pageable
    );

    /**
     * 4. Chiến dịch HAPPY_BIRTHDAY
     * Quét danh sách khách hàng ACTIVE có tháng sinh nhật trùng với tháng truyền vào.
     */
    @Query("SELECT c FROM Customer c " +
            "WHERE c.status = com.sapo.mock.clothing.util.constant.CustomerStatusEnum.ACTIVE " +
            "AND FUNCTION('MONTH', c.dateOfBirth) = :currentMonth " +
            "AND c.id NOT IN (" +
            "    SELECT cl.customer.id FROM CareLog cl " +
            "    WHERE cl.campaign.type = 'HAPPY_BIRTHDAY' " +
            "    AND cl.calledAt >= :todayStart" +
            ")")
    Page<Customer> findCustomersByBirthdayMonth(
            @Param("currentMonth") int currentMonth,
            @Param("todayStart") Instant todayStart, // Thêm mốc đầu ngày hôm nay để chặn trùng
            Pageable pageable
    );
}