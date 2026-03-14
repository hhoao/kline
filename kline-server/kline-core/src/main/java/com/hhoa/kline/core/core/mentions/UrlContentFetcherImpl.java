package com.hhoa.kline.core.core.mentions;

import java.util.concurrent.CompletableFuture;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

@Slf4j
public class UrlContentFetcherImpl implements UrlContentFetcher {
    private static final int READ_TIMEOUT = 10000; // 10 seconds

    @Override
    public CompletableFuture<Void> launchBrowser() {
        return CompletableFuture.completedFuture(null);
    }

    @Override
    public CompletableFuture<Void> closeBrowser() {
        return CompletableFuture.completedFuture(null);
    }

    @Override
    public CompletableFuture<String> urlToMarkdown(String url) {
        return CompletableFuture.supplyAsync(
                () -> {
                    try {
                        // 使用 Jsoup 获取和解析 HTML
                        Document doc =
                                Jsoup.connect(url)
                                        .timeout(READ_TIMEOUT)
                                        .userAgent(
                                                "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36")
                                        .get();

                        doc.select("script, style, nav, footer, header").remove();

                        String markdown = htmlToMarkdown(doc);

                        return markdown;
                    } catch (Exception e) {
                        log.error("Error fetching URL content: {}", url, e);
                        throw new RuntimeException("Error fetching content: " + e.getMessage(), e);
                    }
                });
    }

    /**
     * @param doc Jsoup Document
     * @return Markdown 格式的文本
     */
    private String htmlToMarkdown(Document doc) {
        StringBuilder markdown = new StringBuilder();

        Elements headings = doc.select("h1, h2, h3, h4, h5, h6");
        for (Element heading : headings) {
            int level = Integer.parseInt(heading.tagName().substring(1));
            markdown.append("#".repeat(level)).append(" ").append(heading.text()).append("\n\n");
        }

        Elements paragraphs = doc.select("p");
        for (Element p : paragraphs) {
            String text = p.text().trim();
            if (!text.isEmpty()) {
                markdown.append(text).append("\n\n");
            }
        }

        Elements lists = doc.select("ul, ol");
        for (Element list : lists) {
            Elements items = list.select("li");
            for (Element item : items) {
                markdown.append("- ").append(item.text()).append("\n");
            }
            markdown.append("\n");
        }

        Elements codeBlocks = doc.select("pre, code");
        for (Element code : codeBlocks) {
            if ("pre".equals(code.tagName())) {
                markdown.append("```\n").append(code.text()).append("\n```\n\n");
            } else {
                markdown.append("`").append(code.text()).append("` ");
            }
        }

        Elements links = doc.select("a[href]");
        for (Element link : links) {
            String href = link.attr("href");
            String text = link.text();
            if (!text.isEmpty() && !href.isEmpty()) {
                markdown.append("[").append(text).append("](").append(href).append(")\n");
            }
        }

        if (markdown.length() == 0) {
            markdown.append(doc.body().text());
        }

        return markdown.toString().trim();
    }
}
