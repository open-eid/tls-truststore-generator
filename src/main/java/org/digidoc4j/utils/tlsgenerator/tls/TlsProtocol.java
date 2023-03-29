package org.digidoc4j.utils.tlsgenerator.tls;

public enum TlsProtocol {

    TLS("TLS"),
    TLSv10("TLSv1"),
    TLSv11("TLSv1.1"),
    TLSv12("TLSv1.2"),
    TLSv13("TLSv1.3");

    private final String formalName;

    TlsProtocol(String formalName) {
        this.formalName = formalName;
    }

    public String getFormalName() {
        return formalName;
    }

}
