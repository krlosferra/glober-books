CREATE TABLE IF NOT EXISTS books (
    id             TEXT    NOT NULL PRIMARY KEY,
    title          TEXT    NOT NULL,
    author         TEXT    NOT NULL,
    status         TEXT    NOT NULL,
    rating         INTEGER,
    date_added     TEXT    NOT NULL,
    date_completed TEXT,
    CONSTRAINT ck_books_id CHECK (
        length(id) = 36 AND length(replace(id, '-', '')) = 32
        AND substr(id, 9, 1) = '-' AND substr(id, 14, 1) = '-'
        AND substr(id, 19, 1) = '-' AND substr(id, 24, 1) = '-'
        AND id NOT GLOB '*[^0-9a-f-]*'),
    CONSTRAINT ck_books_title CHECK (length(title) BETWEEN 1 AND 500),
    CONSTRAINT ck_books_author CHECK (length(author) BETWEEN 1 AND 200),
    CONSTRAINT ck_books_status CHECK (status IN ('unread', 'reading', 'completed')),
    CONSTRAINT ck_books_rating CHECK (rating IS NULL OR (rating BETWEEN 1 AND 5 AND status = 'completed')),
    CONSTRAINT ck_books_completed CHECK ((status = 'completed') = (date_completed IS NOT NULL)),
    CONSTRAINT ck_books_added CHECK (length(date_added) = 24 AND date_added GLOB
        '[0-9][0-9][0-9][0-9]-[0-9][0-9]-[0-9][0-9]T[0-9][0-9]:[0-9][0-9]:[0-9][0-9].[0-9][0-9][0-9]Z'),
    CONSTRAINT ck_books_done CHECK (date_completed IS NULL OR (length(date_completed) = 24 AND date_completed GLOB
        '[0-9][0-9][0-9][0-9]-[0-9][0-9]-[0-9][0-9]T[0-9][0-9]:[0-9][0-9]:[0-9][0-9].[0-9][0-9][0-9]Z'))
) STRICT;
