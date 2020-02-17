package org.digidoc4j.utils.tlsgenerator.lotl;

import org.digidoc4j.utils.tlsgenerator.Resources;
import org.digidoc4j.utils.tlsgenerator.exception.TlsGeneratorParseException;
import org.digidoc4j.utils.tlsgenerator.exception.TlsGeneratorTechnicalException;
import org.digidoc4j.utils.tlsgenerator.tls.NoOpTrustManager;
import org.digidoc4j.utils.tlsgenerator.tls.TlsUtils;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import javax.net.ssl.HttpsURLConnection;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

public final class LotlParser {

    private static final String LOTL_ROOT_ELEMENT = "TrustServiceStatusList";
    private static final String INFORMATION_ELEMENT = "SchemeInformation";
    private static final String TSL_POINTERS = "PointersToOtherTSL";
    private static final String TSL_POINTER = "OtherTSLPointer";

    private final String protocol;

    public LotlParser(final Optional<String> tlsProtocol) {
        protocol = TlsUtils.getValidTlsProtocol(tlsProtocol);
    }

    public List<TslPointer> parseLotl(final URL lotlUrl) {
        final Element lotlRootElement = loadLotlDocumentRoot(lotlUrl);
        switch (lotlRootElement.getTagName()) {
            case LOTL_ROOT_ELEMENT:
                return parseTslPointers(lotlRootElement);
            default:
                throw new TlsGeneratorParseException("Failed to parse LOTL: " + lotlUrl + ": No recognized root element found");
        }
    }

    private Element loadLotlDocumentRoot(final URL lotlUrl) {
        try (InputStream lotlInputStream = openLotlStream(lotlUrl)) {
            final DocumentBuilderFactory documentBuilderFactory = DocumentBuilderFactory.newInstance();
            final DocumentBuilder documentBuilder = documentBuilderFactory.newDocumentBuilder();
            final Document document = documentBuilder.parse(lotlInputStream);
            final Element rootElement = document.getDocumentElement();
            if (rootElement == null) {
                throw new TlsGeneratorParseException("No root element found in LOTL: " + lotlUrl);
            } else {
                return rootElement;
            }
        } catch (ParserConfigurationException e) {
            throw new TlsGeneratorTechnicalException("Failed to initialize LOTL parser: " + e.getMessage(), e);
        } catch (SAXException e) {
            throw new TlsGeneratorParseException("Failed to parse LOTL: " + lotlUrl + ": " + e.getMessage(), e);
        } catch (IOException e) {
            throw new TlsGeneratorTechnicalException("Failed to load LOTL: " + lotlUrl + ": " + e.getMessage(), e);
        }
    }

    private InputStream openLotlStream(final URL lotlUrl) {
        try {
            final HttpURLConnection httpUrlConnection = (HttpURLConnection) lotlUrl.openConnection();
            if (httpUrlConnection instanceof HttpsURLConnection) {
                ((HttpsURLConnection) httpUrlConnection).setSSLSocketFactory(
                        TlsUtils.createSocketFactory(Optional.of(protocol), new NoOpTrustManager())
                );
            }

            httpUrlConnection.setInstanceFollowRedirects(false);
            httpUrlConnection.setDoOutput(false);
            httpUrlConnection.setDoInput(true);
            httpUrlConnection.connect();

            return httpUrlConnection.getInputStream();
        } catch (IOException e) {
            final String message = String.format("Failed to open connection to %s: %s", lotlUrl, e.getMessage());
            throw new TlsGeneratorTechnicalException(message, e);
        }
    }

    private static List<TslPointer> parseTslPointers(final Element lotlRootElement) {
        return findElements(lotlRootElement, Arrays.asList(INFORMATION_ELEMENT, TSL_POINTERS, TSL_POINTER))
                .map(LotlParser::parseTslPointer).collect(Collectors.toList());
    }

    private static Stream<Element> findElements(final Element root, final List<String> path) {
        final NodeList children = root.getElementsByTagName(path.get(0));
        final Stream<Element> elements = IntStream
                .range(0, children.getLength())
                .mapToObj(children::item)
                .map(child -> (Element) child);

        if (path.size() > 1) {
            return elements.flatMap(e -> findElements(e, path.subList(1, path.size())));
        } else {
            return elements;
        }
    }

    private static TslPointer parseTslPointer(final Element tslPointerElement) {
        if (!TSL_POINTER.equals(tslPointerElement.getTagName())) {
            throw new IllegalArgumentException();
        }

        final String territory = findElements(tslPointerElement,
                Arrays.asList("AdditionalInformation", "OtherInformation", "SchemeTerritory"))
                .map(Node::getTextContent)
                .filter(Objects::nonNull)
                .findFirst()
                .orElse(Resources.EMPTY_STRING);

        final URL url = findElements(tslPointerElement, Arrays.asList("TSLLocation"))
                .map(Node::getTextContent)
                .filter(Objects::nonNull)
                .map(LotlParser::parseTslLocation)
                .findFirst()
                .orElseThrow(() -> new IllegalStateException());

        return new TslPointer(territory, url);
    }

    private static URL parseTslLocation(final String tslLocation) {
        try {
            return new URL(tslLocation);
        } catch (MalformedURLException e) {
            throw new TlsGeneratorParseException("Invalid TSL location: " + tslLocation, e);
        }
    }

}
