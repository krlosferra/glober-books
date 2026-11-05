package com.globant.globerbooks.config.transport;

import jakarta.servlet.ReadListener;
import jakarta.servlet.ServletInputStream;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletRequestWrapper;
import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.time.Duration;

/**
 * A request whose body has been read into a buffer of at most 16,384 bytes (SC-06). The read
 * stops at byte 16,385 and fails, so a chunked or unread body never grows past the cap. The whole
 * read has a total deadline, because the connector's 5 s timeout restarts on every byte a slow
 * client sends (SEC-F-073).
 */
public final class BoundedBodyRequest extends HttpServletRequestWrapper {

    /** The body cap in bytes. */
    public static final int LIMIT = 16_384;

    /** Total time allowed to read a body; an assumption of Task 16, not a spec value. */
    public static final Duration DEADLINE = Duration.ofSeconds(10);

    private static final int CHUNK = 1_024;

    private final byte[] body;

    private BoundedBodyRequest(HttpServletRequest request, byte[] body) {
        super(request);
        this.body = body;
    }

    /**
     * Reads the body of the request into a buffer, stopping at byte 16,385.
     *
     * @param request the request
     * @return the request that serves the buffered body
     * @throws TooLargeException when the body is longer than 16,384 bytes
     * @throws TooSlowException when the body is not complete within the total deadline
     * @throws IOException when the body cannot be read
     */
    public static BoundedBodyRequest read(HttpServletRequest request) throws IOException {
        return read(request, DEADLINE);
    }

    /**
     * Reads the body with a given total deadline; the deadline is checked after every read.
     *
     * @param request the request
     * @param deadline the total time allowed
     * @return the request that serves the buffered body
     * @throws TooLargeException when the body is longer than 16,384 bytes
     * @throws TooSlowException when the body is not complete within the deadline
     * @throws IOException when the body cannot be read
     */
    public static BoundedBodyRequest read(HttpServletRequest request, Duration deadline) throws IOException {
        long expiry = System.nanoTime() + deadline.toNanos();
        ByteArrayOutputStream collected = new ByteArrayOutputStream();
        byte[] chunk = new byte[CHUNK];
        InputStream input = request.getInputStream();
        int count = input.read(chunk, 0, Math.min(CHUNK, LIMIT + 1));
        while (count >= 0) {
            collected.write(chunk, 0, count);
            if (collected.size() > LIMIT) {
                throw new TooLargeException();
            }
            if (System.nanoTime() - expiry > 0) {
                throw new TooSlowException();
            }
            count = input.read(chunk, 0, Math.min(CHUNK, LIMIT + 1 - collected.size()));
        }
        return new BoundedBodyRequest(request, collected.toByteArray());
    }

    @Override
    public ServletInputStream getInputStream() {
        return new BufferedServletInputStream(body);
    }

    @Override
    public BufferedReader getReader() {
        String name = getCharacterEncoding();
        Charset charset = name == null ? StandardCharsets.UTF_8 : Charset.forName(name);
        return new BufferedReader(new InputStreamReader(new ByteArrayInputStream(body), charset));
    }

    @Override
    public int getContentLength() {
        return body.length;
    }

    @Override
    public long getContentLengthLong() {
        return body.length;
    }

    /** Signals that the body is over the cap. */
    public static final class TooLargeException extends IOException {

        private static final long serialVersionUID = 1L;

        private TooLargeException() {
            super("body over the cap");
        }
    }

    /** Signals that the body took longer than the total deadline. */
    public static final class TooSlowException extends IOException {

        private static final long serialVersionUID = 1L;

        private TooSlowException() {
            super("body too slow");
        }
    }

    /** Servlet stream over a byte array. */
    private static final class BufferedServletInputStream extends ServletInputStream {

        private final ByteArrayInputStream source;

        BufferedServletInputStream(byte[] bytes) {
            this.source = new ByteArrayInputStream(bytes);
        }

        @Override
        public int read() {
            return source.read();
        }

        @Override
        public int read(byte[] target, int offset, int length) {
            return source.read(target, offset, length);
        }

        @Override
        public boolean isFinished() {
            return source.available() == 0;
        }

        @Override
        public boolean isReady() {
            return true;
        }

        @Override
        public void setReadListener(ReadListener listener) {
            throw new IllegalStateException("Asynchronous reads are not supported.");
        }
    }
}
