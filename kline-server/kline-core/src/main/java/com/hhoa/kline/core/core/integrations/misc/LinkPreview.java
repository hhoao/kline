package com.hhoa.kline.core.core.integrations.misc;

import java.net.URI;
import java.net.URISyntaxException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.regex.Pattern;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;

/**
 * 链接预览工具类 用于获取 URL 的 Open Graph 元数据
 *
 * @author hhoa
 */
@Slf4j
public class LinkPreview {

    private static final HttpClient httpClient =
            HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5)).build();

    private static final Pattern IMAGE_EXTENSION_PATTERN =
            Pattern.compile(
                    "\\.(jpg|jpeg|png|gif|webp|bmp|svg|tiff|tif|avif)$", Pattern.CASE_INSENSITIVE);

    @Data
    public static class OpenGraphData {
        private String title;
        private String description;
        private String image;
        private String url;
        private String siteName;
        private String type;
    }

    /**
     * 获取 Open Graph 元数据
     *
     * @param urlString URL 字符串
     * @return Open Graph 数据
     */
    public static OpenGraphData fetchOpenGraphData(String urlString) {
        try {
            URI uri = new URI(urlString);

            HttpRequest request =
                    HttpRequest.newBuilder()
                            .uri(uri)
                            .timeout(Duration.ofSeconds(5))
                            .header(
                                    "User-Agent",
                                    "Mozilla/5.0 (compatible; JavaClient/1.0; +https://cline.bot)")
                            .GET()
                            .build();

            HttpResponse<String> response =
                    httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() >= 200 && response.statusCode() < 300) {
                Document doc = Jsoup.parse(response.body(), urlString);
                return parseOpenGraphData(doc, urlString, uri);
            } else {
                return createFallbackData(uri, urlString);
            }
        } catch (Exception e) {
            log.warn("Failed to fetch Open Graph data for URL: {}", urlString, e);
            try {
                URI uri = new URI(urlString);
                return createFallbackData(uri, urlString);
            } catch (URISyntaxException ex) {
                return createFallbackDataFromString(urlString);
            }
        }
    }

    /**
     * 解析 Open Graph 数据
     *
     * @param doc HTML 文档
     * @param urlString 原始 URL
     * @param uri URI 对象
     * @return Open Graph 数据
     */
    private static OpenGraphData parseOpenGraphData(Document doc, String urlString, URI uri) {
        OpenGraphData data = new OpenGraphData();

        String title = getMetaTag(doc, "og:title");
        if (title == null) {
            title = getMetaTag(doc, "twitter:title");
        }
        if (title == null) {
            title = getMetaTag(doc, "dc:title");
        }
        if (title == null) {
            Element titleElement = doc.selectFirst("title");
            title = titleElement != null ? titleElement.text() : null;
        }
        data.setTitle(title != null ? title : uri.getHost());

        String description = getMetaTag(doc, "og:description");
        if (description == null) {
            description = getMetaTag(doc, "twitter:description");
        }
        if (description == null) {
            description = getMetaTag(doc, "dc:description");
        }
        if (description == null) {
            description = getMetaTag(doc, "description");
        }
        data.setDescription(description != null ? description : "No description available");

        String imageUrl = getMetaTag(doc, "og:image");
        if (imageUrl == null) {
            imageUrl = getMetaTag(doc, "twitter:image");
        }
        if (imageUrl != null) {
            if (imageUrl.startsWith("/") || imageUrl.startsWith("./")) {
                try {
                    String baseUrl = uri.getScheme() + "://" + uri.getHost();
                    imageUrl = new URI(baseUrl).resolve(imageUrl).toString();
                } catch (URISyntaxException e) {
                    log.warn("Error converting relative URL to absolute: {}", imageUrl, e);
                }
            }
        }
        data.setImage(imageUrl);

        String ogUrl = getMetaTag(doc, "og:url");
        data.setUrl(ogUrl != null ? ogUrl : urlString);

        String siteName = getMetaTag(doc, "og:site_name");
        data.setSiteName(siteName != null ? siteName : uri.getHost());

        data.setType(getMetaTag(doc, "og:type"));

        return data;
    }

    /**
     * 获取 Meta 标签内容
     *
     * @param doc HTML 文档
     * @param property 属性名
     * @return 属性值
     */
    private static String getMetaTag(Document doc, String property) {
        Element element = doc.selectFirst("meta[property=\"" + property + "\"]");
        if (element != null) {
            return element.attr("content");
        }

        element = doc.selectFirst("meta[name=\"" + property + "\"]");
        if (element != null) {
            return element.attr("content");
        }

        return null;
    }

    /**
     * 创建备用数据（当无法获取元数据时）
     *
     * @param uri URI 对象
     * @param urlString URL 字符串
     * @return Open Graph 数据
     */
    private static OpenGraphData createFallbackData(URI uri, String urlString) {
        OpenGraphData data = new OpenGraphData();
        data.setTitle(uri.getHost());
        data.setDescription(urlString);
        data.setUrl(urlString);
        data.setSiteName(uri.getHost());
        return data;
    }

    /**
     * 从字符串创建备用数据
     *
     * @param urlString URL 字符串
     * @return Open Graph 数据
     */
    private static OpenGraphData createFallbackDataFromString(String urlString) {
        OpenGraphData data = new OpenGraphData();
        data.setTitle(urlString);
        data.setDescription(urlString);
        data.setUrl(urlString);
        return data;
    }

    /**
     * 检测 URL 是否为图片
     *
     * @param urlString URL 字符串
     * @return 是否为图片
     */
    public static boolean detectImageUrl(String urlString) {
        try {
            URI uri = new URI(urlString);
            HttpRequest request =
                    HttpRequest.newBuilder()
                            .uri(uri)
                            .timeout(Duration.ofSeconds(3))
                            .header(
                                    "User-Agent",
                                    "Mozilla/5.0 (compatible; JavaClient/1.0; +https://cline.bot)")
                            .method("HEAD", HttpRequest.BodyPublishers.noBody())
                            .build();

            HttpResponse<Void> response =
                    httpClient.send(request, HttpResponse.BodyHandlers.discarding());
            String contentType = response.headers().firstValue("content-type").orElse("");
            if (contentType.startsWith("image/")) {
                return true;
            }
        } catch (Exception e) {
            log.debug("Error detecting image URL: {}", urlString, e);
        }
        return IMAGE_EXTENSION_PATTERN.matcher(urlString).find();
    }
}
