package com.globant.globerbooks.controller;

import com.globant.globerbooks.config.transport.StaticAssetCatalog;
import com.globant.globerbooks.config.transport.StaticAssetCatalog.Asset;
import com.globant.globerbooks.error.MalformedRequestException;
import jakarta.servlet.http.HttpServletRequest;
import java.io.IOException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.resource.NoResourceFoundException;

/**
 * Serves the entries of the static catalog by exact raw path. Anything else is {@code NOT_FOUND}.
 * Conditional and range headers are not read, so the answer is always the full body.
 */
@RestController
public class StaticContentController {

    private final StaticAssetCatalog catalog;

    /**
     * Creates the controller.
     *
     * @param catalog the fixed catalog
     */
    public StaticContentController(StaticAssetCatalog catalog) {
        this.catalog = catalog;
    }

    /**
     * Serves a static asset.
     *
     * @param request the request
     * @return {@code 200} with the asset bytes
     * @throws IOException when the request body cannot be read
     * @throws NoResourceFoundException when the path is not in the catalog
     */
    @GetMapping("/**")
    public ResponseEntity<byte[]> serve(HttpServletRequest request) throws IOException, NoResourceFoundException {
        String path = request.getRequestURI();
        Asset asset = catalog.find(path)
                .orElseThrow(() -> new NoResourceFoundException(HttpMethod.GET, "static", "not in catalog"));
        String query = request.getQueryString();
        if (query != null && !query.isEmpty() || request.getInputStream().read() != -1) {
            throw new MalformedRequestException();
        }
        return ResponseEntity.ok().header(HttpHeaders.CONTENT_TYPE, asset.contentType()).body(asset.content());
    }
}
