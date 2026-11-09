package com.globant.globerbooks.support;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Helpers of the capacity tests (Task 41+42): a data location that passes the directory guard, a library
 * file of maximum-size books written through plain JDBC before the packaged jar starts, a list reader that only
 * counts bytes, and the peak heap read from the garbage-collector log of a child JVM.
 */
public final class CapacityFixtures {

    private static final DateTimeFormatter STAMP =
            DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'").withZone(ZoneOffset.UTC);
    private static final Pattern GC_LINE = Pattern.compile("(\\d+)M->(\\d+)M\\((\\d+)M\\)");
    private static final int WIDE_RANGE = 40;
    private static final String INSERT = "INSERT INTO books (id, title, author, status, rating, date_added, "
            + "date_completed) VALUES (?, ?, ?, 'unread', NULL, ?, NULL)";

    private CapacityFixtures() {
    }

    /**
     * Returns a title of exactly 500 code points, all but the short prefix outside the basic plane (four
     * bytes each in UTF-8): the largest possible body for the size bound of C-05.
     *
     * @param index the zero-based index (makes the title distinct)
     * @return the title
     */
    public static String wideTitle(int index) {
        return wide("T" + index + " ", LibrarySeeder.MAX_TITLE);
    }

    /**
     * Returns an author of exactly 200 code points, all but the short prefix outside the basic plane.
     *
     * @param index the zero-based index (makes the author distinct)
     * @return the author
     */
    public static String wideAuthor(int index) {
        return wide("A" + index + " ", LibrarySeeder.MAX_AUTHOR);
    }

    private static String wide(String prefix, int codePoints) {
        StringBuilder text = new StringBuilder(prefix);
        for (int length = prefix.length(); length < codePoints; length++) {
            text.appendCodePoint(0x1F4D8 + (length % WIDE_RANGE));
        }
        return text.toString();
    }

    /**
     * Creates the data location the guard accepts: a directory with mode 0700 holding {@code data} (0700)
     * and {@code data/tmp} (0700), below the given parent.
     *
     * @param parent an existing directory the test owns
     * @return the {@code data} directory
     * @throws IOException when a directory cannot be created
     */
    public static Path guardedData(Path parent) throws IOException {
        Path root = Files.createDirectory(parent.resolve("loc"), FileFacts.dirAttr("rwx------"));
        Path data = Files.createDirectory(root.resolve("data"), FileFacts.dirAttr("rwx------"));
        Files.createDirectory(data.resolve("tmp"), FileFacts.dirAttr("rwx------"));
        return data;
    }

    /**
     * Writes a valid library of maximum-size books (500-code-point titles, 200-code-point authors, all
     * unread) as the application's own schema would hold it.
     *
     * @param file the database file to create (mode 0600)
     * @param books how many books
     * @param sharedAuthor whether every book has the author of index 0 (the worst case of the duplicate scan)
     * @return the file
     * @throws Exception when the database cannot be written
     */
    public static Path maxSizeLibrary(Path file, int books, boolean sharedAuthor) throws Exception {
        StoredLibraryFixtures.emptyFile(file);
        StoredLibraryFixtures.run(file, false, StoredLibraryFixtures.schema());
        try (Connection connection = DriverManager.getConnection("jdbc:sqlite:" + file);
                PreparedStatement insert = connection.prepareStatement(INSERT)) {
            connection.setAutoCommit(false);
            for (int index = 0; index < books; index++) {
                insert.setString(1, LibrarySeeder.idOf(index));
                insert.setString(2, wideTitle(index));
                insert.setString(3, wideAuthor(sharedAuthor ? 0 : index));
                insert.setString(4, STAMP.format(LibrarySeeder.addedAt(index)));
                insert.executeUpdate();
            }
            connection.commit();
        } catch (SQLException failure) {
            throw new IllegalStateException("The library file could not be written", failure);
        }
        return file;
    }

    /**
     * Fetches the book list and reads the body without keeping it, so many readers need no memory.
     *
     * @param client the HTTP client
     * @param port the port of the application
     * @return the status and the number of body bytes
     * @throws Exception when the transport fails
     */
    public static ListSize fetchList(HttpClient client, int port) throws Exception {
        HttpRequest request = HttpRequest.newBuilder(URI.create("http://127.0.0.1:" + port + "/api/v1/books"))
                .GET().build();
        HttpResponse<InputStream> response = client.send(request,
                HttpResponse.BodyHandlers.ofInputStream());
        long bytes = 0;
        try (InputStream body = response.body()) {
            byte[] buffer = new byte[65_536];
            for (int read = body.read(buffer); read >= 0; read = body.read(buffer)) {
                bytes += read;
            }
        }
        return new ListSize(response.statusCode(), bytes);
    }

    /** Status and body size of one list answer. */
    public record ListSize(int status, long bytes) {
    }

    /**
     * Returns the largest heap occupancy before a collection, in MB, found in a unified GC log.
     *
     * @param gcLog the log file written by the child JVM
     * @return the peak in MB and the committed maximum seen on that line, as {@code peak/max}
     * @throws IOException when the log cannot be read
     */
    public static String peakHeap(Path gcLog) throws IOException {
        long peak = 0;
        long max = 0;
        for (String line : Files.readAllLines(gcLog)) {
            Matcher matcher = GC_LINE.matcher(line);
            if (matcher.find()) {
                peak = Math.max(peak, Long.parseLong(matcher.group(1)));
                max = Math.max(max, Long.parseLong(matcher.group(3)));
            }
        }
        return peak + "M of " + max + "M";
    }

    /**
     * Returns the peak resident set of a process in kB (the {@code VmHWM} line of its status file).
     *
     * @param pid the process id
     * @return the text of the line, or "n/a" when the status file cannot be read
     */
    public static String peakResidentSet(long pid) {
        try {
            return Files.readAllLines(Path.of("/proc", Long.toString(pid), "status")).stream()
                    .filter(line -> line.startsWith("VmHWM")).findFirst().orElse("n/a").replaceAll("\\s+", " ");
        } catch (IOException unreadable) {
            return "n/a";
        }
    }
}
