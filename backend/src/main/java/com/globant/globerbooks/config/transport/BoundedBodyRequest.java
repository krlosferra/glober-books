package com.globant.globerbooks.config.transport;

import jakarta.servlet.ReadListener;
import jakarta.servlet.ServletInputStream;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletRequestWrapper;
import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;

/**
 * A request whose body has been read into a buffer of at most 16,384 bytes (SC-06). The read
 * stops at byte 16,385 and fails, so a chunked or unread body never grows past the cap.
 */
public final class BoundedBodyRequest extends HttpServletRequestWrapper {

    /** The body cap in bytes. */
    public static final int LIMIT = 16_384;

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
     * @throws IOException when the body cannot be read
     */
    public static BoundedBodyRequest read(HttpServletRequest request) throws IOException {
        byte[] bytes = request.getInputStream().readNBytes(LIMIT + 1);
        if (bytes.length > LIMIT) {
            throw new TooLargeException();
        }
        return new BoundedBodyRequest(request, bytes);
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
