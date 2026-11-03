package com.globant.globerbooks.config.transport;

import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * The header rules of the gate: the security headers of every response and the grammar of the
 * request headers {@code Host}, {@code Origin}, {@code Sec-Fetch-Site} and {@code Content-Type}
 * (BR-012 step 1, SC-03, SC-04, SC-05, SC-10, SC-28). Each check takes all the values of the
 * header so that an absent or repeated header can be judged.
 */
public final class HeaderPolicy {

    private static final String CSP = "default-src 'self'; script-src 'self'; style-src 'self'; "
            + "img-src 'self'; font-src 'self'; connect-src 'self'; object-src 'none'; frame-src 'none'; "
            + "worker-src 'none'; manifest-src 'self'; frame-ancestors 'none'; base-uri 'none'; "
            + "form-action 'self'; require-trusted-types-for 'script'";

    private static final String PERMISSIONS = "camera=(), microphone=(), geolocation=(), payment=(), "
            + "usb=(), serial=(), bluetooth=(), clipboard-read=(), display-capture=(), fullscreen=()";

    /** Headers set on every response. */
    public static final Map<String, String> SECURITY_HEADERS = Map.of(
            "Content-Security-Policy", CSP,
            "X-Content-Type-Options", "nosniff",
            "X-Frame-Options", "DENY",
            "Referrer-Policy", "no-referrer",
            "Cross-Origin-Opener-Policy", "same-origin",
            "Cross-Origin-Resource-Policy", "same-origin",
            "Permissions-Policy", PERMISSIONS,
            "Cache-Control", "no-store");

    private static final Pattern JSON_TYPE =
            Pattern.compile("application/json(?:[ \\t]*;[ \\t]*charset=utf-8)?[ \\t]*", Pattern.CASE_INSENSITIVE);

    private HeaderPolicy() {
    }

    /**
     * Judges the {@code Host} header: exactly one value whose lower-cased text is
     * {@code localhost:<port>} or {@code 127.0.0.1:<port>}.
     *
     * @param values all values of the header (empty when absent)
     * @param port the local port of the connection
     * @return true when the host is allowed
     */
    public static boolean hostAllowed(List<String> values, int port) {
        if (values.size() != 1) {
            return false;
        }
        String host = values.get(0).toLowerCase(Locale.ROOT);
        return host.equals("localhost:" + port) || host.equals("127.0.0.1:" + port);
    }

    /**
     * Judges the {@code Origin} header: absent, or exactly one value equal to
     * {@code http://localhost:<port>} or {@code http://127.0.0.1:<port>}.
     *
     * @param values all values of the header (empty when absent)
     * @param port the local port of the connection
     * @return true when the origin is allowed
     */
    public static boolean originAllowed(List<String> values, int port) {
        if (values.isEmpty()) {
            return true;
        }
        if (values.size() > 1) {
            return false;
        }
        String origin = values.get(0).toLowerCase(Locale.ROOT);
        return origin.equals("http://localhost:" + port) || origin.equals("http://127.0.0.1:" + port);
    }

    /**
     * Judges the {@code Sec-Fetch-Site} header: absent, or exactly one value that is
     * {@code same-origin} or {@code none}.
     *
     * @param values all values of the header (empty when absent)
     * @return true when the fetch site is allowed
     */
    public static boolean fetchSiteAllowed(List<String> values) {
        if (values.isEmpty()) {
            return true;
        }
        return values.size() == 1 && (values.get(0).equals("same-origin") || values.get(0).equals("none"));
    }

    /**
     * Judges the {@code Content-Type} header against the grammar of spec 5.0: one header, media type
     * {@code application/json}, at most the parameter {@code charset=utf-8} unquoted.
     *
     * @param values all values of the header (empty when absent)
     * @return true when the content type is allowed
     */
    public static boolean contentTypeAllowed(List<String> values) {
        return values.size() == 1 && JSON_TYPE.matcher(values.get(0)).matches();
    }

    /**
     * Tells whether the method needs a content type: every method except GET and HEAD.
     *
     * @param method the HTTP method
     * @return true when the content type is required
     */
    public static boolean requiresContentType(String method) {
        return !"GET".equals(method) && !"HEAD".equals(method);
    }
}
