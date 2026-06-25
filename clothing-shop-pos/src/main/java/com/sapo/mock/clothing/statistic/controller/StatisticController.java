package com.sapo.mock.clothing.statistic.controller;

import com.sapo.mock.clothing.common.dto.response.RestResponse;
import com.sapo.mock.clothing.statistic.dto.response.DailyStatisticItemResponse;
import com.sapo.mock.clothing.statistic.dto.response.DailyStatisticResponse;
import com.sapo.mock.clothing.statistic.service.StatisticService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/statistics")
@CrossOrigin(origins = "*")
@RequiredArgsConstructor
public class StatisticController {

    private final StatisticService statisticService;

    @GetMapping("/daily")
    public ResponseEntity<RestResponse<DailyStatisticResponse>> getDailyStatistics() {
        DailyStatisticResponse result = statisticService.getDailyStatistics();

        RestResponse<DailyStatisticResponse> response = new RestResponse<>();
        response.setStatusCode(HttpStatus.OK.value());
        response.setError(null);
        response.setMessage("Lấy thống kê hàng ngày thành công");
        response.setData(result);

        return ResponseEntity.ok(response);
    }

    @GetMapping("/weekly")
    public ResponseEntity<RestResponse<List<DailyStatisticItemResponse>>> getWeeklyStatistics() {
        List<DailyStatisticItemResponse> result = statisticService.getWeeklyStatistics();

        RestResponse<List<DailyStatisticItemResponse>> response = new RestResponse<>();
        response.setStatusCode(HttpStatus.OK.value());
        response.setError(null);
        response.setMessage("Lấy thống kê 7 ngày gần nhất thành công");
        response.setData(result);

        return ResponseEntity.ok(response);
    }
}
