package org.digidoc4j.utils.tlsgenerator.extract;

import org.digidoc4j.utils.tlsgenerator.exception.TlsGeneratorException;
import org.digidoc4j.utils.tlsgenerator.exception.TlsGeneratorTechnicalException;
import org.digidoc4j.utils.tlsgenerator.tls.NoOpTrustManager;
import org.digidoc4j.utils.tlsgenerator.tls.TlsProtocol;
import org.digidoc4j.utils.tlsgenerator.tls.TlsUtils;
import org.digidoc4j.utils.tlsgenerator.url.UrlUtils;

import javax.net.ssl.HttpsURLConnection;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.ProtocolException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;

public final class RedirectedUrlChainExtractor {

    private static final String REQUEST_METHOD = "GET";
    private static final String REDIRECT_TARGET_HEADER = "Location";

    private final TlsProtocol protocol;
    private final Consumer<TlsGeneratorException> handler;

    public RedirectedUrlChainExtractor(final TlsProtocol tlsProtocol, final Consumer<TlsGeneratorException> errorHandler) {
        protocol = Objects.requireNonNull(tlsProtocol);
        handler = Objects.requireNonNull(errorHandler);
    }

    public List<URL> extractRedirectionUrlChain(final URL url) {
        final List<URL> accumulatedUrls = new ArrayList<>();
        try {
            followRedirectsRecursively(url, accumulatedUrls);
        } catch (TlsGeneratorTechnicalException exception) {
            handler.accept(exception);
        }
        return accumulatedUrls;
    }

    private void followRedirectsRecursively(final URL currentUrl, final List<URL> urlAccumulator) {
        final URL nextUrl = followRedirectAndGetNextUrl(currentUrl, urlAccumulator);
        if (nextUrl == null || UrlUtils.contains(urlAccumulator, nextUrl)) {
            return; // Break the recursion if there is no next URL or this URL has already been followed
        }
        followRedirectsRecursively(nextUrl, urlAccumulator);
    }

    private URL followRedirectAndGetNextUrl(final URL currentUrl, final List<URL> urlAccumulator) {
        try {
            final HttpURLConnection httpURLConnection = (HttpURLConnection) currentUrl.openConnection();

            try {
                httpURLConnection.setRequestMethod(REQUEST_METHOD);
                httpURLConnection.setInstanceFollowRedirects(false);
                if (httpURLConnection instanceof HttpsURLConnection) {
                    configureForTls((HttpsURLConnection) httpURLConnection);
                }
                httpURLConnection.connect();

                // Only after the connection was opened successfully,
                //  it is safe to add the URL for subsequent certificate fetching
                urlAccumulator.add(currentUrl);

                return getNextUrl(httpURLConnection);
            } catch (ProtocolException e) {
                throw new TlsGeneratorTechnicalException("Protocol error: " + e.getMessage(), e);
            } finally {
                httpURLConnection.disconnect();
            }
        } catch (IOException e) {
            final String message = String.format("Failed to open connection to %s: %s", currentUrl, e.getMessage());
            throw new TlsGeneratorTechnicalException(message, e);
        }
    }

    private void configureForTls(final HttpsURLConnection httpsURLConnection) {
        httpsURLConnection.setSSLSocketFactory(TlsUtils.createSocketFactory(protocol, new NoOpTrustManager()));
    }

    private static URL getNextUrl(final HttpURLConnection httpURLConnection) {
        try {
            final int responseCode = httpURLConnection.getResponseCode();

            if (responseCode / 100 != 3) {
                return null; // Not a HTTP 3XX response
            }

            final URL connectedUrl = httpURLConnection.getURL();
            final String redirectTargetHeaderValue = httpURLConnection.getHeaderField(REDIRECT_TARGET_HEADER);

            if (redirectTargetHeaderValue == null || redirectTargetHeaderValue.trim().isEmpty()) {
                String message = String.format("No %s header provided for %d response", REDIRECT_TARGET_HEADER, responseCode);
                throw new TlsGeneratorTechnicalException(message);
            }

            return toUrl(connectedUrl, responseCode, redirectTargetHeaderValue);
        } catch (IOException e) {
            final URL connectionUrl = httpURLConnection.getURL();
            final String message = String.format("Failed to get response from %s: %s", connectionUrl, e.getMessage());
            throw new TlsGeneratorTechnicalException(message, e);
        }
    }

    private static URL toUrl(final URL currentUrl, final int responseCode, final String redirectTargetHeader) {
        try {
            URI newUri = new URI(redirectTargetHeader);

            if (!newUri.isAbsolute()) {
                newUri = currentUrl.toURI().resolve(newUri);
                System.out.println(String.format("Resolved redirect target \"%s\" to \"%s\"", redirectTargetHeader, newUri));
            }
            if (!isProtocolSupported(newUri.getScheme())) {
                throw new TlsGeneratorTechnicalException("Failed to resolve redirect URL from " + redirectTargetHeader);
            }

            final URL newUrl = newUri.toURL();
            System.out.println(String.format("%s -(%d)-> %s", currentUrl, responseCode, newUrl));
            return newUrl;
        } catch (URISyntaxException e) {
            String message = String.format("Invalid %s header value: %s", REDIRECT_TARGET_HEADER, redirectTargetHeader);
            throw new TlsGeneratorTechnicalException(message, e);
        } catch (MalformedURLException e) {
            throw new TlsGeneratorTechnicalException("Failed to form URL: " + redirectTargetHeader, e);
        }
    }

    private static boolean isProtocolSupported(final String protocol) {
        return UrlUtils.isHttpUrlProtocol(protocol) || UrlUtils.isHttpsUrlProtocol(protocol);
    }

}
