# TLS Truststore Generator

TLS Truststore Generator is a command line utility for generating TLS truststores for specific sets of HTTPS URLs.

## How to Build

TLS Truststore Generator requires at least JDK 1.8 and Maven to build:

1. Fetch it from Github:<br>
   `git clone https://github.com/open-eid/tls-truststore-generator.git`
2. Navigate into the project's root directory:<br>
   `cd tls-truststore-generator`
3. Compile and package:<br>
   `mvn clean package`

The built and packaged application will be located in `tls-truststore-generator/target` as an executable JAR file `tls-truststore-generator-1.0.0.jar`.

## How to Use

TLS Truststore Generator is run using the following command:
```
java -jar tls-truststore-generator-1.0.0.jar ARGUMENTS...
```
where `ARGUMENTS...` is any valid combination of supported command line arguments and their parameters.

### Truststore Output

The generated truststore destination path can be specified using the `--out` argument or its short alias `-o` followed by the desired path:
```
java -jar tls-truststore-generator-1.0.0.jar --out /path/to/truststore.extension
```
```
java -jar tls-truststore-generator-1.0.0.jar -o /path/to/truststore.extension
```

**--out, -o** is a mandatory argument, requiring exactly one parameter: truststore destination path.

#### Truststore Password

The password of the generated truststore can be specified using the `--password` argument or its short alias `-p` followed by the desired password:
```
java -jar tls-truststore-generator-1.0.0.jar --password changeit
```
```
java -jar tls-truststore-generator-1.0.0.jar -p changeit
```

**--password, -p** is an optional argument, requiring exactly one parameter: truststore password.
If not present, queries the password interactively in a secure manner (requires interactive console support on the system the utility is run).

#### Truststore Type

The type of the generated truststore can be specified using the `--type` argument or its short alias `-t` followed by the desired type:
```
java -jar tls-truststore-generator-1.0.0.jar --type PKCS12
```
```
java -jar tls-truststore-generator-1.0.0.jar -t PKCS12
```

**--type, -t** is an optional argument, requiring exactly one parameter: truststore type.
If not present, defaults to **PKCS12**.

### URL Input

TLS Truststore Generator is currently able to process any generic URLs, as well as LOTL (List Of Trusted Lists) URLs.
In case of the latter, the LOTL is fetched, parsed and all the URLs of individual trusted lists from this LOTL are sent for further processing.

**NB:** at least one of URL input arguments must be present!

#### Generic URLs

Generic input URLs can be specified using the `--url` argument followed by a space-separated list of URL strings:
```
java -jar tls-truststore-generator-1.0.0.jar --url https://host[:port][/path]
```

**--url** is a mandatory (if no other input arguments are present) argument, requiring one or more parameters: input URLs.

#### LOTL URLs

Currently, only **EU Trusted List of Trust Service Providers** version **5** is guaranteed to be compatible with TLS Truststore Generator.<br>
LOTL input URLs can be specified using the `--lotl-url` argument or its shorter alias `--lotl` followed by a space-separated list of URL strings:
```
java -jar tls-truststore-generator-1.0.0.jar --lotl-url https://host[:port]/path/to/lotl
```
```
java -jar tls-truststore-generator-1.0.0.jar --lotl https://host[:port]/path/to/lotl
```

**--lotl-url, --lotl** is a mandatory (if no other input arguments are present) argument, requiring one or more parameters: LOTL URLs.

### Extraction from Certificate Chains

For all the **HTTPS** URLs originating from any input sources, TLS Truststore Generator fetches the certificate chains of their server certificates.
Specific certificates to extract from the chains can be specified using the `--extract-from-chain` argument followed by one of the following options:

| Option        | Description                                                                                                  |
| ------------- | ------------------------------------------------------------------------------------------------------------ |
| `all`         | Extracts all certificates from the certificate chain                                                         |
| `first`       | Extracts the first certificate from the certificate chain (if chain contains at least one certificate)       |
| `last`        | Extracts the last certificate from the certificate chain (if chain contains at least one certificate)        |
| `ca-or-cert`  | Extract the CA (second certificate), the certificate itself or nothing depending on the length of the chain  |
| <index...>    | Extracts certificates at the specified indices from the certificate chain                                    |
| `interactive` | Interactive mode* - interactively queries which certificate(s) to extract from each certificate chain        |

\* interactive mode requires interactive console support on the system the utility is run

Example parameter usage:
```
java -jar tls-truststore-generator-1.0.0.jar --extract-from-chain ca-or-cert
```

Example indices usage:
```
java -jar tls-truststore-generator-1.0.0.jar --extract-from-chain 0 1 2
```

**--extract-from-chain** is an optional argument, requiring one or more parameters.
If not present, acts as if `--extract-from-chain all` was specified.

### External Communication

#### HTTP Redirection

Following HTTP 3XX redirects can be enabled using the `--follow-redirects` argument:
```
java -jar tls-truststore-generator-1.0.0.jar --follow-redirects
```

**--follow-redirects** is an optional argument, requiring no parameters.
If not specified, will not follow any redirects.

#### TLS Protocol

Specific TLS protocol for secure connections can be specified using the `--tls-protocol` argument followed by the TLS protocol identifier:
```
java -jar tls-truststore-generator-1.0.0.jar --tls-protocol TLSv1.3
```

**--tls-protocol** is an optional argument, requiring exactly one parameter: [TLS protocol](https://docs.oracle.com/javase/8/docs/technotes/guides/security/StandardNames.html#SSLContext).
If not present, defaults to **TLSv1.3**. Currently accepts any of `TLS`, `TLSv1`, `TLSv1.1`, `TLSv1.2` and `TLSv1.3`.

### Misc

**--help, -h** is an optional argument, requiring no parameters.
If present and is the only provided argument, prints the help of TLS Truststore Generator and exits.
