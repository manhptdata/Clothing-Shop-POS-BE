package com.sapo.mock.clothing.statistic.service;

import com.sapo.mock.clothing.statistic.dto.response.DailyStatisticItemResponse;
import com.sapo.mock.clothing.statistic.dto.response.DailyStatisticResponse;

import java.util.List;

public interface StatisticService {
    DailyStatisticResponse getDailyStatistics();

    List<DailyStatisticItemResponse> getWeeklyStatistics();

    DailyStatisticResponse getPeriodStatistics(String period);

    DailyStatisticResponse getPeriodStatistics(String period, String startDate, String endDate);

    List<DailyStatisticItemResponse> getChartStatistics(String period);

    List<DailyStatisticItemResponse> getChartStatistics(String period, String startDate, String endDate);
}
