package org.digidoc4j.utils.tlsgenerator.tls;

import org.digidoc4j.utils.tlsgenerator.exception.TlsGeneratorTechnicalException;

import javax.net.ssl.SSLSocket;
import javax.net.ssl.SSLSocketFactory;
import java.io.IOException;
import java.net.URL;
import java.security.cert.X509Certificate;
import java.util.List;
import java.util.Objects;

public final class CertificateChainFetcher {

    private final TlsProtocol protocol;

    public CertificateChainFetcher(final TlsProtocol tlsProtocol) {
        protocol = Objects.requireNonNull(tlsProtocol);
    }

    public List<X509Certificate> fetchCertificateChainFrom(final URL url) {
        final AccumulatingTrustManager trustManager = new AccumulatingTrustManager();

        try {
            final SSLSocketFactory sslSocketFactory = TlsUtils.createSocketFactory(protocol, trustManager);
            final int port = (url.getPort() > 0) ? url.getPort() : 443;

            try (SSLSocket sslSocket = (SSLSocket) sslSocketFactory.createSocket(url.getHost(), port)) {
                sslSocket.startHandshake();
            }
        } catch (TlsGeneratorTechnicalException | IOException e) {
            final String message = String.format("Failed to load TLS certificate chain from %s: %s", url, e.getMessage());
            throw new TlsGeneratorTechnicalException(message, e);
        }

        final List<List<X509Certificate>> certificateChains = trustManager.getAccumulatedServerCertificateChains();
        assertFetchedCertificateChains(certificateChains, url.toString());
        return certificateChains.get(0);
    }

    private static void assertFetchedCertificateChains(final List<List<X509Certificate>> certificateChains, final String location) {
        final int numberOfCertificateChains = certificateChains.size();
        if (numberOfCertificateChains < 1) {
            throw new TlsGeneratorTechnicalException("Failed to fetch certificate chains from " + location);
        } else if (numberOfCertificateChains > 1) {
            throw new TlsGeneratorTechnicalException("Unexpected amount of certificate chains fetched from " + location);
        }
    }

}
