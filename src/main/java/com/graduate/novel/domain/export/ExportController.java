package com.graduate.novel.domain.export;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/export")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Export", description = "APIs for exporting stories to various formats")
public class ExportController {

    private final EpubService epubService;

    @GetMapping("/{storyId}/epub")
    @Operation(
            summary = "Export story to EPUB format",
            description = "Generate and download an EPUB file for a story. Optionally specify chapter range."
    )
    public ResponseEntity<byte[]> exportToEpub(
            @Parameter(description = "Story ID", required = true)
            @PathVariable Long storyId,

            @Parameter(description = "Start chapter index (optional, inclusive)")
            @RequestParam(required = false) Integer start,

            @Parameter(description = "End chapter index (optional, inclusive)")
            @RequestParam(required = false) Integer end
    ) {
        try {
            log.info("Exporting story {} to EPUB (start: {}, end: {})", storyId, start, end);

            EpubService.EpubResult result = epubService.generateEpub(storyId, start, end);
            byte[] epubBytes = result.data();
            String storyTitle = result.storyTitle();

            if (epubBytes == null || epubBytes.length == 0) {
                log.error("Generated EPUB file is empty for story {}", storyId);
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                        .body(null);
            }

            // Generate filename from story title
            String filename = generateFilename(storyId, storyTitle, start, end);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.parseMediaType("application/epub+zip"));

            // Simple ASCII-safe filename in Content-Disposition
            headers.add("Content-Disposition", "attachment; filename=\"" + filename + "\"");

            headers.setContentLength(epubBytes.length);
            headers.add("Cache-Control", "no-cache, no-store, must-revalidate");
            headers.add("Pragma", "no-cache");
            headers.add("Expires", "0");

            log.info("Successfully exported story {} to EPUB, size: {} bytes, filename: {}",
                    storyId, epubBytes.length, filename);

            return new ResponseEntity<>(epubBytes, headers, HttpStatus.OK);

        } catch (RuntimeException e) {
            log.error("Failed to export story {} to EPUB: {}", storyId, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(null);
        } catch (Exception e) {
            log.error("Unexpected error exporting story {} to EPUB: {}", storyId, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(null);
        }
    }

    private String generateFilename(Long storyId, String storyTitle, Integer start, Integer end) {
        // Use story ID as base for ASCII-safe filename
        StringBuilder filename = new StringBuilder("story_");
        filename.append(storyId);

        // Try to add ASCII-safe part of title if available
        if (storyTitle != null && !storyTitle.isEmpty()) {
            String asciiPart = convertToAsciiSafe(storyTitle);
            if (!asciiPart.isEmpty()) {
                filename.append("_").append(asciiPart);
            }
        }

        if (start != null && end != null) {
            filename.append("_ch").append(start).append("-").append(end);
        } else {
            filename.append("_full");
        }

        filename.append(".epub");

        return filename.toString();
    }

    private String convertToAsciiSafe(String title) {
        // First, remove Vietnamese diacritics
        String normalized = removeVietnameseDiacritics(title);

        // Then extract ASCII-safe characters
        StringBuilder ascii = new StringBuilder();
        for (char c : normalized.toCharArray()) {
            if ((c >= 'a' && c <= 'z') || (c >= 'A' && c <= 'Z') ||
                (c >= '0' && c <= '9') || c == ' ') {
                ascii.append(c);
            }
        }

        String result = ascii.toString().trim().replace(" ", "_");

        // Limit to 50 characters for better readability
        if (result.length() > 50) {
            result = result.substring(0, 50);
        }

        return result;
    }

    private String removeVietnameseDiacritics(String text) {
        if (text == null) return "";

        // Vietnamese character mappings
        String[][] vietnameseMappings = {
            // Lowercase
            {"á|à|ả|ã|ạ|ă|ắ|ằ|ẳ|ẵ|ặ|â|ấ|ầ|ẩ|ẫ|ậ", "a"},
            {"đ", "d"},
            {"é|è|ẻ|ẽ|ẹ|ê|ế|ề|ể|ễ|ệ", "e"},
            {"í|ì|ỉ|ĩ|ị", "i"},
            {"ó|ò|ỏ|õ|ọ|ô|ố|ồ|ổ|ỗ|ộ|ơ|ớ|ờ|ở|ỡ|ợ", "o"},
            {"ú|ù|ủ|ũ|ụ|ư|ứ|ừ|ử|ữ|ự", "u"},
            {"ý|ỳ|ỷ|ỹ|ỵ", "y"},
            // Uppercase
            {"Á|À|Ả|Ã|Ạ|Ă|Ắ|Ằ|Ẳ|Ẵ|Ặ|Â|Ấ|Ầ|Ẩ|Ẫ|Ậ", "A"},
            {"Đ", "D"},
            {"É|È|Ẻ|Ẽ|Ẹ|Ê|Ế|Ề|Ể|Ễ|Ệ", "E"},
            {"Í|Ì|Ỉ|Ĩ|Ị", "I"},
            {"Ó|Ò|Ỏ|Õ|Ọ|Ô|Ố|Ồ|Ổ|Ỗ|Ộ|Ơ|Ớ|Ờ|Ở|Ỡ|Ợ", "O"},
            {"Ú|Ù|Ủ|Ũ|Ụ|Ư|Ứ|Ừ|Ử|Ữ|Ự", "U"},
            {"Ý|Ỳ|Ỷ|Ỹ|Ỵ", "Y"}
        };

        String result = text;
        for (String[] mapping : vietnameseMappings) {
            result = result.replaceAll(mapping[0], mapping[1]);
        }

        return result;
    }
}

