package com.globant.globerbooks.controller;

import com.globant.globerbooks.config.transport.QueryPolicy;
import com.globant.globerbooks.dto.BookListResponse;
import com.globant.globerbooks.dto.BookResponse;
import com.globant.globerbooks.dto.CreateBookRequest;
import com.globant.globerbooks.dto.StatusRequest;
import com.globant.globerbooks.service.BookService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * HTTP binding of the book endpoints. It holds no rule, builds no error body and never logs
 * request data. Bodies are DTOs handed to the message converter, which writes them straight to the
 * response stream (no intermediate byte array or string).
 */
@RestController
@RequestMapping("/api/v1/books")
public class BookController {

    private static final MediaType JSON_UTF8 = MediaType.parseMediaType("application/json; charset=UTF-8");

    private final BookService service;

    /**
     * Creates the controller.
     *
     * @param service the book use cases
     */
    public BookController(BookService service) {
        this.service = service;
    }

    /**
     * Adds a book.
     *
     * @param request the new book
     * @return {@code 201} with the stored book
     */
    @PostMapping
    public ResponseEntity<BookResponse> add(@Valid @RequestBody CreateBookRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).contentType(JSON_UTF8).body(service.create(request));
    }

    /**
     * Lists the books, newest first. The optional {@code status} filter is read from the request
     * attribute set by the gate, which has already decoded and checked it (never from the servlet
     * parameter API).
     *
     * @param status the checked filter value, or null for every book
     * @return {@code 200} with the books and their total
     */
    @GetMapping
    public ResponseEntity<BookListResponse> list(
            @RequestAttribute(name = QueryPolicy.STATUS_ATTRIBUTE, required = false) String status) {
        return ResponseEntity.ok().contentType(JSON_UTF8).body(BookListResponse.of(service.list(status)));
    }

    /**
     * Reads one book. The gate has checked the id shape, the query and the empty body.
     *
     * @param id the id text
     * @return {@code 200} with the book
     */
    @GetMapping("/{id}")
    public ResponseEntity<BookResponse> get(@PathVariable("id") String id) {
        return ResponseEntity.ok().contentType(JSON_UTF8).body(service.get(id));
    }

    /**
     * Changes the status of a book. The body is read and validated before the service looks for the book.
     *
     * @param id the id text
     * @param request the new status
     * @return {@code 200} with the book after the change
     */
    @PutMapping("/{id}/status")
    public ResponseEntity<BookResponse> changeStatus(@PathVariable("id") String id,
            @Valid @RequestBody StatusRequest request) {
        return ResponseEntity.ok().contentType(JSON_UTF8).body(service.changeStatus(id, request.status()));
    }

    /**
     * Deletes a book.
     *
     * @param id the id text
     * @return {@code 200} with the removed book
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<BookResponse> delete(@PathVariable("id") String id) {
        return ResponseEntity.ok().contentType(JSON_UTF8).body(service.delete(id));
    }
}
