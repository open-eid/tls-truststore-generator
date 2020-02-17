package org.digidoc4j.utils.tlsgenerator.lotl;

import org.digidoc4j.utils.tlsgenerator.exception.TlsGeneratorInputException;
import org.digidoc4j.utils.tlsgenerator.extract.RedirectedUrlChainExtractor;
import org.digidoc4j.utils.tlsgenerator.url.AbstractUrlsInputSource;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.List;
import java.util.Optional;
import java.util.function.UnaryOperator;
import java.util.stream.Stream;

public final class LotlUrlsInputSource extends AbstractUrlsInputSource {

    public LotlUrlsInputSource(final List<String> lotlUrlStrings, final Optional<String> tlsProtocol, final boolean followRedirects) {
        super(extractLotlUrls(
                lotlUrlStrings.stream().distinct().map(LotlUrlsInputSource::parseURL),
                tlsProtocol, followRedirects
        ));
    }

    private static Stream<URL> extractLotlUrls(final Stream<URL> lotlUrls, final Optional<String> tlsProtocol, final boolean followRedirects) {
        final LotlParser lotlParser = new LotlParser(tlsProtocol);
        return lotlUrls
                .map(getRedirectionHandler(tlsProtocol, followRedirects))
                .flatMap(lotlUrl -> Stream.concat(Stream.of(lotlUrl),
                        lotlParser.parseLotl(lotlUrl).stream().map(TslPointer::getUrl)));
    }

    private static URL parseURL(final String urlString) {
        try {
            return new URL(urlString);
        } catch (MalformedURLException e) {
            final String message = urlString.trim().isEmpty() ? "Empty input" : ("Invalid input: " + urlString);
            throw new TlsGeneratorInputException("Failed to parse LOTL URL: " + message, e);
        }
    }

    private static UnaryOperator<URL> getRedirectionHandler(final Optional<String> tlsProtocol, final boolean followRedirects) {
        if (followRedirects) {
            final RedirectedUrlChainExtractor extractor = new RedirectedUrlChainExtractor(tlsProtocol);
            return url -> {
                final List<URL> redirectionChain = extractor.extractRedirectionUrlChain(url);
                return redirectionChain.isEmpty() ? url : redirectionChain.get(redirectionChain.size() - 1);
            };
        } else {
            return UnaryOperator.identity();
        }
    }

}
