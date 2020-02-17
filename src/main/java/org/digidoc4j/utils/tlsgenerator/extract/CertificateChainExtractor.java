package org.digidoc4j.utils.tlsgenerator.extract;

import org.digidoc4j.utils.tlsgenerator.Resources;
import org.digidoc4j.utils.tlsgenerator.cli.CommandLineInterface;
import org.digidoc4j.utils.tlsgenerator.exception.TlsGeneratorInputException;
import org.digidoc4j.utils.tlsgenerator.exception.TlsGeneratorParseException;
import org.digidoc4j.utils.tlsgenerator.exception.TlsGeneratorTechnicalException;
import org.digidoc4j.utils.tlsgenerator.x509.X509Utils;

import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.function.UnaryOperator;
import java.util.stream.Collectors;

public final class CertificateChainExtractor {

    public static UnaryOperator<List<X509Certificate>> createCertificateChainExtractor(final List<String> options) {
        final List<Integer> specificIndices = new ArrayList<>();
        final List<Option> specificOptions = new ArrayList<>();

        for (final String option : options) {
            if (Arrays.stream(Option.values()).anyMatch(o -> o.matches(option))) {
                specificOptions.add(Arrays.stream(Option.values()).filter(o -> o.matches(option)).findFirst().get());
            } else if (option.matches("0|([1-9][0-9]*)")){
                specificIndices.add(Integer.parseInt(option));
            } else {
                throw new TlsGeneratorInputException("Unrecognized certificate chain extraction option: " + option);
            }
        }

        if (specificOptions.size() > 0 && specificIndices.size() > 0 || specificOptions.size() > 1) {
            throw new TlsGeneratorInputException("Illegal combination of certificate chain extraction options: " + options);
        } else if (specificOptions.size() == 1) {
            return specificOptions.get(0);
        } else {
            return chain -> extractCertificates(chain, specificIndices);
        }
    }

    private static List<X509Certificate> extractCertificates(final List<X509Certificate> chain, final List<Integer> indices) {
        return indices.stream().peek(index -> {
            if (index >= chain.size()) {
                throw new TlsGeneratorTechnicalException(String.format(
                        "Failed to extract certificate (%d) from chain %s: Index out of bounds",
                        index, X509Utils.getCertificatesSimpleNames(chain)));
            }
        }).map(chain::get).collect(Collectors.toList());
    }

    private enum Option implements UnaryOperator<List<X509Certificate>> {

        ALL {
            @Override
            public List<X509Certificate> apply(final List<X509Certificate> chain) {
                return chain;
            }
        },

        FIRST {
            @Override
            public List<X509Certificate> apply(final List<X509Certificate> chain) {
                return chain.isEmpty() ? Collections.emptyList() : chain.subList(0, 1);
            }
        },

        LAST {
            @Override
            public List<X509Certificate> apply(final List<X509Certificate> chain) {
                return chain.isEmpty() ? Collections.emptyList() : chain.subList(chain.size() - 1, chain.size());
            }
        },

        CA_OR_CERT {
            @Override
            public List<X509Certificate> apply(final List<X509Certificate> chain) {
                return chain.isEmpty() ? Collections.emptyList() : (chain.size() == 1) ?
                        chain.subList(0, 1) : chain.subList(1, 2);
            }
        },

        INTERACTIVE {
            @Override
            public List<X509Certificate> apply(final List<X509Certificate> chain) {
                return chain.isEmpty() ? Collections.emptyList() : CertificateChainExtractor
                        .extractCertificates(chain, queryAndParseCertificatesIndices(chain));
            }
        },

        ;

        public boolean matches(final String name) {
            return Resources.enumToKebabCase(this).equalsIgnoreCase(name);
        }

    }

    private static List<Integer> queryAndParseCertificatesIndices(final List<X509Certificate> chain) {
        final String input = CommandLineInterface.getConsoleOrFail()
                .readLine("Which certificates to extract from chain %s?%nInput zero or more indices from 0 to %d: ",
                        X509Utils.getCertificatesSimpleNames(chain),
                        Math.max(0, chain.size() - 1)
                );
        if (input.trim().isEmpty()) {
            return Collections.emptyList();
        }
        return Arrays.stream(input.split("\\s+"))
                .map(CertificateChainExtractor::parseCertificateIndex)
                .collect(Collectors.toList());
    }

    private static int parseCertificateIndex(final String input) {
        try {
            return Integer.parseInt(input);
        } catch (NumberFormatException e) {
            throw new TlsGeneratorParseException("Failed to parse certificate index: " + input, e);
        }
    }

}
