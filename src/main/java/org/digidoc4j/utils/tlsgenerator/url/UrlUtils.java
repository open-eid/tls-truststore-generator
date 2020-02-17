package org.digidoc4j.utils.tlsgenerator.url;

import java.net.URL;

public final class UrlUtils {

    public static final String HTTP_URL_PROTOCOL = "http";
    public static final String HTTPS_URL_PROTOCOL = "https";

    public static boolean isHttpUrlProtocol(final String urlProtocol) {
        return HTTP_URL_PROTOCOL.equalsIgnoreCase(urlProtocol);
    }

    public static boolean isHttpUrl(final URL url) {
        return isHttpUrlProtocol(url.getProtocol());
    }

    public static boolean isHttpsUrlProtocol(final String urlProtocol) {
        return HTTPS_URL_PROTOCOL.equalsIgnoreCase(urlProtocol);
    }

    public static boolean isHttpsUrl(final URL url) {
        return isHttpsUrlProtocol(url.getProtocol());
    }

    private UrlUtils() {}

}
