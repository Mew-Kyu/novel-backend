package com.graduate.novel.service;

import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Map;

/**
 * Service for handling image uploads to Cloudinary.
 * Provides functionality to upload images with automatic WebP conversion and optimization.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class CloudinaryService {

    private final Cloudinary cloudinary;

    /**
     * Uploads an image to Cloudinary with automatic WebP conversion and optimization.
     * Images are stored in the "novel_covers" folder.
     *
     * @param file The image file to upload
     * @return The secure URL of the uploaded image
     * @throws IOException If upload fails
     */
    @SuppressWarnings("unchecked")
    public String uploadImage(MultipartFile file) throws IOException {
        log.info("Uploading image to Cloudinary: {}", file.getOriginalFilename());

        // Upload the file with WebP conversion and optimization settings
        Map<String, Object> uploadResult = (Map<String, Object>) cloudinary.uploader().upload(
                file.getBytes(),
                ObjectUtils.asMap(
                        "folder", "novel_covers",           // Store in novel_covers folder
                        "format", "webp",                    // Auto-convert to WebP format
                        "quality", "auto",                   // Automatic quality optimization
                        "resource_type", "image"             // Specify resource type as image
                )
        );

        // Extract and return the secure URL
        String secureUrl = (String) uploadResult.get("secure_url");
        log.info("Image uploaded successfully: {}", secureUrl);

        return secureUrl;
    }
}

