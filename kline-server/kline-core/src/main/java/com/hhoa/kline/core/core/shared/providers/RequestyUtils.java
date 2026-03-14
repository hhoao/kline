package com.hhoa.kline.core.core.shared.providers;

import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;

public class RequestyUtils {
    public static final String REQUESTY_BASE_URL = "https://router.requesty.ai/v1";

    public enum URLType {
        ROUTER("router"),
        APP("app"),
        API("api");

        private final String value;

        URLType(String value) {
            this.value = value;
        }

        public String getValue() {
            return value;
        }
    }

    private static String replaceCname(String baseUrl, URLType type) {
        if (type == URLType.ROUTER) {
            return baseUrl;
        } else {
            return baseUrl.replace("router", type.getValue()).replace("v1", "");
        }
    }

    public static URL toRequestyServiceUrl(String baseUrl, URLType service) {
        String url = replaceCname(baseUrl != null ? baseUrl : REQUESTY_BASE_URL, service);

        try {
            return new URI(url).toURL();
        } catch (URISyntaxException | MalformedURLException e) {
            return null;
        }
    }

    public static URL toRequestyServiceUrl(String baseUrl) {
        return toRequestyServiceUrl(baseUrl, URLType.ROUTER);
    }

    public static String toRequestyServiceStringUrl(String baseUrl, URLType service) {
        URL url = toRequestyServiceUrl(baseUrl, service);
        return url != null ? url.toString() : null;
    }

    public static String toRequestyServiceStringUrl(String baseUrl) {
        return toRequestyServiceStringUrl(baseUrl, URLType.ROUTER);
    }
}
