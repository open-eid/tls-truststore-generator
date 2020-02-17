package org.digidoc4j.utils.tlsgenerator.lotl;

import org.digidoc4j.utils.tlsgenerator.exception.TlsGeneratorInputException;
import org.digidoc4j.utils.tlsgenerator.url.AbstractUrlsInputSource;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

public final class LotlUrlsInputSource extends AbstractUrlsInputSource {

    public LotlUrlsInputSource(final List<String> lotlUrlStrings, final Optional<String> tlsProtocol) {
        super(extractLotlUrls(lotlUrlStrings.stream().distinct().map(LotlUrlsInputSource::parseURL), tlsProtocol));
    }

    private static Stream<URL> extractLotlUrls(final Stream<URL> lotlUrls, final Optional<String> tlsProtocol) {
        final LotlParser lotlParser = new LotlParser(tlsProtocol);

        return lotlUrls.flatMap(lotlUrl -> Stream.concat(Stream.of(lotlUrl),
                lotlParser.parseLotl(lotlUrl).stream().map(TslPointer::getUrl)
        ));
    }

    private static URL parseURL(final String urlString) {
        try {
            return new URL(urlString);
        } catch (MalformedURLException e) {
            final String message = urlString.trim().isEmpty() ? "Empty input" : ("Invalid input: " + urlString);
            throw new TlsGeneratorInputException("Failed to parse LOTL URL: " + message, e);
        }
    }

}
