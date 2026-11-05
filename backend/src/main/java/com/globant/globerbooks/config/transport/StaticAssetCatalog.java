package com.globant.globerbooks.config.transport;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.URI;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.TreeMap;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.core.io.support.ResourcePatternResolver;

/**
 * The fixed set of static assets, read once at construction from a resource location. Only
 * {@code .html}, {@code .css} and {@code .js} files are accepted; any other file, any
 * {@code *.test.js}, {@code *.map} or dotfile stops the start with a fixed reason that names no
 * path. The location is a constructor argument: no property, profile or environment variable
 * reaches it (SEC-F-072).
 */
public final class StaticAssetCatalog {

    /** Fixed reason used when the location holds a file the catalog must never serve. */
    public static final String REJECTED_FILE = "static content: unsupported file in the static location";

    private static final Map<String, String> CONTENT_TYPES = Map.of(
            "html", "text/html;charset=UTF-8",
            "css", "text/css;charset=UTF-8",
            "js", "text/javascript;charset=UTF-8");

    private final Map<String, Asset> assets;

    /**
     * Reads the location and fixes the catalog.
     *
     * @param location the resource location, for example {@code classpath:/static/}
     */
    public StaticAssetCatalog(String location) {
        this.assets = load(location);
    }

    /** One served file: its content type and its bytes (copied in and out, so the catalog stays fixed). */
    public static final class Asset {

        private final String contentType;
        private final byte[] content;

        Asset(String contentType, byte[] content) {
            this.contentType = contentType;
            this.content = content.clone();
        }

        /**
         * Returns the content type header value.
         *
         * @return the content type
         */
        public String contentType() {
            return contentType;
        }

        /**
         * Returns a copy of the file bytes.
         *
         * @return the bytes
         */
        public byte[] content() {
            return content.clone();
        }
    }

    /**
     * Finds the asset of an exact request path; {@code /} is {@code /index.html}.
     *
     * @param path the raw request path
     * @return the asset, or empty when the path is not in the catalog
     */
    public Optional<Asset> find(String path) {
        String key = "/".equals(path) ? "/index.html" : path;
        return Optional.ofNullable(assets.get(key));
    }

    /**
     * Lists the served paths.
     *
     * @return the paths in sorted order (a copy)
     */
    public List<String> paths() {
        return List.copyOf(assets.keySet());
    }

    private static Map<String, Asset> load(String location) {
        ResourcePatternResolver resolver = new PathMatchingResourcePatternResolver();
        Map<String, Asset> found = new TreeMap<>();
        try {
            String root = rootUri(resolver.getResource(location));
            for (Resource resource : resolver.getResources(location + "**")) {
                String uri = resource.getURI().toString();
                if (uri.endsWith("/") || !uri.startsWith(root)) {
                    continue;
                }
                String relative = URLDecoder.decode(uri.substring(root.length()), StandardCharsets.UTF_8);
                String type = contentType(relative);
                found.put("/" + relative, new Asset(type, resource.getContentAsByteArray()));
            }
        } catch (IOException failure) {
            throw new UncheckedIOException("static content: location unreadable", failure);
        }
        return Map.copyOf(found);
    }

    private static String rootUri(Resource root) throws IOException {
        URI uri = root.getURI();
        String text = uri.toString();
        return text.endsWith("/") ? text : text + "/";
    }

    private static String contentType(String relative) {
        for (String segment : relative.split("/")) {
            if (segment.startsWith(".")) {
                throw new IllegalStateException(REJECTED_FILE);
            }
        }
        int dot = relative.lastIndexOf('.');
        String type = dot < 0 ? null : CONTENT_TYPES.get(relative.substring(dot + 1));
        if (type == null || relative.endsWith(".test.js")) {
            throw new IllegalStateException(REJECTED_FILE);
        }
        return type;
    }
}
