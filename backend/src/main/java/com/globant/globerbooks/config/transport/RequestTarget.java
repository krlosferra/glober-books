package com.globant.globerbooks.config.transport;

import java.util.List;
import java.util.Locale;

/**
 * The path of a request exactly as received (spec 5.0 routing). Nothing is normalised: a double
 * slash, a {@code ;}, an encoded dot, a trailing slash and letter case all stay as sent. The class
 * only answers whether the path can be routed at all and exposes its raw segments.
 */
public final class RequestTarget {

    private final String path;

    private RequestTarget(String path) {
        this.path = path;
    }

    /**
     * Wraps a raw request path (the request URI without the query).
     *
     * @param rawPath the path as received; a null value is treated as empty
     * @return the target
     */
    public static RequestTarget of(String rawPath) {
        return new RequestTarget(rawPath == null ? "" : rawPath);
    }

    /**
     * Returns the path as received.
     *
     * @return the raw path
     */
    public String path() {
        return path;
    }

    /**
     * Returns the raw segments after the leading slash, empty segments included.
     *
     * @return the segments, or an empty list when the path does not start with a slash
     */
    public List<String> segments() {
        if (!path.startsWith("/")) {
            return List.of();
        }
        return List.of(path.substring(1).split("/", -1));
    }

    /**
     * Tells whether the path may be routed: it starts with a slash and holds no {@code ;}, no
     * literal {@code \}, no encoded {@code /}, {@code \} or {@code ;}, and no dot segment, literal
     * or encoded (a segment equal to {@code .} or {@code ..} once every {@code %2e} is decoded).
     *
     * @return true when no rule of spec 5.0 makes the path not routable
     */
    public boolean routable() {
        if (!path.startsWith("/") || path.indexOf(';') >= 0 || path.indexOf('\\') >= 0) {
            return false;
        }
        String lower = path.toLowerCase(Locale.ROOT);
        if (lower.contains("%2f") || lower.contains("%5c") || lower.contains("%3b")) {
            return false;
        }
        for (String segment : segments()) {
            String decoded = segment.toLowerCase(Locale.ROOT).replace("%2e", ".");
            if (".".equals(decoded) || "..".equals(decoded)) {
                return false;
            }
        }
        return true;
    }
}
