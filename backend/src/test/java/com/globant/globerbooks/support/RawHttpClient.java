package com.globant.globerbooks.support;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/** Raw-socket HTTP/1.1 client for requests a normal client cannot produce; loopback only. */
public final class RawHttpClient {

    private final int port;

    /**
     * Creates a client for the loopback address.
     *
     * @param port the server port
     */
    public RawHttpClient(int port) {
        this.port = port;
    }

    /** A parsed raw answer: status, header lines, body and the whole text. */
    public record Answer(int status, List<String> headers, String body, String raw) {

        /**
         * Returns the first value of a header, case-insensitively.
         *
         * @param name the header name
         * @return the value, or {@code null} when absent
         */
        public String header(String name) {
            String prefix = name.toLowerCase(Locale.ROOT) + ":";
            return headers.stream().filter(line -> line.toLowerCase(Locale.ROOT).startsWith(prefix))
                    .map(line -> line.substring(prefix.length()).trim()).findFirst().orElse(null);
        }
    }

    /**
     * Sends bytes as they are and reads until the server closes the connection.
     *
     * @param head the request head, ending with the blank line
     * @param body the bytes sent after the head, or {@code null}
     * @return the parsed answer
     * @throws IOException on a socket failure before the connection was open
     */
    public Answer send(String head, byte[] body) throws IOException {
        try (Socket socket = new Socket(InetAddress.getLoopbackAddress(), port)) {
            socket.setSoTimeout(5000);
            OutputStream out = socket.getOutputStream();
            out.write(head.getBytes(StandardCharsets.ISO_8859_1));
            writeQuietly(out, body);
            ByteArrayOutputStream buffer = new ByteArrayOutputStream();
            try {
                socket.getInputStream().transferTo(buffer);
            } catch (IOException reset) {
                // the connection may be reset after the answer; the bytes read so far count
            }
            return parse(buffer.toString(StandardCharsets.ISO_8859_1));
        }
    }

    /**
     * Sends one request with {@code Connection: close}; adds the allowed Host unless one is given.
     *
     * @param method the method
     * @param path the request target
     * @param body the body bytes or {@code null}
     * @param headers header lines without CRLF
     * @return the parsed answer
     * @throws IOException on a socket failure
     */
    public Answer request(String method, String path, byte[] body, String... headers) throws IOException {
        StringBuilder text = new StringBuilder(method + " " + path + " HTTP/1.1\r\n");
        boolean hasHost = false;
        for (String header : headers) {
            hasHost |= header.toLowerCase(Locale.ROOT).startsWith("host:");
            text.append(header).append("\r\n");
        }
        if (!hasHost) {
            text.append("Host: localhost:").append(port).append("\r\n");
        }
        return send(text.append("Connection: close\r\n\r\n").toString(), body);
    }

    /**
     * Builds a chunked body of spaces.
     *
     * @param total the number of payload bytes
     * @return the chunk-encoded bytes
     */
    public static byte[] chunkedSpaces(int total) {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        int left = total;
        while (left > 0) {
            int size = Math.min(left, 4096);
            out.writeBytes((Integer.toHexString(size) + "\r\n").getBytes(StandardCharsets.ISO_8859_1));
            out.writeBytes(" ".repeat(size).getBytes(StandardCharsets.ISO_8859_1));
            out.writeBytes("\r\n".getBytes(StandardCharsets.ISO_8859_1));
            left -= size;
        }
        out.writeBytes("0\r\n\r\n".getBytes(StandardCharsets.ISO_8859_1));
        return out.toByteArray();
    }

    private static void writeQuietly(OutputStream out, byte[] body) {
        try {
            if (body != null) {
                out.write(body);
            }
            out.flush();
        } catch (IOException closed) {
            // the server may close after an early rejection
        }
    }

    private static Answer parse(String raw) {
        int split = raw.indexOf("\r\n\r\n");
        String head = split < 0 ? raw : raw.substring(0, split);
        String body = split < 0 ? "" : raw.substring(split + 4);
        List<String> lines = new ArrayList<>(List.of(head.split("\r\n")));
        int status = lines.isEmpty() || lines.get(0).length() < 12 ? -1
                : Integer.parseInt(lines.remove(0).substring(9, 12));
        return new Answer(status, lines, body, raw);
    }
}
