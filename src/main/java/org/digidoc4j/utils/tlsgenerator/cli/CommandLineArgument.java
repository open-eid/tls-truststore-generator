package org.digidoc4j.utils.tlsgenerator.cli;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public enum CommandLineArgument {

    URL(CommandLineArgumentGroup.IN, 1, Integer.MAX_VALUE, "--url"),
    LOTL(CommandLineArgumentGroup.IN, 1, Integer.MAX_VALUE, "--lotl-url", "--lotl"),
    OUT(CommandLineArgumentGroup.OUT, 1, "--out", "-o"),

    TYPE(1, "--type", "-t"),
    PASSWORD(1, "--password", "-p"),
    TLS_PROTOCOL(1, "--tls-protocol", "--tls"),
    EXTRACT_FROM_CHAIN(1, Integer.MAX_VALUE, "--extract-from-chain"),
    CONTINUE_ON_ERROR("--continue-on-error"),
    FOLLOW_REDIRECTS("--follow-redirects"),

    HELP("--help", "-h");

    private final CommandLineArgumentGroup group;
    private final int minParameterCount;
    private final int maxParameterCount;
    private final String[] formalNames;

    CommandLineArgument(final String... formalNames) {
        this(null, 0, formalNames);
    }

    CommandLineArgument(final int parameterCount, final String... formalNames) {
        this(null, parameterCount, formalNames);
    }

    CommandLineArgument(final int minParameterCount, final int maxParameterCount, final String... formalNames) {
        this(null, minParameterCount, maxParameterCount, formalNames);
    }

    CommandLineArgument(final CommandLineArgumentGroup group, final int parameterCount, final String... formalNames) {
        this(group, parameterCount, parameterCount, formalNames);
    }

    CommandLineArgument(final CommandLineArgumentGroup group, final int minParameterCount, final int maxParameterCount, final String... formalNames) {
        this.group = group;
        this.minParameterCount = minParameterCount;
        this.maxParameterCount = maxParameterCount;
        this.formalNames = formalNames;
    }

    public boolean isInGroup(final CommandLineArgumentGroup group) {
        return this.group == group;
    }

    public List<String> getFormalNames() {
        return Collections.unmodifiableList(Arrays.asList(formalNames));
    }

    public int getMinParameterCount() {
        return minParameterCount;
    }

    public int getMaxParameterCount() {
        return maxParameterCount;
    }

}
