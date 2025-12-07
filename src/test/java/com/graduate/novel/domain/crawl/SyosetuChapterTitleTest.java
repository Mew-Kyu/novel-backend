package com.graduate.novel.domain.crawl;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.junit.jupiter.api.Test;

import java.io.IOException;

/**
 * Test to verify Syosetu chapter title extraction
 */
public class SyosetuChapterTitleTest {

    private static final String USER_AGENT = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36";

    @Test
    public void testExtractChapterTitleFromN9669bk() throws IOException {
        // Test URL: https://ncode.syosetu.com/n9669bk/1/
        String url = "https://ncode.syosetu.com/n9669bk/1/";

        System.out.println("Testing chapter title extraction from: " + url);
        System.out.println("Expected title: プロローグ");
        System.out.println("---");

        Document doc = Jsoup.connect(url)
                .userAgent(USER_AGENT)
                .timeout(30000)
                .get();

        System.out.println("Page title: " + doc.title());
        System.out.println("---");

        // Try different selectors
        testSelector(doc, ".novel_subtitle");
        testSelector(doc, "p.novel_subtitle");
        testSelector(doc, ".p-novel__title");
        testSelector(doc, "#novel_subtitle");
        testSelector(doc, "h1");
        testSelector(doc, ".chapter-title");
        testSelector(doc, ".novel_bn");
        testSelector(doc, "p.chapter_title");

        // Print all elements with class
        System.out.println("\n=== All elements with 'novel' in class name: ===");
        doc.select("[class*=novel]").forEach(el -> {
            System.out.println(el.tagName() + "." + el.className() + " = " +
                el.text().substring(0, Math.min(50, el.text().length())));
        });

        // Print all elements with 'subtitle' or 'title' in class
        System.out.println("\n=== All elements with 'title' or 'subtitle' in class: ===");
        doc.select("[class*=title], [class*=subtitle]").forEach(el -> {
            System.out.println(el.tagName() + "." + el.className() + " = " +
                el.text().substring(0, Math.min(50, el.text().length())));
        });
    }

    @Test
    public void testExtractChapterTitleFromN5375cy() throws IOException {
        // Test URL: https://ncode.syosetu.com/n5375cy/1/
        String url = "https://ncode.syosetu.com/n5375cy/1/";

        System.out.println("Testing chapter title extraction from: " + url);
        System.out.println("Expected title: 死亡そして……");
        System.out.println("---");

        Document doc = Jsoup.connect(url)
                .userAgent(USER_AGENT)
                .timeout(30000)
                .get();

        System.out.println("Page title: " + doc.title());
        System.out.println("---");

        // Try different selectors
        testSelector(doc, ".novel_subtitle");
        testSelector(doc, "p.novel_subtitle");
        testSelector(doc, ".p-novel__title");
        testSelector(doc, "#novel_subtitle");
        testSelector(doc, "h1");
    }

    private void testSelector(Document doc, String selector) {
        Element element = doc.selectFirst(selector);
        if (element != null) {
            String text = element.text().trim();
            System.out.println("✅ Selector '" + selector + "' found: " + text);
        } else {
            System.out.println("❌ Selector '" + selector + "' not found");
        }
    }
}

