package com.graduate.novel.service;

import com.graduate.novel.domain.chapter.Chapter;
import com.graduate.novel.domain.chapter.ChapterRepository;
import com.graduate.novel.domain.story.Story;
import com.graduate.novel.domain.story.StoryRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import nl.siegmann.epublib.domain.Author;
import nl.siegmann.epublib.domain.Book;
import nl.siegmann.epublib.domain.MediaType;
import nl.siegmann.epublib.domain.Resource;
import nl.siegmann.epublib.epub.EpubWriter;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class EpubService {

    private final StoryRepository storyRepository;
    private final ChapterRepository chapterRepository;
    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .build();

    public record EpubResult(byte[] data, String storyTitle) {}

    public EpubResult generateEpub(Long storyId, Integer startChapter, Integer endChapter) {
        // Fetch the Story
        Story story = storyRepository.findById(storyId)
                .orElseThrow(() -> new RuntimeException("Story not found with id: " + storyId));

        // Determine chapters to fetch
        List<Chapter> chapters;
        String titleSuffix;

        if (startChapter != null && endChapter != null) {
            chapters = chapterRepository.findByStoryIdAndChapterIndexBetweenOrderByChapterIndexAsc(
                    storyId, startChapter, endChapter);
            titleSuffix = String.format(" (Ch.%d-%d)", startChapter, endChapter);
        } else {
            chapters = chapterRepository.findByStoryIdOrderByChapterIndexAsc(storyId);
            titleSuffix = " (Full)";
        }

        if (chapters.isEmpty()) {
            throw new RuntimeException("No chapters found for the specified range");
        }

        // Initialize Book
        Book book = new Book();

        // Set Metadata
        String bookTitle = (story.getTitle() != null ? story.getTitle() : "Untitled") + titleSuffix;
        book.getMetadata().addTitle(bookTitle);
        book.getMetadata().setLanguage("ja"); // Set language to Japanese

        String authorName = story.getAuthorName() != null ? story.getAuthorName() : "Unknown Author";
        book.getMetadata().addAuthor(new Author(authorName));

        // Add CSS stylesheet for proper formatting
        try {
            String css = buildCssContent();
            Resource cssResource = new Resource(
                    "main-css",
                    css.getBytes(StandardCharsets.UTF_8),
                    "styles/main.css",
                    new MediaType("text/css", ".css")
            );
            book.getResources().add(cssResource);
        } catch (Exception e) {
            log.warn("Failed to add CSS: {}", e.getMessage());
        }

        // Add cover image if available
        if (story.getCoverImageUrl() != null && !story.getCoverImageUrl().isEmpty()) {
            try {
                byte[] coverImageBytes = downloadImage(story.getCoverImageUrl());
                if (coverImageBytes != null) {
                    String imageType = determineImageType(story.getCoverImageUrl());
                    MediaType mediaType = getMediaTypeForImage(imageType);
                    Resource coverResource = new Resource(coverImageBytes, mediaType);
                    book.setCoverImage(coverResource);
                    log.info("Cover image added successfully");
                }
            } catch (Exception e) {
                log.warn("Failed to download or set cover image: {}", e.getMessage());
            }
        }

        // Add chapters
        for (Chapter chapter : chapters) {
            try {
                String chapterTitle = chapter.getTitle() != null
                        ? chapter.getTitle()
                        : "Chapter " + chapter.getChapterIndex();

                // Use translated content if available, otherwise use raw content
                String content = chapter.getTranslatedContent() != null
                        ? chapter.getTranslatedContent()
                        : (chapter.getRawContent() != null ? chapter.getRawContent() : "");

                String htmlContent = buildHtmlContent(chapterTitle, content);

                String chapterHref = "chapter-" + chapter.getChapterIndex() + ".html";
                Resource chapterResource = new Resource(
                        "chapter-" + chapter.getChapterIndex(),
                        htmlContent.getBytes(StandardCharsets.UTF_8),
                        chapterHref,
                        new MediaType("application/xhtml+xml", ".html")
                );

                book.addSection(chapterTitle, chapterResource);
            } catch (Exception e) {
                log.error("Failed to add chapter {}: {}", chapter.getChapterIndex(), e.getMessage());
            }
        }

        // Write the book to ByteArrayOutputStream
        try (ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
            EpubWriter epubWriter = new EpubWriter();
            epubWriter.write(book, outputStream);
            byte[] epubBytes = outputStream.toByteArray();
            log.info("EPUB generated successfully: {} bytes", epubBytes.length);
            return new EpubResult(epubBytes, story.getTitle());
        } catch (IOException e) {
            log.error("Failed to generate EPUB: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to generate EPUB file", e);
        }
    }

    private String buildHtmlContent(String title, String content) {
        // Convert plain text to HTML paragraphs
        String[] paragraphs = content.split("\n");
        StringBuilder htmlBuilder = new StringBuilder();

        htmlBuilder.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n");
        htmlBuilder.append("<!DOCTYPE html>\n");
        htmlBuilder.append("<html xmlns=\"http://www.w3.org/1999/xhtml\" xml:lang=\"ja\" lang=\"ja\">\n");
        htmlBuilder.append("<head>\n");
        htmlBuilder.append("<meta charset=\"UTF-8\"/>\n");
        htmlBuilder.append("<title>").append(escapeHtml(title)).append("</title>\n");
        htmlBuilder.append("<link rel=\"stylesheet\" type=\"text/css\" href=\"../styles/main.css\"/>\n");
        htmlBuilder.append("</head>\n");
        htmlBuilder.append("<body>\n");
        htmlBuilder.append("<h1>").append(escapeHtml(title)).append("</h1>\n");

        for (String paragraph : paragraphs) {
            if (!paragraph.trim().isEmpty()) {
                htmlBuilder.append("<p>").append(escapeHtml(paragraph)).append("</p>\n");
            }
        }

        htmlBuilder.append("</body>\n");
        htmlBuilder.append("</html>");

        return htmlBuilder.toString();
    }

    private String buildCssContent() {
        return """
                @charset "UTF-8";
                
                body {
                    font-family: serif;
                    line-height: 1.8;
                    margin: 1em;
                    padding: 0;
                    text-align: justify;
                }
                
                h1 {
                    font-size: 1.5em;
                    font-weight: bold;
                    text-align: center;
                    margin: 1em 0;
                    page-break-before: always;
                }
                
                p {
                    margin: 0.5em 0;
                    text-indent: 1em;
                }
                
                p:first-of-type {
                    text-indent: 0;
                }
                """;
    }

    private String escapeHtml(String text) {
        if (text == null) return "";
        return text.replace("&", "&amp;")
                   .replace("<", "&lt;")
                   .replace(">", "&gt;")
                   .replace("\"", "&quot;")
                   .replace("'", "&#39;");
    }

    private byte[] downloadImage(String imageUrl) {
        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(imageUrl))
                    .timeout(Duration.ofSeconds(10))
                    .GET()
                    .build();

            HttpResponse<byte[]> response = httpClient.send(request,
                    HttpResponse.BodyHandlers.ofByteArray());

            if (response.statusCode() == 200) {
                return response.body();
            } else {
                log.warn("Failed to download image, status code: {}", response.statusCode());
                return null;
            }
        } catch (Exception e) {
            log.warn("Exception while downloading image: {}", e.getMessage());
            return null;
        }
    }

    private String determineImageType(String imageUrl) {
        String lowerUrl = imageUrl.toLowerCase();
        if (lowerUrl.endsWith(".png")) return "png";
        if (lowerUrl.endsWith(".gif")) return "gif";
        if (lowerUrl.endsWith(".jpg") || lowerUrl.endsWith(".jpeg")) return "jpg";
        return "jpg"; // default
    }

    private MediaType getMediaTypeForImage(String imageType) {
        return switch (imageType.toLowerCase()) {
            case "png" -> new MediaType("image/png", ".png");
            case "gif" -> new MediaType("image/gif", ".gif");
            case "jpg", "jpeg" -> new MediaType("image/jpeg", ".jpg");
            default -> new MediaType("image/jpeg", ".jpg");
        };
    }
}

