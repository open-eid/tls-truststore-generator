package org.digidoc4j.utils.tlsgenerator.cli;

import org.digidoc4j.utils.tlsgenerator.Resources;
import org.digidoc4j.utils.tlsgenerator.exception.TlsGeneratorInputException;
import org.digidoc4j.utils.tlsgenerator.exception.TlsGeneratorTechnicalException;

import java.io.Console;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

public final class CommandLineInterface {

    public static Console getConsoleOrFail() {
        final Console console = System.console();
        if (console == null) {
            throw new TlsGeneratorTechnicalException("No console available on this platform");
        } else {
            return console;
        }
    }

    public static Map<CommandLineArgument, List<String>> parseArguments(final String... argumentsToParse) {
        final Map<CommandLineArgument, List<String>> parsedArguments = new LinkedHashMap<>();

        for (int i = 0; i < argumentsToParse.length; ++i) {
            final String argumentToParse = argumentsToParse[i];
            final Optional<CommandLineArgument> matchingArgument = Arrays.stream(CommandLineArgument.values())
                    .filter(arg -> arg.getFormalNames().contains(argumentToParse))
                    .findFirst();

            final List<String> argumentParameters = parseArgumentParameters(
                    matchingArgument.orElseThrow(() -> new TlsGeneratorInputException("Unsupported argument: " + argumentToParse)),
                    argumentsToParse, i + 1);

            parsedArguments.put(matchingArgument.get(), argumentParameters);
            i += argumentParameters.size();
        }

        checkParsedArguments(parsedArguments);
        return Collections.unmodifiableMap(parsedArguments);
    }

    private static List<String> parseArgumentParameters(final CommandLineArgument argument, final String[] source, final int offset) {
        final List<String> parsedArgumentParameters = new ArrayList<>(argument.getMinParameterCount());

        for (int i = 0; i < argument.getMaxParameterCount(); ++i) {
            if (offset + i >= source.length || source[offset + i].startsWith("-")) {
                if (i < argument.getMinParameterCount()) {
                    throw new TlsGeneratorInputException("Not enough parameters provided for argument: " + source[offset - 1]);
                } else {
                    break;
                }
            }

            parsedArgumentParameters.add(source[offset + i]);
        }

        return Collections.unmodifiableList(parsedArgumentParameters);
    }

    private static void checkParsedArguments(final Map<CommandLineArgument, List<String>> parsedArguments) {
        if (Collections.singleton(CommandLineArgument.HELP).equals(parsedArguments.keySet())) {
            printHelp();
            System.exit(0);
        }
        Arrays.stream(CommandLineArgumentGroup.values())
                .filter(CommandLineArgumentGroup::isRequired)
                .filter(group -> parsedArguments.keySet().stream().noneMatch(arg -> arg.isInGroup(group)))
                .findFirst().ifPresent(group -> {
                    final String groupIdentifier = Resources.enumToKebabCase(group);
                    final String groupName = Resources.getCliStringOrDefault("groups." + groupIdentifier + ".name", groupIdentifier);
                    throw new TlsGeneratorInputException("No " + groupName + " argument provided");
                });
    }

    public static void printHelp() {
        System.out.println(Resources.getCliStringOrDefault("program.usage", "Usage:"));
        System.out.println();

        final int maxArgumentWidth = Arrays.stream(CommandLineArgument.values())
                .mapToInt(arg -> formalNamesToString(arg).length())
                .max().getAsInt();

        Arrays.stream(CommandLineArgument.values()).map(arg -> {
            final String argumentHeader = formalNamesToString(arg);
            final String argumentInfo = "  " + argumentHeader +
                    generateStringOfSpaces(maxArgumentWidth - argumentHeader.length() + 2) +
                    Resources.getCliStringOrEmpty("arguments." + Resources.enumToKebabCase(arg) + ".info");
            return Arrays.stream(argumentInfo.split("(\n)|(\r\n)|(\r)"))
                    .collect(Collectors.joining(System.lineSeparator() + generateStringOfSpaces(6)));
        }).forEach(System.out::println);

        System.out.println();
        System.out.flush();
    }

    private static String formalNamesToString(final CommandLineArgument argument) {
        return argument.getFormalNames().stream().collect(Collectors.joining(", "));
    }

    private static String generateStringOfSpaces(final int length) {
        final char[] spaces = new char[length];
        Arrays.fill(spaces, ' ');
        return new String(spaces);
    }

    private CommandLineInterface() {}

}
