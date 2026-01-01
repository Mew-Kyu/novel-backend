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

    /**
     * Uploads a user avatar to Cloudinary with automatic WebP conversion and optimization.
     * Avatars are stored in the "user_avatars" folder with size constraints.
     *
     * @param file The avatar image file to upload
     * @return The secure URL of the uploaded avatar
     * @throws IOException If upload fails
     */
    @SuppressWarnings("unchecked")
    public String uploadAvatar(MultipartFile file) throws IOException {
        log.info("Uploading avatar to Cloudinary: {}", file.getOriginalFilename());

        // Upload the file with WebP conversion, optimization and avatar-specific settings
        Map<String, Object> uploadResult = (Map<String, Object>) cloudinary.uploader().upload(
                file.getBytes(),
                ObjectUtils.asMap(
                        "folder", "user_avatars",            // Store in user_avatars folder
                        "format", "webp",                    // Auto-convert to WebP format
                        "quality", "auto",                   // Automatic quality optimization
                        "width", 400,                        // Resize to 400x400
                        "height", 400,
                        "crop", "fill",                      // Fill the area (may crop)
                        "gravity", "face",                   // Focus on face if detected
                        "resource_type", "image"             // Specify resource type as image
                )
        );

        // Extract and return the secure URL
        String secureUrl = (String) uploadResult.get("secure_url");
        log.info("Avatar uploaded successfully: {}", secureUrl);

        return secureUrl;
    }
}

