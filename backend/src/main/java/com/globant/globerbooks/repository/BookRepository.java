package com.globant.globerbooks.repository;

import com.globant.globerbooks.domain.Book;
import com.globant.globerbooks.domain.BookStatus;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

/** Persistence of books; derived queries only (no native SQL). */
public interface BookRepository extends JpaRepository<Book, String> {

    /**
     * Lists all books, newest first, ties by id ascending (BR-010).
     *
     * @return the ordered books
     */
    List<Book> findAllByOrderByDateAddedDescIdAsc();

    /**
     * Lists the books with one status in the same order (BR-010).
     *
     * @param status the exact status
     * @return the ordered books
     */
    List<Book> findByStatusOrderByDateAddedDescIdAsc(BookStatus status);
}
