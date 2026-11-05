package com.globant.globerbooks.config.transport;

import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import org.springframework.stereotype.Component;

/**
 * The fixed table of routes and methods, decided from the raw request target (spec 5.0). It holds
 * {@code GET} and {@code POST} on {@code /api/v1/books} plus {@code GET} on {@code /} and on every
 * path of the static catalog. No property, profile or request can add a route; a new endpoint is
 * added here by hand, and {@code RouteTableTest} fails until it is. HEAD and OPTIONS are never
 * defined. This class is the one place that computes the {@code Allow} header.
 */
@Component
public final class RouteTable {

    /** Path of the book collection. */
    public static final String BOOKS = "/api/v1/books";

    private static final String ALLOW_SEPARATOR = ", ";

    private final Map<String, Set<String>> routes;

    /**
     * Builds the table from the fixed API routes and the catalog paths.
     *
     * @param catalog the static catalog, whose paths are served with {@code GET}
     */
    public RouteTable(StaticAssetCatalog catalog) {
        Map<String, Set<String>> table = new TreeMap<>();
        table.put(BOOKS, new TreeSet<>(Set.of("GET", "POST")));
        table.put("/", new TreeSet<>(Set.of("GET")));
        for (String path : catalog.paths()) {
            table.put(path, new TreeSet<>(Set.of("GET")));
        }
        this.routes = Map.copyOf(table);
    }

    /** Outcome of a route decision. */
    public enum Outcome {
        /** Path and method exist. */
        FOUND,
        /** The path is not routable or not defined. */
        NOT_FOUND,
        /** The path exists, the method does not. */
        METHOD_NOT_ALLOWED
    }

    /**
     * Result of a decision; {@code allow} is set only for {@code METHOD_NOT_ALLOWED}.
     *
     * @param outcome the outcome
     * @param allow the value of the {@code Allow} header, or null
     */
    public record Decision(Outcome outcome, String allow) {
    }

    /**
     * Decides route and method. The method is compared exactly as received (case-sensitive).
     *
     * @param target the raw request target
     * @param method the request method
     * @return the decision
     */
    public Decision decide(RequestTarget target, String method) {
        Optional<String> allow = allowFor(target);
        if (allow.isEmpty()) {
            return new Decision(Outcome.NOT_FOUND, null);
        }
        if (routes.get(target.path()).contains(method)) {
            return new Decision(Outcome.FOUND, null);
        }
        return new Decision(Outcome.METHOD_NOT_ALLOWED, allow.get());
    }

    /**
     * Computes the {@code Allow} value of a defined path: its methods, sorted, never HEAD or
     * OPTIONS.
     *
     * @param target the raw request target
     * @return the header value, or empty when the path is not routable or not defined
     */
    public Optional<String> allowFor(RequestTarget target) {
        if (!target.routable() || !routes.containsKey(target.path())) {
            return Optional.empty();
        }
        return Optional.of(String.join(ALLOW_SEPARATOR, routes.get(target.path())));
    }

    /**
     * Lists the table for tests and diagnostics.
     *
     * @return path to sorted methods (a copy)
     */
    public Map<String, Set<String>> routes() {
        Map<String, Set<String>> copy = new TreeMap<>();
        routes.forEach((path, methods) -> copy.put(path, new TreeSet<>(methods)));
        return copy;
    }
}
