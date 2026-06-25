package com.sapo.mock.clothing.statistic.service;

import com.sapo.mock.clothing.statistic.dto.response.DailyStatisticItemResponse;
import com.sapo.mock.clothing.statistic.dto.response.DailyStatisticResponse;

import java.util.List;

public interface StatisticService {
    DailyStatisticResponse getDailyStatistics();

    List<DailyStatisticItemResponse> getWeeklyStatistics();
}
