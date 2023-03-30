package org.digidoc4j.utils.tlsgenerator;

import org.digidoc4j.utils.tlsgenerator.cli.CommandLineArgument;
import org.digidoc4j.utils.tlsgenerator.cli.CommandLineInterface;
import org.digidoc4j.utils.tlsgenerator.exception.TlsGeneratorException;
import org.digidoc4j.utils.tlsgenerator.exception.TlsGeneratorInputException;
import org.digidoc4j.utils.tlsgenerator.extract.CertificateChainExtractor;
import org.digidoc4j.utils.tlsgenerator.extract.RedirectedUrlChainExtractor;
import org.digidoc4j.utils.tlsgenerator.lotl.LotlUrlsInputSource;
import org.digidoc4j.utils.tlsgenerator.tls.CertificateChainFetcher;
import org.digidoc4j.utils.tlsgenerator.tls.TlsProtocol;
import org.digidoc4j.utils.tlsgenerator.tls.TlsUtils;
import org.digidoc4j.utils.tlsgenerator.url.HttpUrlsInputSource;
import org.digidoc4j.utils.tlsgenerator.url.UrlUtils;
import org.digidoc4j.utils.tlsgenerator.x509.TrustStoreOutput;
import org.digidoc4j.utils.tlsgenerator.x509.X509Utils;

import java.net.URL;
import java.nio.file.InvalidPathException;
import java.nio.file.Paths;
import java.security.cert.X509Certificate;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.UnaryOperator;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public final class TlsTrustStoreGenerator {

    public static void main(final String[] args) {
        try {
            final Map<CommandLineArgument, List<String>> options = CommandLineInterface.parseArguments(args);
            verifyArgumentsParameters(options);

            final Set<X509Certificate> certificates = fetchCertificates(Stream.of(
                    new HttpUrlsInputSource(options.getOrDefault(CommandLineArgument.URL, Collections.emptyList())),
                    new LotlUrlsInputSource(options.getOrDefault(CommandLineArgument.LOTL, Collections.emptyList()),
                            getTlsProtocol(options), options.containsKey(CommandLineArgument.FOLLOW_REDIRECTS))
            ).flatMap(UrlsInputSource::stream).filter(UrlUtils.statefulDistinctFilter()), options);

            System.out.println();
            System.out.println("Saving truststore: " + options.get(CommandLineArgument.OUT).get(0));
            TrustStoreOutput.saveCertificatesToTruststore(certificates, options);
            System.out.printf("Done%n").println();
        } catch (TlsGeneratorInputException exception) {
            outputErrorMessage(exception);
            System.out.println();
            CommandLineInterface.printHelp();
        } catch (TlsGeneratorException exception) {
            outputErrorMessage(exception);
            System.out.println();
        }
    }

    private static Set<X509Certificate> fetchCertificates(final Stream<URL> urls, final Map<CommandLineArgument, List<String>> options) {
        System.out.println();
        System.out.println("Fetching certificate chains...");

        final List<List<X509Certificate>> certificateChains = urls
                .flatMap(getUrlExtractor(options))
                .filter(UrlUtils::isHttpsUrl)
                .flatMap(getCertificateChainFetcher(options))
                .distinct()
                .collect(Collectors.toList());

        System.out.println();
        System.out.println("Extracting certificates...");

        return certificateChains.stream()
                .flatMap(getChainExtractor(options))
                .collect(Collectors.toSet());
    }

    private static Function<URL, Stream<URL>> getUrlExtractor(final Map<CommandLineArgument, List<String>> options) {
        if (options.containsKey(CommandLineArgument.FOLLOW_REDIRECTS)) {
            final RedirectedUrlChainExtractor redirectedUrlChainExtractor = new RedirectedUrlChainExtractor(getTlsProtocol(options), getErrorHandler(options));
            return url -> redirectedUrlChainExtractor.extractRedirectionUrlChain(url).stream();
        } else {
            return Stream::of;
        }
    }

    private static Function<URL, Stream<List<X509Certificate>>> getCertificateChainFetcher(final Map<CommandLineArgument, List<String>> options) {
        final CertificateChainFetcher certificateChainFetcher = new CertificateChainFetcher(getTlsProtocol(options));
        final Consumer<TlsGeneratorException> errorHandler = getErrorHandler(options);
        return url -> {
            try {
                final List<X509Certificate> chain = certificateChainFetcher.fetchCertificateChainFrom(url);
                System.out.println(url + ": " + X509Utils.getCertificatesSimpleNames(chain));
                return Stream.of(chain);
            } catch (TlsGeneratorException exception) {
                errorHandler.accept(exception);
                return Stream.empty();
            }
        };
    }

    private static Function<List<X509Certificate>, Stream<X509Certificate>> getChainExtractor(final Map<CommandLineArgument, List<String>> options) {
        final UnaryOperator<List<X509Certificate>> extractor = options.containsKey(CommandLineArgument.EXTRACT_FROM_CHAIN)
                ? CertificateChainExtractor.createCertificateChainExtractor(options.get(CommandLineArgument.EXTRACT_FROM_CHAIN))
                : UnaryOperator.identity();
        return chain -> {
            final List<X509Certificate> extractedList = extractor.apply(chain);
            System.out.println(X509Utils.getCertificatesSimpleNames(chain) + " -> " + X509Utils.getCertificatesSimpleNames(extractedList));
            return extractedList.stream();
        };
    }

    private static Consumer<TlsGeneratorException> getErrorHandler(final Map<CommandLineArgument, List<String>> options) {
        return options.containsKey(CommandLineArgument.CONTINUE_ON_ERROR)
                ? TlsTrustStoreGenerator::outputErrorMessage
                : exception -> { throw exception; };
    }

    private static void verifyArgumentsParameters(final Map<CommandLineArgument, List<String>> options) {
        if (options.containsKey(CommandLineArgument.EXTRACT_FROM_CHAIN)) {
            // Try to create a certificate chain extractor in order to verify the input parameters
            CertificateChainExtractor.createCertificateChainExtractor(options.get(CommandLineArgument.EXTRACT_FROM_CHAIN));
        }
        if (options.containsKey(CommandLineArgument.TLS_PROTOCOL)) {
            TlsUtils.getValidTlsProtocol(options.get(CommandLineArgument.TLS_PROTOCOL).get(0));
        }
        try {
            Paths.get(options.get(CommandLineArgument.OUT).get(0));
        } catch (InvalidPathException e) {
            throw new TlsGeneratorInputException("Invalid output parameter: " + e.getMessage(), e);
        }
    }

    private static TlsProtocol getTlsProtocol(final Map<CommandLineArgument, List<String>> options) {
        if (options.containsKey(CommandLineArgument.TLS_PROTOCOL)) {
            return TlsUtils.getValidTlsProtocol(options.get(CommandLineArgument.TLS_PROTOCOL).get(0));
        } else {
            return TlsUtils.getValidTlsProtocol(null);
        }
    }

    private static void outputErrorMessage(final TlsGeneratorException exception) {
        System.out.flush(); // Flush stdout before writing anything to stderr
        System.err.println(exception.getMessage());
        System.err.flush();
    }

}
