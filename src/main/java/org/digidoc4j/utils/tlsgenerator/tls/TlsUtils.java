package org.digidoc4j.utils.tlsgenerator.tls;

import org.digidoc4j.utils.tlsgenerator.exception.TlsGeneratorInputException;
import org.digidoc4j.utils.tlsgenerator.exception.TlsGeneratorTechnicalException;

import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManager;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.util.Optional;

public final class TlsUtils {

    public static final String PROTOCOL_TLS = "TLS";
    public static final String PROTOCOL_TLSv10 = "TLSv1";
    public static final String PROTOCOL_TLSv11 = "TLSv1.1";
    public static final String PROTOCOL_TLSv12 = "TLSv1.2";

    private static final String[] SUPPORTED_PROTOCOLS = {
            PROTOCOL_TLS, PROTOCOL_TLSv10, PROTOCOL_TLSv11, PROTOCOL_TLSv12
    };

    public static String getValidTlsProtocol(final Optional<String> protocol) {
        if (protocol.isPresent()) {
            validateTlsProtocolSupport(protocol.get());
            return protocol.get();
        } else {
            return PROTOCOL_TLSv12;
        }
    }

    public static void validateTlsProtocolSupport(final String protocol) {
        for (final String supportedProtocol : SUPPORTED_PROTOCOLS) {
            if (supportedProtocol.equals(protocol)) return;
        }
        throw new TlsGeneratorInputException("Unsupported TLS protocol: " + protocol);
    }

    public static SSLSocketFactory createSocketFactory(
            final Optional<String> tlsProtocol, final TrustManager... trustManagers
    ) throws TlsGeneratorTechnicalException {
        try {
            final SSLContext sslContext = SSLContext.getInstance(getValidTlsProtocol(tlsProtocol));
            sslContext.init(null, trustManagers, null);
            return sslContext.getSocketFactory();
        } catch (NoSuchAlgorithmException | KeyManagementException e) {
            throw new TlsGeneratorTechnicalException("Failed to initialize socket factory: " + e.getMessage(), e);
        }
    }

    private TlsUtils() {}

}
