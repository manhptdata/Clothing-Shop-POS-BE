package com.sapo.mock.clothing.statistic.service.impl;

import com.sapo.mock.clothing.customer.repository.CustomerRepository;
import com.sapo.mock.clothing.order.repository.OrderRepository;
import com.sapo.mock.clothing.returnorder.repository.ReturnOrderRepository;
import com.sapo.mock.clothing.statistic.dto.response.DailyStatisticItemResponse;
import com.sapo.mock.clothing.statistic.dto.response.DailyStatisticResponse;
import com.sapo.mock.clothing.statistic.service.StatisticService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class StatisticServiceImpl implements StatisticService {

    private final OrderRepository orderRepository;
    private final CustomerRepository customerRepository;
    private final ReturnOrderRepository returnOrderRepository;

    @Override
    @Transactional(readOnly = true)
    public DailyStatisticResponse getDailyStatistics() {
        LocalDate today = LocalDate.now();
        Instant startOfDay = today.atStartOfDay(ZoneId.systemDefault()).toInstant();
        Instant endOfDay = today.plusDays(1).atStartOfDay(ZoneId.systemDefault()).toInstant().minusMillis(1);

        BigDecimal dailyRevenue = orderRepository.calculateRevenueBetween(startOfDay, endOfDay);
        if (dailyRevenue == null) {
            dailyRevenue = BigDecimal.ZERO;
        }

        BigDecimal grossCogs = orderRepository.calculateTotalCogsBetween(startOfDay, endOfDay);
        BigDecimal restockedCogs = returnOrderRepository.calculateRestockedCogsBetween(startOfDay, endOfDay);
        BigDecimal dailyCogs = (grossCogs != null ? grossCogs : BigDecimal.ZERO).subtract(restockedCogs != null ? restockedCogs : BigDecimal.ZERO);
        BigDecimal dailyProfit = dailyRevenue.subtract(dailyCogs);

        long newCustomers = customerRepository.countByCreatedAtBetween(startOfDay, endOfDay);
        long newOrders = orderRepository.countByCreatedAtBetween(startOfDay, endOfDay);

        return new DailyStatisticResponse(dailyRevenue, dailyCogs, dailyProfit, newCustomers, newOrders);
    }

    @Override
    @Transactional(readOnly = true)
    public List<DailyStatisticItemResponse> getWeeklyStatistics() {
        List<DailyStatisticItemResponse> weeklyStats = new ArrayList<>();
        LocalDate today = LocalDate.now();

        // Loop from 6 days ago to today (total 7 days)
        for (int i = 6; i >= 0; i--) {
            LocalDate targetDate = today.minusDays(i);
            Instant startOfDay = targetDate.atStartOfDay(ZoneId.systemDefault()).toInstant();
            Instant endOfDay = targetDate.plusDays(1).atStartOfDay(ZoneId.systemDefault()).toInstant().minusMillis(1);

            BigDecimal revenue = orderRepository.calculateRevenueBetween(startOfDay, endOfDay);
            if (revenue == null) {
                revenue = BigDecimal.ZERO;
            }

            BigDecimal grossCogs = orderRepository.calculateTotalCogsBetween(startOfDay, endOfDay);
            BigDecimal restockedCogs = returnOrderRepository.calculateRestockedCogsBetween(startOfDay, endOfDay);
            BigDecimal cogs = (grossCogs != null ? grossCogs : BigDecimal.ZERO).subtract(restockedCogs != null ? restockedCogs : BigDecimal.ZERO);
            BigDecimal profit = revenue.subtract(cogs);

            long orderCount = orderRepository.countByCreatedAtBetween(startOfDay, endOfDay);

            weeklyStats.add(new DailyStatisticItemResponse(targetDate, revenue, cogs, profit, orderCount));
        }

        return weeklyStats;
    }
}
