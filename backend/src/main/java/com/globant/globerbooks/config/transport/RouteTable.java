package com.globant.globerbooks.config.transport;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import org.springframework.stereotype.Component;

/**
 * The fixed table of routes and methods, decided from the raw request target (spec 5.0). It holds
 * {@code GET} and {@code POST} on {@code /api/v1/books}, {@code GET} and {@code DELETE} on
 * {@code /api/v1/books/{id}}, {@code PUT} on {@code /api/v1/books/{id}/status}, plus {@code GET} on
 * {@code /} and on every path of the static catalog. Any non-empty single segment stands for the id:
 * the route is decided first and the shape of the id second (BR-012 steps 2 and 3).
 * No property, profile or request can add a route; a new endpoint is added here by hand, and
 * {@code RouteTableTest} fails until it is. HEAD and OPTIONS are never defined.
 * This class is the one place that computes the {@code Allow} header.
 */
@Component
public final class RouteTable {

    /** Path of the book collection. */
    public static final String BOOKS = "/api/v1/books";

    /** Template of the path of one book. */
    public static final String BOOK = BOOKS + "/{id}";

    /** Template of the path of the status of one book. */
    public static final String BOOK_STATUS = BOOK + "/status";

    private static final int BOOKS_SEGMENTS = 3;
    private static final int ID_SEGMENT_COUNT = 4;
    private static final int SUBRESOURCE_SEGMENT_COUNT = 5;

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
        table.put(BOOK, new TreeSet<>(Set.of("GET", "DELETE")));
        table.put(BOOK_STATUS, new TreeSet<>(Set.of("PUT")));
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
        Optional<String> key = keyFor(target);
        if (key.isEmpty()) {
            return new Decision(Outcome.NOT_FOUND, null);
        }
        if (routes.get(key.get()).contains(method)) {
            return new Decision(Outcome.FOUND, null);
        }
        return new Decision(Outcome.METHOD_NOT_ALLOWED, String.join(ALLOW_SEPARATOR, routes.get(key.get())));
    }

    /**
     * Computes the {@code Allow} value of a defined path: its methods, sorted, never HEAD or
     * OPTIONS.
     *
     * @param target the raw request target
     * @return the header value, or empty when the path is not routable or not defined
     */
    public Optional<String> allowFor(RequestTarget target) {
        return keyFor(target).map(key -> String.join(ALLOW_SEPARATOR, routes.get(key)));
    }

    /** Finds the table key of a routable path: the path itself or the template with one id segment. */
    private Optional<String> keyFor(RequestTarget target) {
        if (!target.routable()) {
            return Optional.empty();
        }
        if (routes.containsKey(target.path())) {
            return Optional.of(target.path());
        }
        List<String> segments = target.segments();
        boolean underBooks = segments.size() >= ID_SEGMENT_COUNT
                && segments.size() <= SUBRESOURCE_SEGMENT_COUNT
                && String.join("/", segments.subList(0, BOOKS_SEGMENTS)).equals(BOOKS.substring(1))
                && !segments.get(ID_SEGMENT_COUNT - 1).isEmpty();
        if (!underBooks) {
            return Optional.empty();
        }
        if (segments.size() == ID_SEGMENT_COUNT) {
            return Optional.of(BOOK);
        }
        return "status".equals(segments.get(ID_SEGMENT_COUNT)) ? Optional.of(BOOK_STATUS) : Optional.empty();
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
