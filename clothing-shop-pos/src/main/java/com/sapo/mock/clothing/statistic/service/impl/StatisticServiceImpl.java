package com.sapo.mock.clothing.statistic.service.impl;

import com.sapo.mock.clothing.customer.repository.CustomerRepository;
import com.sapo.mock.clothing.entity.ProductVariant;
import com.sapo.mock.clothing.order.repository.OrderLineItemRepository;
import com.sapo.mock.clothing.order.repository.OrderRepository;
import com.sapo.mock.clothing.product.repository.ProductVariantRepository;
import com.sapo.mock.clothing.returnorder.repository.ReturnOrderRepository;
import com.sapo.mock.clothing.statistic.dto.response.DailyStatisticItemResponse;
import com.sapo.mock.clothing.statistic.dto.response.DailyStatisticResponse;
import com.sapo.mock.clothing.statistic.dto.response.LowStockProductDTO;
import com.sapo.mock.clothing.statistic.dto.response.TopProductStatisticDTO;
import com.sapo.mock.clothing.statistic.service.StatisticService;
import com.sapo.mock.clothing.util.constant.PaymentMethod;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class StatisticServiceImpl implements StatisticService {

    private final OrderRepository orderRepository;
    private final CustomerRepository customerRepository;
    private final ReturnOrderRepository returnOrderRepository;
    private final OrderLineItemRepository orderLineItemRepository;
    private final ProductVariantRepository productVariantRepository;

    @Override
    @Transactional(readOnly = true)
    public DailyStatisticResponse getDailyStatistics() {
        return getPeriodStatistics("today");
    }

    @Override
    @Transactional(readOnly = true)
    public List<DailyStatisticItemResponse> getWeeklyStatistics() {
        List<DailyStatisticItemResponse> weeklyStats = new ArrayList<>();
        LocalDate today = LocalDate.now();

        // Loop from 6 days ago to today (total 7 days)
        for (int i = 6; i >= 0; i--) {
            LocalDate targetDate = today.minusDays(i);
            addDailyItem(weeklyStats, targetDate);
        }

        return weeklyStats;
    }

    @Override
    @Transactional(readOnly = true)
    public DailyStatisticResponse getPeriodStatistics(String period) {
        return getPeriodStatistics(period, null, null);
    }

    @Override
    @Transactional(readOnly = true)
    public DailyStatisticResponse getPeriodStatistics(String period, String startDateStr, String endDateStr) {
        LocalDate today = LocalDate.now();
        Instant start;
        Instant end;

        if ("custom".equalsIgnoreCase(period) && startDateStr != null && !startDateStr.isEmpty() && endDateStr != null && !endDateStr.isEmpty()) {
            LocalDate sDate = LocalDate.parse(startDateStr);
            LocalDate eDate = LocalDate.parse(endDateStr);
            start = sDate.atStartOfDay(ZoneId.systemDefault()).toInstant();
            end = eDate.plusDays(1).atStartOfDay(ZoneId.systemDefault()).toInstant().minusMillis(1);
        } else if ("week".equalsIgnoreCase(period)) {
            LocalDate monday = today.minusDays(today.getDayOfWeek().getValue() - 1);
            start = monday.atStartOfDay(ZoneId.systemDefault()).toInstant();
            end = today.plusDays(1).atStartOfDay(ZoneId.systemDefault()).toInstant().minusMillis(1);
        } else if ("month".equalsIgnoreCase(period)) {
            LocalDate firstDayOfMonth = today.withDayOfMonth(1);
            start = firstDayOfMonth.atStartOfDay(ZoneId.systemDefault()).toInstant();
            end = today.plusDays(1).atStartOfDay(ZoneId.systemDefault()).toInstant().minusMillis(1);
        } else if ("year".equalsIgnoreCase(period)) {
            LocalDate firstDayOfYear = today.withDayOfYear(1);
            start = firstDayOfYear.atStartOfDay(ZoneId.systemDefault()).toInstant();
            end = today.plusDays(1).atStartOfDay(ZoneId.systemDefault()).toInstant().minusMillis(1);
        } else {
            start = today.atStartOfDay(ZoneId.systemDefault()).toInstant();
            end = today.plusDays(1).atStartOfDay(ZoneId.systemDefault()).toInstant().minusMillis(1);
        }

        BigDecimal revenue = orderRepository.calculateRevenueBetween(start, end);
        if (revenue == null) {
            revenue = BigDecimal.ZERO;
        }

        BigDecimal grossCogs = orderRepository.calculateTotalCogsBetween(start, end);
        BigDecimal restockedCogs = returnOrderRepository.calculateRestockedCogsBetween(start, end);
        BigDecimal cogs = (grossCogs != null ? grossCogs : BigDecimal.ZERO).subtract(restockedCogs != null ? restockedCogs : BigDecimal.ZERO);
        BigDecimal profit = revenue.subtract(cogs);

        long newCustomers = customerRepository.countByCreatedAtBetween(start, end);
        long newOrders = orderRepository.countByCreatedAtBetween(start, end);

        // 1. Tính AOV (Average Order Value)
        BigDecimal averageOrderValue = BigDecimal.ZERO;
        if (newOrders > 0) {
            averageOrderValue = revenue.divide(BigDecimal.valueOf(newOrders), 0, RoundingMode.HALF_UP);
        }

        // 2. Lấy Top 5 sản phẩm bán chạy nhất
        List<Object[]> topProductRaw = orderLineItemRepository.findTopProductsBetween(start, end, PageRequest.of(0, 5));
        List<TopProductStatisticDTO> topProducts = new ArrayList<>();
        for (Object[] row : topProductRaw) {
            String name = (String) row[0];
            String sku = (String) row[1];
            long qty = ((Number) row[2]).longValue();
            BigDecimal totalRev = (BigDecimal) row[3];
            topProducts.add(new TopProductStatisticDTO(name, sku, qty, totalRev));
        }

        // 3. Lấy Top 5 sản phẩm sắp hết hàng (tồn kho <= threshold)
        List<ProductVariant> lowStockVariants = productVariantRepository.findLowStockVariants(PageRequest.of(0, 5));
        List<LowStockProductDTO> lowStockProducts = new ArrayList<>();
        for (ProductVariant v : lowStockVariants) {
            String pName = v.getProduct() != null ? v.getProduct().getName() : "Sản phẩm";
            lowStockProducts.add(new LowStockProductDTO(
                    v.getId(),
                    pName,
                    v.getSku(),
                    v.getQuantity(),
                    v.getLowStockThreshold()
            ));
        }

        // 4. Phân tích phương thức thanh toán
        List<Object[]> paymentRaw = orderRepository.calculatePaymentMethodBreakdownBetween(start, end);
        Map<String, BigDecimal> paymentMethodBreakdown = new HashMap<>();
        for (Object[] row : paymentRaw) {
            PaymentMethod method = (PaymentMethod) row[0];
            BigDecimal amount = (BigDecimal) row[1];
            if (method != null && amount != null) {
                paymentMethodBreakdown.put(method.name(), amount);
            }
        }

        return DailyStatisticResponse.builder()
                .dailyRevenue(revenue)
                .dailyCogs(cogs)
                .dailyProfit(profit)
                .newCustomers(newCustomers)
                .newOrders(newOrders)
                .averageOrderValue(averageOrderValue)
                .topProducts(topProducts)
                .lowStockProducts(lowStockProducts)
                .paymentMethodBreakdown(paymentMethodBreakdown)
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public List<DailyStatisticItemResponse> getChartStatistics(String period) {
        return getChartStatistics(period, null, null);
    }

    @Override
    @Transactional(readOnly = true)
    public List<DailyStatisticItemResponse> getChartStatistics(String period, String startDateStr, String endDateStr) {
        List<DailyStatisticItemResponse> chartStats = new ArrayList<>();
        LocalDate today = LocalDate.now();

        if ("custom".equalsIgnoreCase(period) && startDateStr != null && !startDateStr.isEmpty() && endDateStr != null && !endDateStr.isEmpty()) {
            LocalDate sDate = LocalDate.parse(startDateStr);
            LocalDate eDate = LocalDate.parse(endDateStr);
            LocalDate curr = sDate;
            while (!curr.isAfter(eDate)) {
                addDailyItem(chartStats, curr);
                curr = curr.plusDays(1);
            }
        } else if ("week".equalsIgnoreCase(period)) {
            LocalDate monday = today.minusDays(today.getDayOfWeek().getValue() - 1);
            for (int i = 0; i < 7; i++) {
                LocalDate targetDate = monday.plusDays(i);
                addDailyItem(chartStats, targetDate);
            }
        } else if ("month".equalsIgnoreCase(period)) {
            int daysInMonth = today.lengthOfMonth();
            LocalDate firstDay = today.withDayOfMonth(1);
            for (int i = 0; i < daysInMonth; i++) {
                LocalDate targetDate = firstDay.plusDays(i);
                if (targetDate.isAfter(today)) break;
                addDailyItem(chartStats, targetDate);
            }
        } else if ("year".equalsIgnoreCase(period)) {
            for (int month = 1; month <= today.getMonthValue(); month++) {
                LocalDate firstDayOfMonth = LocalDate.of(today.getYear(), month, 1);
                LocalDate lastDayOfMonth = firstDayOfMonth.plusMonths(1).minusDays(1);
                Instant start = firstDayOfMonth.atStartOfDay(ZoneId.systemDefault()).toInstant();
                Instant end = lastDayOfMonth.plusDays(1).atStartOfDay(ZoneId.systemDefault()).toInstant().minusMillis(1);

                BigDecimal revenue = orderRepository.calculateRevenueBetween(start, end);
                if (revenue == null) revenue = BigDecimal.ZERO;

                BigDecimal grossCogs = orderRepository.calculateTotalCogsBetween(start, end);
                BigDecimal restockedCogs = returnOrderRepository.calculateRestockedCogsBetween(start, end);
                BigDecimal cogs = (grossCogs != null ? grossCogs : BigDecimal.ZERO).subtract(restockedCogs != null ? restockedCogs : BigDecimal.ZERO);
                BigDecimal profit = revenue.subtract(cogs);

                long orderCount = orderRepository.countByCreatedAtBetween(start, end);

                chartStats.add(new DailyStatisticItemResponse(firstDayOfMonth, revenue, cogs, profit, orderCount));
            }
        } else {
            return getWeeklyStatistics();
        }

        return chartStats;
    }

    private void addDailyItem(List<DailyStatisticItemResponse> list, LocalDate targetDate) {
        Instant startOfDay = targetDate.atStartOfDay(ZoneId.systemDefault()).toInstant();
        Instant endOfDay = targetDate.plusDays(1).atStartOfDay(ZoneId.systemDefault()).toInstant().minusMillis(1);

        BigDecimal revenue = orderRepository.calculateRevenueBetween(startOfDay, endOfDay);
        if (revenue == null) revenue = BigDecimal.ZERO;

        BigDecimal grossCogs = orderRepository.calculateTotalCogsBetween(startOfDay, endOfDay);
        BigDecimal restockedCogs = returnOrderRepository.calculateRestockedCogsBetween(startOfDay, endOfDay);
        BigDecimal cogs = (grossCogs != null ? grossCogs : BigDecimal.ZERO).subtract(restockedCogs != null ? restockedCogs : BigDecimal.ZERO);
        BigDecimal profit = revenue.subtract(cogs);

        long orderCount = orderRepository.countByCreatedAtBetween(startOfDay, endOfDay);

        list.add(new DailyStatisticItemResponse(targetDate, revenue, cogs, profit, orderCount));
    }
}
