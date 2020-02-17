package org.digidoc4j.utils.tlsgenerator;

import java.net.URL;
import java.util.stream.Stream;

@FunctionalInterface
public interface UrlsInputSource {

    Stream<URL> stream();

}
