package org.digidoc4j.utils.tlsgenerator.x509;

import java.security.cert.X509Certificate;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public final class X509Utils {

    private static final String DN_SPLITTER_REGEX = "[\\s]*,[\\s]*";
    private static final String CN_PREFIX = "CN=";

    public static List<String> getCertificatesSimpleNames(final List<X509Certificate> certificates) {
        return certificates.stream()
                .map(X509Utils::getCertificateSimpleName)
                .collect(Collectors.toList());
    }

    public static String getCertificateSimpleName(final X509Certificate certificate) {
        final String subjectDn = certificate.getSubjectDN().getName();
        return Arrays.stream(subjectDn.split(DN_SPLITTER_REGEX))
                .filter(e -> e.startsWith(CN_PREFIX))
                .map(e -> e.substring(CN_PREFIX.length()))
                .findFirst()
                .orElse(subjectDn);
    }

    private X509Utils() {}

}
