package org.digidoc4j.utils.tlsgenerator.url;

import org.digidoc4j.utils.tlsgenerator.exception.TlsGeneratorInputException;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.List;

public final class HttpUrlsInputSource extends AbstractUrlsInputSource {

    public HttpUrlsInputSource(final List<String> urlStrings) {
        super(urlStrings.stream().distinct()
                .map(HttpUrlsInputSource::parseURL)
                .peek(HttpUrlsInputSource::validateUrlProtocol)
        );
    }

    private static URL parseURL(final String urlString) {
        try {
            return new URL(urlString);
        } catch (MalformedURLException e) {
            final String message = urlString.trim().isEmpty() ? "Empty input" : ("Invalid input: " + urlString);
            throw new TlsGeneratorInputException("Failed to parse URL: " + message, e);
        }
    }

    private static void validateUrlProtocol(final URL url) {
        if (!UrlUtils.isHttpUrl(url) && !UrlUtils.isHttpsUrl(url)) {
            throw new TlsGeneratorInputException("Invalid input URL protocol: " + url.getProtocol());
        }
    }

}
