package com.graduate.novel.domain.crawl;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;

/**
 * Simple test to check Syosetu HTML structure
 */
public class QuickSyosetuTest {

    public static void main(String[] args) {
        try {
            String url = "https://ncode.syosetu.com/n9669bk/1/";
            System.out.println("Fetching: " + url);
            System.out.println("Expected: プロローグ\n");

            Document doc = Jsoup.connect(url)
                    .userAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36")
                    .timeout(30000)
                    .get();

            System.out.println("Page title: " + doc.title());
            System.out.println("\n=== Testing Selectors ===");

            testSelector(doc, ".novel_subtitle");
            testSelector(doc, "p.novel_subtitle");
            testSelector(doc, ".p-novel__title");
            testSelector(doc, "#novel_subtitle");
            testSelector(doc, "h1");
            testSelector(doc, ".chapter_title");
            testSelector(doc, ".novel_bn");

            System.out.println("\n=== Elements with 'subtitle' ===");
            doc.select("[class*=subtitle]").forEach(el ->
                System.out.println(el.tagName() + "." + el.className() + " = '" + el.text() + "'")
            );

            System.out.println("\n=== All <p> tags with class ===");
            doc.select("p[class]").stream().limit(10).forEach(el ->
                System.out.println("p." + el.className() + " = '" +
                    (el.text().length() > 100 ? el.text().substring(0, 100) + "..." : el.text()) + "'")
            );

        } catch (Exception e) {
            System.err.println("Error: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private static void testSelector(Document doc, String selector) {
        Element element = doc.selectFirst(selector);
        if (element != null && !element.text().trim().isEmpty()) {
            System.out.println("✅ " + selector + " = '" + element.text().trim() + "'");
        } else if (element != null) {
            System.out.println("⚠️ " + selector + " found but empty");
        } else {
            System.out.println("❌ " + selector + " not found");
        }
    }
}

