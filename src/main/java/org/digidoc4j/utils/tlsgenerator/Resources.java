package org.digidoc4j.utils.tlsgenerator;

import java.util.Objects;
import java.util.ResourceBundle;

public final class Resources {

    public static final String EMPTY_STRING = "";

    private static final ResourceBundle CLI_RESOURCE_BUNDLE = ResourceBundle.getBundle("cli");

    public static String getCliStringOrDefault(final String key, final String defaultValue) {
        if (CLI_RESOURCE_BUNDLE.containsKey(Objects.requireNonNull(key))) {
            return CLI_RESOURCE_BUNDLE.getString(key);
        } else {
            return defaultValue;
        }
    }

    public static String getCliStringOrEmpty(final String key) {
        if (CLI_RESOURCE_BUNDLE.containsKey(Objects.requireNonNull(key))) {
            return CLI_RESOURCE_BUNDLE.getString(key);
        } else {
            return EMPTY_STRING;
        }
    }

    public static String enumToKebabCase(final Enum<?> enumValue) {
        return enumValue.name().toLowerCase().replace('_', '-');
    }

    private Resources() {}

}
