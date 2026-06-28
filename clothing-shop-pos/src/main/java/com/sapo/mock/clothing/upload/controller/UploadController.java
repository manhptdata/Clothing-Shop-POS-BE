package com.sapo.mock.clothing.upload.controller;

import com.sapo.mock.clothing.upload.service.UploadService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/uploads")
@RequiredArgsConstructor
public class UploadController {

    private final UploadService uploadService;

    @DeleteMapping
    public ResponseEntity<String> deleteImage(@RequestParam String url) {
        boolean success = uploadService.deleteImage(url);
        if (success) {
            return ResponseEntity.ok("Image deleted successfully");
        } else {
            return ResponseEntity.badRequest().body("Failed to delete image");
        }
    }
}
