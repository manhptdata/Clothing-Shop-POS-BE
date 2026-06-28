package com.sapo.mock.clothing.upload.service;

import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class UploadService {

    private final Cloudinary cloudinary;

    /**
     * Deletes an image from Cloudinary given its secure URL.
     * @param imageUrl the secure URL of the image
     * @return true if successfully deleted, false otherwise
     */
    public boolean deleteImage(String imageUrl) {
        try {
            if (imageUrl == null || imageUrl.isEmpty()) {
                return false;
            }
            
            // Extract public ID from Cloudinary URL
            // Format: https://res.cloudinary.com/<cloud_name>/image/upload/v1234567890/folder/image_name.jpg
            String publicId = extractPublicId(imageUrl);
            if (publicId == null) {
                return false;
            }
            
            Map result = cloudinary.uploader().destroy(publicId, ObjectUtils.emptyMap());
            log.info("Cloudinary delete result for {}: {}", publicId, result);
            
            String resultStr = (String) result.get("result");
            return "ok".equalsIgnoreCase(resultStr) || "not found".equalsIgnoreCase(resultStr);
        } catch (Exception e) {
            log.error("Failed to delete image from Cloudinary: {}", imageUrl, e);
            return false;
        }
    }

    private String extractPublicId(String imageUrl) {
        try {
            int uploadIndex = imageUrl.indexOf("/upload/");
            if (uploadIndex == -1) return null;
            
            String substring = imageUrl.substring(uploadIndex + 8);
            int versionEndIndex = substring.indexOf('/');
            if (versionEndIndex != -1 && substring.matches("^v\\d+/.*")) {
                substring = substring.substring(versionEndIndex + 1);
            }
            
            int extensionIndex = substring.lastIndexOf('.');
            if (extensionIndex != -1) {
                substring = substring.substring(0, extensionIndex);
            }
            return substring;
        } catch (Exception e) {
            log.warn("Could not extract public_id from url: {}", imageUrl);
            return null;
        }
    }
}
