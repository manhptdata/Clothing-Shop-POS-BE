package com.sapo.mock.clothing.customer.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sapo.mock.clothing.customer.dto.response.AiResultDto;
import com.sapo.mock.clothing.customer.service.AiAnalysisService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class AiAnalysisServiceImpl implements AiAnalysisService {

    @Value("${ai.gemini.api-key}")
    private String apiKey;

    @Value("${ai.gemini.api-url}")
    private String apiUrl;

    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(java.time.Duration.ofSeconds(10))
            .build();
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public AiResultDto analyzeNote(String note) {
        AiResultDto fallback = new AiResultDto();
        fallback.setResult("KHONG_XAC_DINH");
        fallback.setPotentialStatus("KHONG_XAC_DINH");

        if (note == null || note.trim().isEmpty()) {
            return fallback;
        }

        try {
            String url = apiUrl;

            java.time.ZoneId vnZone = java.time.ZoneId.of("Asia/Ho_Chi_Minh");
            java.time.ZonedDateTime vnTime = java.time.ZonedDateTime.now(vnZone);
            String currentDateTimeVn = vnTime.format(java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ssXXX"));
            
            String systemInstruction = "Bạn là hệ thống CRM phân tích cuộc gọi cho cửa hàng quần áo Sapo.\n"
                    + "Nhiệm vụ của bạn là đọc ghi chú (note) cuộc gọi và phân loại thành 3 thông tin:\n"
                    + "1. result: Thuộc 1 trong các giá trị: GOI_NHO, TU_CHOI, NGHE_MAY, HEN_GOI_LAI.\n" 
                    + "2. potential_status: Phân loại thái độ khách hàng, bắt buộc trả về đúng 1 trong 3 giá trị sau: 'TIEM_NANG' (quan tâm, hỏi giá, muốn xem đồ, chốt đơn), 'KHONG_TIEM_NANG' (từ chối, sai số, gắt gỏng, thuê bao), 'MONG_LUNG' (khách nghe máy nhưng lấp lửng, chưa rõ ràng).\n"
                    + "3. nextRetryTime: Nếu khách hẹn gọi lại (ví dụ chiều nay, mai lúc 3h), hãy ước lượng thời gian gọi lại theo chuẩn ISO-8601 múi giờ UTC (ví dụ 2026-06-28T08:00:00Z). Giờ hiện tại của nhân viên đang là " + currentDateTimeVn + " (Múi giờ Việt Nam UTC+7). Lưu ý: Khi tính toán xong giờ Việt Nam, PHẢI đổi ngược sang UTC (trừ đi 7 tiếng) để trả về hệ thống. Nếu không có hẹn, trả về null.\n"
                    + "Yêu cầu bắt buộc trả về ĐÚNG định dạng JSON cấu trúc sau, không giải thích gì thêm:\n"
                    + "{\"result\": \"chuỗi_kết_quả\", \"potential_status\": \"TIEM_NANG_hoac_MONG_LUNG\", \"nextRetryTime\": \"chuỗi_ISO8601_hoac_null\"}\n\n"
                    + "Nội dung ghi chú cần phân tích: " + note;

            // Dùng Map đóng gói tự động bằng ObjectMapper để chống vỡ định dạng JSON do dấu ngoặc kép
            Map<String, Object> textPart = new HashMap<>();
            textPart.put("text", systemInstruction);

            Map<String, Object> parts = new HashMap<>();
            parts.put("parts", List.of(textPart));

            Map<String, Object> contents = new HashMap<>();
            contents.put("contents", List.of(parts));

            String jsonPayload = objectMapper.writeValueAsString(contents);

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .header("Content-Type", "application/json")
                    .header("X-goog-api-key", apiKey)
                    .timeout(java.time.Duration.ofSeconds(15))
                    .POST(HttpRequest.BodyPublishers.ofString(jsonPayload, java.nio.charset.StandardCharsets.UTF_8))
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() != 200 || response.body() == null) {
                System.err.println("Gemini API Error Body: " + response.body());
                return fallback;
            }

            String responseBodyStr = response.body();
            Map<String, Object> responseMap = objectMapper.readValue(responseBodyStr, Map.class);

            List<Map<String, Object>> candidates = (List<Map<String, Object>>) responseMap.get("candidates");
            if (candidates != null && !candidates.isEmpty()) {
                Map<String, Object> content = (Map<String, Object>) candidates.get(0).get("content");
                List<Map<String, Object>> resParts = (List<Map<String, Object>>) content.get("parts");
                if (resParts != null && !resParts.isEmpty()) {
                    String aiJsonText = (String) resParts.get(0).get("text");

                    if (aiJsonText != null) {
                        aiJsonText = aiJsonText.replace("```json", "")
                                .replace("```", "")
                                .trim();

                        Map<String, Object> aiData = objectMapper.readValue(aiJsonText, Map.class);
                        AiResultDto resultDto = new AiResultDto();
                        resultDto.setResult((String) aiData.get("result"));
                        resultDto.setPotentialStatus((String) aiData.get("potential_status"));

                        Object retryTimeObj = aiData.get("nextRetryTime");
                        if (retryTimeObj != null && retryTimeObj instanceof String) {
                            resultDto.setNextRetryTime((String) retryTimeObj);
                        }

                        return resultDto;
                    }
                }
            }
        } catch (IOException | InterruptedException e) {
            System.err.println("Lỗi xử lý gọi AI bằng HttpClient: " + e.getMessage());
            if (e instanceof InterruptedException) {
                Thread.currentThread().interrupt();
            }
        }
        return fallback;
    }
}