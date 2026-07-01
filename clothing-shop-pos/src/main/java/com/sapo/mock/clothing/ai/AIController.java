package com.sapo.mock.clothing.ai;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/ai")
@CrossOrigin("*")
public class AIController {

    @Autowired
    private GeminiAIService geminiService;

    @PostMapping("/inventory-report")
    public ResponseEntity<String> getInventoryReport(@RequestBody AIRequest request) {
        String prompt = request.getPrompt();
        if (prompt == null || prompt.trim().isEmpty()) {
            prompt = "Hãy viết một đánh giá ngắn gọn chỉ ra mặt hàng nào cần nhập gấp, mặt hàng nào đang ế cần xả hàng.";
        }
        int days = (request.getDays() != null && request.getDays() > 0) ? request.getDays() : 30;
        return ResponseEntity.ok(geminiService.analyzeInventory(prompt, days));
        // return ResponseEntity.ok(geminiService.analyzeInventoryOld(prompt, days));
    }
}
