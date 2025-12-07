package com.graduate.novel.domain.crawl;

import com.graduate.novel.common.exception.BadRequestException;
import com.graduate.novel.domain.chapter.Chapter;
import com.graduate.novel.domain.chapter.ChapterRepository;
import com.graduate.novel.domain.story.Story;
import com.graduate.novel.domain.story.StoryRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Random;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
@RequiredArgsConstructor
@Slf4j
public class SyosetuCrawlService {

    private final StoryRepository storyRepository;
    private final ChapterRepository chapterRepository;

    private static final String USER_AGENT = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36";
    private static final int MIN_DELAY_MS = 1000;
    private static final int MAX_DELAY_MS = 3000;
    private static final int TIMEOUT_MS = 30000;
    private static final Random random = new Random();

    /**
     * Main entry point to crawl a novel from Syosetu
     */
    @Transactional
    public CrawlNovelResponse crawlNovel(CrawlNovelRequest request) {
        log.info("Starting crawl for novel URL: {}", request.getNovelUrl());

        // Validate URL
        validateNovelUrl(request.getNovelUrl());

        // Extract novel code from URL (e.g., "n5375cy" from "https://ncode.syosetu.com/n5375cy/")
        String novelCode = extractNovelCode(request.getNovelUrl());
        String baseUrl = "https://ncode.syosetu.com/" + novelCode + "/";

        try {
            // Step 1: Crawl main page to get title and description
            Document mainPage = fetchDocument(baseUrl);
            String title = extractTitle(mainPage);
            String description = extractDescription(mainPage);
            String authorName = extractAuthorName(mainPage);

            log.info("Crawled novel info - Title: {}, Author: {}, Description length: {}",
                    title, authorName, description != null ? description.length() : 0);

            // Step 2: Save or update story
            Story story = saveOrUpdateStory(baseUrl, title, description, authorName);

            // Step 3: Determine chapter range
            int startChapter = request.getStartChapter() != null ? request.getStartChapter() : 1;
            int endChapter = request.getEndChapter() != null ? request.getEndChapter() : 1;

            if (endChapter < startChapter) {
                throw new BadRequestException("End chapter must be greater than or equal to start chapter");
            }

            log.info("Crawling chapters {} to {} for story ID: {}", startChapter, endChapter, story.getId());

            // Step 4: Crawl chapters
            int succeeded = 0;
            int failed = 0;

            for (int chapterNum = startChapter; chapterNum <= endChapter; chapterNum++) {
                try {
                    // Add polite delay between requests
                    if (chapterNum > startChapter) {
                        addPoliteDelay();
                    }

                    boolean success = crawlAndSaveChapter(story, baseUrl, chapterNum);
                    if (success) {
                        succeeded++;
                        log.info("Successfully crawled chapter {}/{}", chapterNum, endChapter);
                    } else {
                        failed++;
                        log.warn("Failed to crawl chapter {}/{}", chapterNum, endChapter);
                    }
                } catch (Exception e) {
                    failed++;
                    log.error("Error crawling chapter {}: {}", chapterNum, e.getMessage(), e);
                }
            }

            int totalCrawled = endChapter - startChapter + 1;

            return CrawlNovelResponse.builder()
                    .storyId(story.getId())
                    .title(story.getTitle())
                    .description(story.getDescription())
                    .chaptersCrawled(totalCrawled)
                    .chaptersSucceeded(succeeded)
                    .chaptersFailed(failed)
                    .message(String.format("Crawl completed: %d succeeded, %d failed out of %d chapters",
                            succeeded, failed, totalCrawled))
                    .build();

        } catch (Exception e) {
            log.error("Error during novel crawl: {}", e.getMessage(), e);
            throw new BadRequestException("Failed to crawl novel: " + e.getMessage());
        }
    }

    /**
     * Validate that the URL is from ncode.syosetu.com
     */
    private void validateNovelUrl(String url) {
        if (url == null || !url.toLowerCase().contains("ncode.syosetu.com")) {
            throw new BadRequestException("Invalid URL. Must be from ncode.syosetu.com");
        }
    }

    /**
     * Extract novel code from URL (e.g., "n5375cy" from "https://ncode.syosetu.com/n5375cy/")
     */
    private String extractNovelCode(String url) {
        Pattern pattern = Pattern.compile("ncode\\.syosetu\\.com/([a-z0-9]+)", Pattern.CASE_INSENSITIVE);
        Matcher matcher = pattern.matcher(url);

        if (matcher.find()) {
            return matcher.group(1);
        }

        throw new BadRequestException("Could not extract novel code from URL");
    }

    /**
     * Fetch a document from URL with proper headers and encoding
     */
    private Document fetchDocument(String url) throws Exception {
        log.debug("Fetching URL: {}", url);

        return Jsoup.connect(url)
                .userAgent(USER_AGENT)
                .timeout(TIMEOUT_MS)
                .followRedirects(true)
                .ignoreHttpErrors(false)
                .get();
    }

    /**
     * Extract title from main page
     */
    private String extractTitle(Document doc) {
        // Try .novel_title first
        Element titleElement = doc.selectFirst(".novel_title");
        if (titleElement != null && !titleElement.text().trim().isEmpty()) {
            return titleElement.text().trim();
        }

        // Fallback to h1
        titleElement = doc.selectFirst("h1");
        if (titleElement != null && !titleElement.text().trim().isEmpty()) {
            return titleElement.text().trim();
        }

        // Fallback to title tag
        String title = doc.title();
        if (title != null && !title.trim().isEmpty()) {
            return title.trim();
        }

        return "Untitled Novel";
    }

    /**
     * Extract description/synopsis from main page
     */
    private String extractDescription(Document doc) {
        Element descElement = doc.selectFirst("#novel_ex");
        if (descElement != null) {
            // Get text but preserve line breaks
            String html = descElement.html();
            // Replace <br> tags with newlines
            html = html.replaceAll("<br\\s*/?>", "\n");
            // Remove all other HTML tags
            return Jsoup.parse(html).text();
        }
        return null;
    }

    /**
     * Extract author name from main page
     * Author name is typically after "作者：" text
     */
    private String extractAuthorName(Document doc) {
        // Try to find author in the novel_writername div
        Element authorElement = doc.selectFirst(".novel_writername");
        if (authorElement != null) {
            String text = authorElement.text();
            // Author name is usually after "作者："
            // Example: "作者：錬金王"
            if (text.contains("作者：")) {
                String authorName = text.substring(text.indexOf("作者：") + 3).trim();
                // Remove any trailing links or extra text
                if (authorName.contains("(") || authorName.contains("（")) {
                    authorName = authorName.split("[（(]")[0].trim();
                }
                log.info("Extracted author name: {}", authorName);
                return authorName;
            }
            // If no "作者：" prefix, return the whole text
            return text.trim();
        }

        // Try alternative selector
        authorElement = doc.selectFirst(".p-novel__author");
        if (authorElement != null) {
            return authorElement.text().replaceFirst("作者：", "").trim();
        }

        log.warn("Could not find author name in page");
        return null;
    }

    /**
     * Save or update story in database
     */
    @Transactional
    private Story saveOrUpdateStory(String sourceUrl, String title, String description, String authorName) {
        // Check if story already exists by source URL
        var existingStories = storyRepository.findBySourceUrl(sourceUrl);

        if (!existingStories.isEmpty()) {
            Story story = existingStories.get(0);
            // Update existing story - save to raw columns since this is crawled data
            story.setTitle(title);
            story.setRawTitle(title);
            story.setDescription(description);
            story.setRawDescription(description);
            story.setAuthorName(authorName);
            story.setRawAuthorName(authorName);
            log.info("Updating existing story ID: {}", story.getId());
            return storyRepository.save(story);
        } else {
            // Create new story - save to raw columns since this is crawled data
            Story story = Story.builder()
                    .title(title)
                    .rawTitle(title)
                    .description(description)
                    .rawDescription(description)
                    .authorName(authorName)
                    .rawAuthorName(authorName)
                    .sourceUrl(sourceUrl)
                    .sourceSite("syosetu")
                    .build();
            story = storyRepository.save(story);
            log.info("Created new story with ID: {}", story.getId());
            return story;
        }
    }

    /**
     * Crawl a specific chapter and save to database
     */
    private boolean crawlAndSaveChapter(Story story, String baseUrl, int chapterNum) {
        String chapterUrl = baseUrl + chapterNum + "/";

        log.info("Crawling chapter {} from URL: {}", chapterNum, chapterUrl);

        try {
            // Fetch chapter page
            Document chapterDoc = fetchDocument(chapterUrl);
            log.debug("Fetched chapter page. Title: {}", chapterDoc.title());
            log.info("📄 Page URL: {}", chapterUrl);
            log.info("📄 Page title tag: {}", chapterDoc.title());

            // Extract chapter title (subtitle)
            String chapterTitle = extractChapterTitle(chapterDoc, chapterNum);
            log.info("📝 Extracted chapter title: {}", chapterTitle);

            // Extract chapter content
            String content = extractChapterContent(chapterDoc);

            if (content == null || content.trim().isEmpty()) {
                log.error("❌ No content found for chapter {} at URL: {}", chapterNum, chapterUrl);
                log.error("Page HTML preview: {}", chapterDoc.html().substring(0, Math.min(500, chapterDoc.html().length())));
                saveFailedChapter(story, chapterNum, chapterTitle);
                return false;
            }

            log.info("✅ Successfully extracted content for chapter {}: {} characters", chapterNum, content.length());

            // Check if chapter already exists
            var existingChapter = chapterRepository.findByStoryIdAndChapterIndex(story.getId(), chapterNum);

            Chapter chapter;
            if (existingChapter.isPresent()) {
                // Update existing chapter
                chapter = existingChapter.get();
                chapter.setTitle(chapterTitle);
                chapter.setRawTitle(chapterTitle);
                chapter.setRawContent(content);
                chapter.setCrawlStatus("SUCCESS");
                chapter.setCrawlTime(LocalDateTime.now());
                log.debug("Updating existing chapter {} for story ID: {}", chapterNum, story.getId());
            } else {
                // Create new chapter
                chapter = Chapter.builder()
                        .story(story)
                        .chapterIndex(chapterNum)
                        .title(chapterTitle)
                        .rawTitle(chapterTitle)
                        .rawContent(content)
                        .crawlStatus("SUCCESS")
                        .crawlTime(LocalDateTime.now())
                        .build();
                log.debug("Creating new chapter {} for story ID: {}", chapterNum, story.getId());
            }

            chapterRepository.save(chapter);
            return true;

        } catch (Exception e) {
            log.error("Failed to crawl chapter {}: {}", chapterNum, e.getMessage(), e);
            saveFailedChapter(story, chapterNum, null);
            return false;
        }
    }

    /**
     * Extract chapter title (subtitle)
     */
    private String extractChapterTitle(Document doc, int chapterNum) {
        log.debug("Attempting to extract chapter title for chapter {}", chapterNum);

        // Try .novel_subtitle first (most common)
        Element subtitleElement = doc.selectFirst(".novel_subtitle");
        if (subtitleElement != null && !subtitleElement.text().trim().isEmpty()) {
            String title = subtitleElement.text().trim();
            log.info("Found chapter title using .novel_subtitle: {}", title);
            return title;
        }

        // Try p.novel_subtitle
        subtitleElement = doc.selectFirst("p.novel_subtitle");
        if (subtitleElement != null && !subtitleElement.text().trim().isEmpty()) {
            String title = subtitleElement.text().trim();
            log.info("Found chapter title using p.novel_subtitle: {}", title);
            return title;
        }

        // Try .p-novel__title
        subtitleElement = doc.selectFirst(".p-novel__title");
        if (subtitleElement != null && !subtitleElement.text().trim().isEmpty()) {
            String title = subtitleElement.text().trim();
            log.info("Found chapter title using .p-novel__title: {}", title);
            return title;
        }

        // Try #novel_subtitle
        subtitleElement = doc.selectFirst("#novel_subtitle");
        if (subtitleElement != null && !subtitleElement.text().trim().isEmpty()) {
            String title = subtitleElement.text().trim();
            log.info("Found chapter title using #novel_subtitle: {}", title);
            return title;
        }

        // Try h1 (sometimes chapter title is in h1)
        subtitleElement = doc.selectFirst("h1");
        if (subtitleElement != null) {
            String text = subtitleElement.text().trim();
            // Check if it's not the main novel title (usually main title is longer)
            if (!text.isEmpty() && !text.contains("小説家になろう")) {
                log.info("Found chapter title using h1: {}", text);
                return text;
            }
        }

        // Try .chapter-title or similar
        subtitleElement = doc.selectFirst(".chapter-title");
        if (subtitleElement != null && !subtitleElement.text().trim().isEmpty()) {
            String title = subtitleElement.text().trim();
            log.info("Found chapter title using .chapter-title: {}", title);
            return title;
        }

        // Debug: Log available classes and IDs
        log.warn("Could not find chapter title using any known selector for chapter {}", chapterNum);
        log.debug("Available classes in page: {}",
            doc.select("[class]").stream()
                .map(Element::className)
                .distinct()
                .limit(20)
                .toList());
        log.debug("Available IDs in page: {}",
            doc.select("[id]").stream()
                .map(Element::id)
                .distinct()
                .limit(20)
                .toList());

        // Last resort: check page title
        String pageTitle = doc.title();
        if (pageTitle != null && !pageTitle.isEmpty()) {
            // Try to extract subtitle from page title (format: "Chapter Title - Novel Title")
            String[] parts = pageTitle.split("-|｜|\\|");
            if (parts.length > 1) {
                String possibleTitle = parts[0].trim();
                if (!possibleTitle.isEmpty() && !possibleTitle.equals("小説家になろう")) {
                    log.info("Extracted chapter title from page title: {}", possibleTitle);
                    return possibleTitle;
                }
            }
        }

        // Fallback to generic title
        log.warn("Using fallback generic title for chapter {}", chapterNum);
        return "Chapter " + chapterNum;
    }

    /**
     * Extract chapter content (main body text)
     */
    private String extractChapterContent(Document doc) {
        log.debug("Extracting chapter content...");

        Element contentElement = doc.selectFirst("#novel_honbun");
        if (contentElement == null) {
            log.warn("Content element #novel_honbun not found! Trying alternative selectors...");

            // Try alternative selectors for Syosetu
            contentElement = doc.selectFirst("div.novel_view");
            if (contentElement != null) {
                log.info("Found content with selector: div.novel_view");
            }
        }

        if (contentElement == null) {
            contentElement = doc.selectFirst("#novel_color");
            if (contentElement != null) {
                log.info("Found content with selector: #novel_color");
            }
        }

        if (contentElement == null) {
            contentElement = doc.selectFirst("div#novel_no");
            if (contentElement != null) {
                log.info("Found content with selector: div#novel_no");
            }
        }

        if (contentElement == null) {
            contentElement = doc.selectFirst(".novel_body");
            if (contentElement != null) {
                log.info("Found content with selector: .novel_body");
            }
        }

        if (contentElement == null) {
            // Try to find any div containing paragraph tags as last resort
            contentElement = doc.selectFirst("div.p-novel__body");
            if (contentElement != null) {
                log.info("Found content with selector: div.p-novel__body");
            }
        }

        if (contentElement == null) {
            log.error("No content element found with any selector!");
            log.debug("Page title: {}", doc.title());
            log.debug("Available IDs in page: {}", doc.select("[id]").stream()
                    .map(e -> e.id())
                    .limit(10)
                    .toList());
            log.debug("Available classes in page: {}", doc.select("[class]").stream()
                    .map(e -> e.className())
                    .distinct()
                    .limit(10)
                    .toList());
            return null;
        }

        // Get HTML and preserve line breaks
        String html = contentElement.html();
        log.debug("Raw HTML length: {}", html.length());

        // Replace <br> tags with newlines
        html = html.replaceAll("<br\\s*/?>", "\n");

        // Replace </p> tags with double newlines for paragraph breaks
        html = html.replaceAll("</p>", "\n\n");

        // Remove all other HTML tags
        String text = Jsoup.parse(html).text();

        // Clean up excessive whitespace but preserve intentional line breaks
        text = text.replaceAll("[ \\t]+", " "); // Replace multiple spaces/tabs with single space
        text = text.replaceAll("\n{3,}", "\n\n"); // Replace 3+ newlines with double newline

        String result = text.trim();
        log.debug("Extracted content length: {}", result.length());
        if (result.length() > 0) {
            log.debug("Content preview: {}", result.length() > 100 ? result.substring(0, 100) + "..." : result);
        }

        return result;
    }

    /**
     * Save a failed chapter placeholder
     */
    private void saveFailedChapter(Story story, int chapterNum, String chapterTitle) {
        var existingChapter = chapterRepository.findByStoryIdAndChapterIndex(story.getId(), chapterNum);

        Chapter chapter;
        if (existingChapter.isPresent()) {
            chapter = existingChapter.get();
        } else {
            chapter = Chapter.builder()
                    .story(story)
                    .chapterIndex(chapterNum)
                    .build();
        }

        if (chapterTitle != null) {
            chapter.setTitle(chapterTitle);
        }
        chapter.setCrawlStatus("FAILED");
        chapter.setCrawlTime(LocalDateTime.now());

        chapterRepository.save(chapter);
    }

    /**
     * Add a random delay between requests to be polite
     */
    private void addPoliteDelay() {
        try {
            int delayMs = MIN_DELAY_MS + random.nextInt(MAX_DELAY_MS - MIN_DELAY_MS + 1);
            log.debug("Adding polite delay of {}ms", delayMs);
            TimeUnit.MILLISECONDS.sleep(delayMs);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.warn("Delay interrupted", e);
        }
    }
}

