package org.digidoc4j.utils.tlsgenerator.cli;

public enum CommandLineArgumentGroup {

    IN(true),
    OUT(true);

    private final boolean required;

    CommandLineArgumentGroup(final boolean required) {
        this.required = required;
    }

    public boolean isRequired() {
        return required;
    }

}
