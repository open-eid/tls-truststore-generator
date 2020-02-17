package org.digidoc4j.utils.tlsgenerator.tls;

import javax.net.ssl.X509TrustManager;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

public final class AccumulatingTrustManager implements X509TrustManager {

    private final List<X509Certificate[]> accumulatedClientCertificateChains = new ArrayList<>();
    private final List<X509Certificate[]> accumulatedServerCertificateChains = new ArrayList<>();

    @Override
    public void checkClientTrusted(final X509Certificate[] chain, String authType) {
        accumulatedClientCertificateChains.add(chain);
    }

    @Override
    public void checkServerTrusted(final X509Certificate[] chain, String authType) {
        accumulatedServerCertificateChains.add(chain);
    }

    @Override
    public X509Certificate[] getAcceptedIssuers() {
        return new X509Certificate[0];
    }

    public List<List<X509Certificate>> getAccumulatedClientCertificateChains() {
        return listOfArraysToListOfLists(accumulatedClientCertificateChains);
    }

    public List<List<X509Certificate>> getAccumulatedServerCertificateChains() {
        return listOfArraysToListOfLists(accumulatedServerCertificateChains);
    }

    private static List<List<X509Certificate>> listOfArraysToListOfLists(final List<X509Certificate[]> list) {
        return list.stream()
                .map(array -> Collections.unmodifiableList(Arrays.asList(array)))
                .collect(Collectors.toList());
    }

}
