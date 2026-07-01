package com.sapo.mock.clothing.ai;

import com.sapo.mock.clothing.entity.ProductVariant;
import com.sapo.mock.clothing.product.repository.ProductVariantRepository;
import jakarta.persistence.EntityManager;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.Map;

@Service
public class GeminiAIService {

    @Value("${ai.gemini.api-key}")
    private String apiKey;

    @Value("${ai.gemini.api-url}")
    private String apiUrl;
    @Autowired
    private ProductVariantRepository variantRepository;
    @Autowired
    private com.sapo.mock.clothing.order.repository.OrderLineItemRepository orderLineItemRepository;

    @Autowired
    private EntityManager entityManager;

    private String callGeminiApi(String prompt) {
        RestTemplate restTemplate = new RestTemplate();
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        String escapedPrompt = prompt.replace("\"", "\\\"").replace("\n",
                "\\n").replace("\r", "");

        String requestBody = "{\n" +
                " \"contents\": [{\n" +
                " \"parts\":[{\"text\": \"" + escapedPrompt + "\"}]\n" +
                " }]\n" +
                "}";

        HttpEntity<String> entity = new HttpEntity<>(requestBody, headers);

        try {
            ResponseEntity<Map> response = restTemplate.exchange(
                    apiUrl + "?key=" + apiKey,
                    HttpMethod.POST,
                    entity,
                    Map.class);

            Map<String, Object> body = response.getBody();
            if (body != null && body.containsKey("candidates")) {
                List<Map<String, Object>> candidates = (List<Map<String, Object>>) body.get("candidates");
                if (!candidates.isEmpty()) {
                    Map<String, Object> content = (Map<String, Object>) candidates.get(0).get("content");
                    List<Map<String, Object>> parts = (List<Map<String, Object>>) content.get("parts");
                    return (String) parts.get(0).get("text");
                }
            }
            return "Không thể lấy kết quả từ AI.";

        } catch (Exception e) {
            e.printStackTrace();
            return "Lỗi khi gọi AI: " + e.getMessage();
        }
    }

    public String analyzeInventory(String customPrompt, int days) {
    String schema = "Cấu trúc Database (chỉ sử dụng các cột này):\n" +
    "1. product(id, name, category_id, description)\n" +
    "2. product_variant(id, product_id, sku, quantity, sale_price, import_price)\n" +
    "3. orders(id, created_at, status)\n" +
    "4. order_line_item(id, order_id, product_sku, quantity)\n\n" +
    "QUAN TRỌNG: Chỉ sử dụng các cột đã liệt kê ở trên. Không bịa ra cột mới. Bảng order_line_item dùng product_sku để join với bảng product_variant (ON order_line_item.product_sku = product_variant.sku). Trạng thái đơn hàng thành công là 'COMPLETED'.\n";

    // Bước 1: AI sinh ra SQL
    String sqlPrompt = "Bạn là một AI tạo câu lệnh SQL. Tôi có CSDL như sau:\n" +
    schema +
    "Dựa vào yêu cầu của người dùng: [" + customPrompt + "].\n" +
    "LƯU Ý VỀ THỜI GIAN: Nếu người dùng không chỉ định rõ thời gian trong câu hỏi, hãy mặc định lấy dữ liệu của các đơn hàng trong " + days + " ngày gần nhất (tính từ hôm nay). " +
    "Nếu người dùng có nhắc đến mốc thời gian riêng (ví dụ: tháng trước, năm ngoái, v.v.), hãy ưu tiên dùng mốc thời gian của người dùng.\n" +
    "Hãy tạo 1 câu lệnh MySQL SELECT để lấy dữ liệu. TUYỆT ĐỐI KHÔNG dùng từ khóa LIMIT trong câu lệnh (hệ thống sẽ tự động thêm). TUYỆT ĐỐI CHỈ TRẢ VỀ CÂU LỆNH SQL THUẦN TÚY, không có ký tự markdown, không có chữ sql ở đầu, không giải thích gì thêm.";

    String sqlQuery = callGeminiApi(sqlPrompt).trim();
    sqlQuery = sqlQuery.replace("```sql", "").replace("```", "").trim(); // Remove any accidental markdown

    System.out.println("AI Generated SQL: " + sqlQuery);

    if (sqlQuery.startsWith("Lỗi khi gọi AI") || sqlQuery.startsWith("Không thể lấy kết quả")) {
        return sqlQuery; // Trả về thẳng lỗi API (ví dụ: 429 Too Many Requests) thay vì báo lỗi bảo mật
    }

    // Security check
    if (!sqlQuery.toUpperCase().startsWith("SELECT")) {
        return "Lỗi bảo mật: Câu lệnh do AI sinh ra không hợp lệ hoặc chứa mã độc. Chỉ cho phép lệnh SELECT.\nCâu lệnh bị chặn: " + sqlQuery;
    }

    if (sqlQuery.toUpperCase().matches(".*\\b(INSERT|UPDATE|DELETE|DROP|ALTER|TRUNCATE|GRANT|REVOKE)\\b.*")) {
        return "Lỗi bảo mật: Câu lệnh do AI sinh ra chứa từ khóa cấm.\nCâu lệnh bị chặn: " + sqlQuery;
    }

    // Bước 2: Execute SQL
    List<?> rawData;
    try {
        rawData = entityManager.createNativeQuery(sqlQuery).setMaxResults(50).getResultList();
    } catch (Exception e) {
        e.printStackTrace();
        return "Lỗi khi chạy truy vấn SQL do AI tạo: " + e.getMessage() + "\nSQL: " + sqlQuery;
    }

    // Format Result
    StringBuilder dataBuilder = new StringBuilder();
    if (rawData.isEmpty()) {
    dataBuilder.append("Không có dữ liệu nào khớp với yêu cầu.");
    } else {
    for (Object row : rawData) {
    if (row instanceof Object[]) {
    Object[] arr = (Object[]) row;
    for (int i = 0; i < arr.length; i++) {
    dataBuilder.append(arr[i]).append("\t|\t");
    }
    } else {
    dataBuilder.append(row);
    }
    dataBuilder.append("\n");
    }
    }

    // Bước 3: Phân tích kết quả
    String analysisPrompt = "Bạn là AI chuyên phân tích số liệu.\n" +
    "YÊU CẦU: Trình bày kết quả bằng văn bản thuần túy (plain text) dễ đọc. TUYỆT ĐỐI KHÔNG sử dụng Markdown (không dùng ký tự **, *, #).\n" +
    "Yêu cầu ban đầu của người dùng: [" + customPrompt + "].\n" +
    "THÔNG TIN HỆ THỐNG: Mặc định dữ liệu đang được lọc trong khoảng thời gian " + days + " ngày gần nhất.\n" +
    "Dưới đây là số liệu thực tế rút ra từ hệ thống:\n" + dataBuilder.toString() +
    "\nHãy phân tích và trả lời người dùng dựa trên số liệu này. Nếu không có dữ liệu, hãy giải thích lịch sự rằng không có giao dịch/sản phẩm nào thỏa mãn điều kiện trong thời gian trên.";

    return callGeminiApi(analysisPrompt);
    }
}

// /*
// // --- ĐÂY LÀ ĐOẠN CODE CŨ ĐƯỢC COMMENT LẠI THEO YÊU CẦU CỦA BẠN ---

// public String analyzeInventoryOld(String customPrompt, int days) {
// Page<ProductVariant> page = variantRepository.findAll(PageRequest.of(0,
// 100));
// List<ProductVariant> variants = page.getContent();

// // 1. Tính tổng lượng bán của từng sản phẩm trong N ngày qua
// java.time.Instant cutoffDate =
// java.time.Instant.now().minus(java.time.Duration.ofDays(days));
// List<Object[]> soldData =
// orderLineItemRepository.getVariantSoldQuantitySinceBySku(cutoffDate);
// Map<String, Integer> soldMap = new java.util.HashMap<>();
// for (Object[] row : soldData) {
// String sku = (String) row[0];
// Integer soldQty = ((Number) row[1]).intValue();
// soldMap.put(sku, soldQty);
// }

// // 2. Gộp cả Tồn kho và Số lượng đã bán gửi cho AI
// StringBuilder dataBuilder = new StringBuilder();
// for (ProductVariant v : variants) {
// String productName = v.getProduct() != null ? v.getProduct().getName() :
// "Unknown";
// String category = (v.getProduct() != null && v.getProduct().getCategory() !=
// null)
// ? v.getProduct().getCategory().getName()
// : "Khác";

// // Lấy lượng đã bán từ Map, nếu không có thì mặc định là 0
// int soldQuantity = soldMap.getOrDefault(v.getSku(), 0);

// dataBuilder.append("- Danh mục: ").append(category)
// .append(" | SP: ").append(productName)
// .append(" | SKU: ").append(v.getSku())
// .append(" | Tồn kho (còn lại): ").append(v.getQuantity())
// .append(" | Đã bán (").append(days).append(" ngày
// qua):").append(soldQuantity)
// .append(" | Giá bán: ").append(v.getSalePrice())
// .append("\n");
// }

// String data = dataBuilder.toString();

// String prompt = "Bạn là AI chuyên phân tích kho hàng.\n"
// + "YÊU CẦU QUAN TRỌNG: Trình bày kết quả bằng văn bản thuần túy (plain text)
// dễ đọc. TUYỆT ĐỐI KHÔNG sử dụng Markdown (không dùng ký tự **, *, #). Dùng
// dấu gạch ngang (-) để làm danh sách.\n"
// + "Yêu cầu của người dùng: [" + customPrompt + "].\n"
// + "Dữ liệu kho (tối đa 100 sản phẩm):\n" + data;

// RestTemplate restTemplate = new RestTemplate();
// HttpHeaders headers = new HttpHeaders();
// headers.setContentType(MediaType.APPLICATION_JSON);

// String escapedPrompt = prompt.replace("\"", "\\\"").replace("\n",
// "\\n").replace("\r", "");

// String requestBody = "{\n" +
// " \"contents\": [{\n" +
// " \"parts\":[{\"text\": \"" + escapedPrompt + "\"}]\n" +
// " }]\n" +
// "}";

// HttpEntity<String> entity = new HttpEntity<>(requestBody, headers);

// try {
// ResponseEntity<Map> response = restTemplate.exchange(
// apiUrl + "?key=" + apiKey,
// HttpMethod.POST,
// entity,
// Map.class);

// Map<String, Object> body = response.getBody();
// if (body != null && body.containsKey("candidates")) {
// List<Map<String, Object>> candidates = (List<Map<String, Object>>)
// body.get("candidates");
// if (!candidates.isEmpty()) {
// Map<String, Object> content = (Map<String, Object>)
// candidates.get(0).get("content");
// List<Map<String, Object>> parts = (List<Map<String, Object>>)
// content.get("parts");
// return (String) parts.get(0).get("text");
// }
// }
// return "Không thể lấy kết quả phân tích từ AI.";

// } catch (Exception e) {
// e.printStackTrace();
// return "Lỗi khi gọi AI: " + e.getMessage();
// }
// }
// }
