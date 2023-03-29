package org.digidoc4j.utils.tlsgenerator.extract;

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
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

public final class RedirectedUrlChainExtractor {

    private static final String REQUEST_METHOD = "GET";
    private static final String REDIRECT_TARGET_HEADER = "Location";

    private final TlsProtocol protocol;

    public RedirectedUrlChainExtractor(final TlsProtocol tlsProtocol) {
        protocol = Objects.requireNonNull(tlsProtocol);
    }

    public List<URL> extractRedirectionUrlChain(final URL url) {
        final List<URL> accumulatedUrls = new ArrayList<>(Collections.singletonList(url));
        followRedirectsRecursively(accumulatedUrls);
        return accumulatedUrls;
    }

    private void followRedirectsRecursively(final List<URL> urls) {
        final URL currentUrl = urls.get(urls.size() - 1);
        try {
            final Optional<URL> nextUrl = getNextUrl(currentUrl.openConnection());
            if (!nextUrl.isPresent() || UrlUtils.contains(urls, nextUrl.get())) {
                return;
            }

            urls.add(nextUrl.get());
        } catch (IOException e) {
            throw new TlsGeneratorTechnicalException("Failed to open connection: " + currentUrl, e);
        }

        followRedirectsRecursively(urls);
    }

    private Optional<URL> getNextUrl(final URLConnection urlConnection) {
        final HttpURLConnection httpURLConnection = (HttpURLConnection) urlConnection;

        try {
            httpURLConnection.setRequestMethod(REQUEST_METHOD);
            httpURLConnection.setInstanceFollowRedirects(false);
            if (httpURLConnection instanceof HttpsURLConnection) {
                configureForTls((HttpsURLConnection) httpURLConnection);
            }
            httpURLConnection.connect();

            final int responseCode = httpURLConnection.getResponseCode();
            if (responseCode / 100 == 3) {
                final URL connectedUrl = httpURLConnection.getURL();
                final String redirectTargetHeaderValue = httpURLConnection.getHeaderField(REDIRECT_TARGET_HEADER);
                return Optional.of(toUrl(connectedUrl, responseCode, redirectTargetHeaderValue));
            }
        } catch (ProtocolException e) {
            throw new TlsGeneratorTechnicalException("Request method " + REQUEST_METHOD + " not supported", e);
        } catch (IOException e) {
            throw new TlsGeneratorTechnicalException("Failed to get response from " + urlConnection.getURL(), e);
        } finally {
            httpURLConnection.disconnect();
        }

        return Optional.empty();
    }

    private static URL toUrl(final URL currentUrl, final int responseCode, final String redirectTargetHeader) {
        if (redirectTargetHeader == null || redirectTargetHeader.trim().isEmpty()) {
            throw new TlsGeneratorTechnicalException(String.format("No %s header provided for %d response", REDIRECT_TARGET_HEADER, responseCode));
        }
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
            throw new TlsGeneratorTechnicalException(String.format("Invalid %s header value: %s", REDIRECT_TARGET_HEADER, redirectTargetHeader), e);
        } catch (MalformedURLException e) {
            throw new TlsGeneratorTechnicalException("Failed to form URL: " + redirectTargetHeader, e);
        }
    }

    private void configureForTls(final HttpsURLConnection httpsURLConnection) {
        httpsURLConnection.setSSLSocketFactory(TlsUtils.createSocketFactory(protocol, new NoOpTrustManager()));
    }

    private static boolean isProtocolSupported(final String protocol) {
        return UrlUtils.isHttpUrlProtocol(protocol) || UrlUtils.isHttpsUrlProtocol(protocol);
    }

}
