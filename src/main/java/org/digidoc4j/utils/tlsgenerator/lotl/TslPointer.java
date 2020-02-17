package org.digidoc4j.utils.tlsgenerator.lotl;

import java.net.URL;

public final class TslPointer {

    private final String territory;
    private final URL url;

    public TslPointer(final String territory, final URL url) {
        this.territory = territory;
        this.url = url;
    }

    public String getTerritory() {
        return territory;
    }

    public URL getUrl() {
        return url;
    }

}
