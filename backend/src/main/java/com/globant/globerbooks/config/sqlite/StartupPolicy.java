package com.globant.globerbooks.config.sqlite;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Locale;
import java.util.regex.Pattern;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

/**
 * Fail-closed start policy (SC-01, SC-39). The application starts only when the bound address is a
 * loopback literal and the datasource URL has the fixed form {@code jdbc:sqlite:<file>} with no
 * parameters. Every refusal carries a fixed reason: no URL, path, address text or book data.
 */
@Component
public final class StartupPolicy {

    /** Reason used when the bound address is missing, not a literal, or not loopback. */
    public static final String REASON_ADDRESS = "Start refused: the bind address must be a loopback address.";

    /** Reason used when the datasource URL is not of the form {@code jdbc:sqlite:<file>}. */
    public static final String REASON_URL = "Start refused: the datasource URL must be jdbc:sqlite:<file>.";

    /** Reason used when the datasource URL carries parameters or a fragment. */
    public static final String REASON_PARAMETERS =
            "Start refused: the datasource URL must not carry parameters or a fragment.";

    private static final String URL_PREFIX = "jdbc:sqlite:";
    private static final Pattern IPV4_LITERAL = Pattern.compile("\\d{1,3}(\\.\\d{1,3}){3}");
    private static final Pattern IPV6_LITERAL = Pattern.compile("[0-9a-fA-F:.]*:[0-9a-fA-F:.]*");

    /**
     * Checks the effective configuration at context creation, before the web server starts.
     *
     * @param environment the application environment (properties, environment variables, arguments)
     */
    public StartupPolicy(Environment environment) {
        validate(environment.getProperty("server.address"), environment.getProperty("spring.datasource.url"));
    }

    /**
     * Checks an address and a URL.
     *
     * @param serverAddress the value of {@code server.address}; may be null
     * @param datasourceUrl the value of {@code spring.datasource.url}; may be null
     * @throws IllegalStateException with a fixed reason when either value is refused
     */
    public static void validate(String serverAddress, String datasourceUrl) {
        requireLoopback(serverAddress);
        requireFileUrl(datasourceUrl);
    }

    private static void requireLoopback(String address) {
        if (address == null || !isLiteral(address) || !isLoopback(address)) {
            throw new IllegalStateException(REASON_ADDRESS);
        }
    }

    private static boolean isLiteral(String address) {
        return IPV4_LITERAL.matcher(address).matches() || IPV6_LITERAL.matcher(address).matches();
    }

    private static boolean isLoopback(String literal) {
        try {
            return InetAddress.getByName(literal).isLoopbackAddress();
        } catch (UnknownHostException failure) {
            return false;
        }
    }

    private static void requireFileUrl(String url) {
        if (url == null || !url.startsWith(URL_PREFIX)) {
            throw new IllegalStateException(REASON_URL);
        }
        if (url.indexOf('?') >= 0 || url.indexOf('#') >= 0) {
            throw new IllegalStateException(REASON_PARAMETERS);
        }
        String file = url.substring(URL_PREFIX.length());
        String lower = file.toLowerCase(Locale.ROOT);
        if (file.isBlank() || file.startsWith(":") || lower.startsWith("file:") || lower.startsWith("resource:")
                || file.chars().anyMatch(code -> code < ' ' || code == 0x7f)) {
            throw new IllegalStateException(REASON_URL);
        }
    }
}
