package com.graduate.novel.controller;

import com.graduate.novel.service.CloudinaryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/**
 * Controller for handling image uploads to Cloudinary.
 * Provides endpoints for uploading novel cover images with automatic WebP conversion.
 */
@RestController
@RequestMapping("/api/cloudinary")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Cloudinary", description = "Image upload management using Cloudinary CDN")
public class CloudinaryController {

    private final CloudinaryService cloudinaryService;

    /**
     * Upload a novel cover image to Cloudinary.
     * Only authenticated users with ADMIN or MODERATOR role can upload images.
     *
     * @param file The image file to upload (supports common image formats)
     * @return ResponseEntity containing the secure URL of the uploaded image
     */
    @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasAnyRole('ADMIN', 'MODERATOR')")
    @Operation(
            summary = "Upload novel cover image",
            description = "Upload an image to Cloudinary with automatic WebP conversion and optimization. " +
                         "Images are stored in the 'novel_covers' folder. " +
                         "Requires ADMIN or MODERATOR role.",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Image uploaded successfully",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = UploadResponse.class)
                    )
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Invalid file or file too large",
                    content = @Content(mediaType = "application/json")
            ),
            @ApiResponse(
                    responseCode = "401",
                    description = "Unauthorized - JWT token missing or invalid",
                    content = @Content(mediaType = "application/json")
            ),
            @ApiResponse(
                    responseCode = "403",
                    description = "Forbidden - Insufficient permissions (requires ADMIN or MODERATOR role)",
                    content = @Content(mediaType = "application/json")
            ),
            @ApiResponse(
                    responseCode = "500",
                    description = "Internal server error during upload",
                    content = @Content(mediaType = "application/json")
            )
    })
    public ResponseEntity<?> uploadImage(
            @Parameter(
                    description = "Image file to upload (JPG, PNG, GIF, WebP supported)",
                    required = true,
                    content = @Content(mediaType = MediaType.MULTIPART_FORM_DATA_VALUE)
            )
            @RequestParam("file") MultipartFile file
    ) {
        try {
            // Validate file
            if (file.isEmpty()) {
                return ResponseEntity.badRequest()
                        .body(createErrorResponse("File is empty"));
            }

            // Validate file type
            String contentType = file.getContentType();
            if (contentType == null || !contentType.startsWith("image/")) {
                return ResponseEntity.badRequest()
                        .body(createErrorResponse("File must be an image"));
            }

            // Validate file size (10MB limit)
            long maxSize = 10 * 1024 * 1024; // 10MB
            if (file.getSize() > maxSize) {
                return ResponseEntity.badRequest()
                        .body(createErrorResponse("File size exceeds 10MB limit"));
            }

            log.info("Uploading image: {} ({})", file.getOriginalFilename(), file.getSize());

            // Upload to Cloudinary
            String imageUrl = cloudinaryService.uploadImage(file);

            // Return success response
            return ResponseEntity.ok(createSuccessResponse(imageUrl, file.getOriginalFilename()));

        } catch (IOException e) {
            log.error("Failed to upload image: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(createErrorResponse("Upload failed: " + e.getMessage()));
        } catch (Exception e) {
            log.error("Unexpected error during upload: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(createErrorResponse("Unexpected error: " + e.getMessage()));
        }
    }

    /**
     * Create success response with upload details.
     */
    private Map<String, Object> createSuccessResponse(String imageUrl, String originalFilename) {
        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("message", "Image uploaded successfully");
        response.put("url", imageUrl);
        response.put("originalFilename", originalFilename);
        return response;
    }

    /**
     * Create error response.
     */
    private Map<String, Object> createErrorResponse(String message) {
        Map<String, Object> response = new HashMap<>();
        response.put("success", false);
        response.put("error", message);
        return response;
    }

    /**
     * Schema for Swagger documentation.
     */
    @Schema(description = "Upload response")
    public static class UploadResponse {
        @Schema(description = "Upload success status", example = "true")
        public boolean success;

        @Schema(description = "Response message", example = "Image uploaded successfully")
        public String message;

        @Schema(description = "Cloudinary secure URL", example = "https://res.cloudinary.com/demo/image/upload/v1234567890/novel_covers/abc123.webp")
        public String url;

        @Schema(description = "Original filename", example = "cover.jpg")
        public String originalFilename;
    }
}

