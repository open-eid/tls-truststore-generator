package org.digidoc4j.utils.tlsgenerator.url;

import org.digidoc4j.utils.tlsgenerator.UrlsInputSource;

import java.net.URL;
import java.util.Arrays;
import java.util.stream.Stream;

public abstract class AbstractUrlsInputSource implements UrlsInputSource {

    private final URL[] urls;

    protected AbstractUrlsInputSource(final Stream<URL> urls) {
        this.urls = urls.filter(UrlUtils.statefulDistinctFilter()).toArray(URL[]::new);
    }

    @Override
    public Stream<URL> stream() {
        return Arrays.stream(urls);
    }

}
