package org.digidoc4j.utils.tlsgenerator.url;

import java.net.URL;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import java.util.function.Predicate;

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

    /**
     * Improved URL comparison method that always considers URLs with different hostnames to not be equal.
     * <p>
     * {@link URL#equals(Object)} considers two URLs to be equal if their hostnames resolve to the same IP address.
     * This is not correct when fetching TLS certificates, because different hosts that are hosted on the same address
     * can and should use different TLS certificates.
     *
     * @param url1 the first URL to compare
     * @param url2 the second URL to compare
     *
     * @return whether the two URLs are equal or not
     */
    public static boolean equals(final URL url1, final URL url2) {
        return (url1 == url2) || (
                url1 != null && url2 != null &&
                url1.getHost().equals(url2.getHost()) &&
                url1.equals(url2)
        );
    }

    public static boolean contains(final Collection<URL> urlCollection, final URL urlToCompare) {
        return (urlCollection != null) && urlCollection.stream()
                .anyMatch(url -> equals(url, urlToCompare));
    }

    public static Predicate<URL> statefulDistinctFilter() {
        final List<URL> seenURLs = new LinkedList<>();
        return url -> {
            synchronized (seenURLs) {
                return !contains(seenURLs, url) && seenURLs.add(url);
            }
        };
    }

    private UrlUtils() {}

}
