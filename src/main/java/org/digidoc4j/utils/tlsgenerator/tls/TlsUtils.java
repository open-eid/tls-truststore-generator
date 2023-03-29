package org.digidoc4j.utils.tlsgenerator.tls;

import org.digidoc4j.utils.tlsgenerator.exception.TlsGeneratorInputException;
import org.digidoc4j.utils.tlsgenerator.exception.TlsGeneratorTechnicalException;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManager;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;

public final class TlsUtils {

    public static TlsProtocol getValidTlsProtocol(final String protocol) {
        if (protocol != null) {
            for (final TlsProtocol tlsProtocol : TlsProtocol.values()) {
                if (tlsProtocol.getFormalName().equals(protocol)) return tlsProtocol;
            }
            throw new TlsGeneratorInputException("Unsupported TLS protocol: " + protocol);
        } else {
            return TlsProtocol.TLSv13;
        }
    }

    public static SSLSocketFactory createSocketFactory(
            final TlsProtocol tlsProtocol, final TrustManager... trustManagers
    ) throws TlsGeneratorTechnicalException {
        try {
            final SSLContext sslContext = SSLContext.getInstance(tlsProtocol.getFormalName());
            sslContext.init(null, trustManagers, null);
            return sslContext.getSocketFactory();
        } catch (NoSuchAlgorithmException | KeyManagementException e) {
            throw new TlsGeneratorTechnicalException("Failed to initialize socket factory: " + e.getMessage(), e);
        }
    }

    private TlsUtils() {}

}
