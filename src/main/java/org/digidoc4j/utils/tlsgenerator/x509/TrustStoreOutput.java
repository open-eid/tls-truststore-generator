package org.digidoc4j.utils.tlsgenerator.x509;

import org.digidoc4j.utils.tlsgenerator.cli.CommandLineArgument;
import org.digidoc4j.utils.tlsgenerator.cli.CommandLineInterface;
import org.digidoc4j.utils.tlsgenerator.exception.TlsGeneratorTechnicalException;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;

public final class TrustStoreOutput {

    private static final String DEFAULT_TRUSTSTORE_TYPE = "PKCS12";

    public static void saveCertificatesToTruststore(final Set<X509Certificate> certificates, final Map<CommandLineArgument, List<String>> options) {
        final String type = options.getOrDefault(CommandLineArgument.TYPE, Collections.singletonList(DEFAULT_TRUSTSTORE_TYPE)).get(0);
        final String path = options.get(CommandLineArgument.OUT).get(0);
        final char[] password = getTrustStorePassword(options);

        try {
            final KeyStore trustStore = KeyStore.getInstance(type);
            trustStore.load(null, password);

            certificates.forEach(c -> addCertificateToTrustStore(trustStore, c));

            try (OutputStream out = new FileOutputStream(path)) {
                trustStore.store(out, password);
            }
        } catch (KeyStoreException | CertificateException | NoSuchAlgorithmException | IOException e) {
            throw new TlsGeneratorTechnicalException("Failed to create truststore: " + e.getMessage(), e);
        } finally {
            Arrays.fill(password, '\0');
        }
    }

    private static void addCertificateToTrustStore(final KeyStore trustStore, final X509Certificate certificate) {
        final String alias = X509Utils.getCertificateSimpleName(certificate);
        try {
            trustStore.setCertificateEntry(alias, certificate);
        } catch (KeyStoreException e) {
            throw new TlsGeneratorTechnicalException("Failed to add certificate to truststore: " + alias, e);
        }
    }

    private static char[] getTrustStorePassword(final Map<CommandLineArgument, List<String>> options) {
        if (options.containsKey(CommandLineArgument.PASSWORD)) {
            return options.get(CommandLineArgument.PASSWORD).get(0).toCharArray();
        }
        return CommandLineInterface.getConsoleOrFail()
                .readPassword("Input truststore password: ");
    }

    private TrustStoreOutput() {}

}
