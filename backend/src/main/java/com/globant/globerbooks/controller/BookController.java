package com.globant.globerbooks.controller;

import com.globant.globerbooks.dto.BookListResponse;
import com.globant.globerbooks.dto.BookResponse;
import com.globant.globerbooks.dto.CreateBookRequest;
import com.globant.globerbooks.service.BookService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
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
     * Lists the books, newest first.
     *
     * @return {@code 200} with the books and their total
     */
    @GetMapping
    public ResponseEntity<BookListResponse> list() {
        return ResponseEntity.ok().contentType(JSON_UTF8).body(BookListResponse.of(service.list()));
    }
}
