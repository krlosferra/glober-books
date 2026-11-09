package com.globant.globerbooks.support;

import java.io.IOException;
import java.io.InputStream;
import java.net.InetAddress;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

/**
 * A client that asks for the book list and then reads the answer slowly: one small chunk, then a pause, until
 * the answer ends, the server closes the connection or the deadline passes (Task 41+42, criterion 4). The pause
 * is a throttle on a latch, never a synchronisation sleep. Loopback only.
 */
public final class SlowReader {

    private static final int CHUNK = 16_384;

    private SlowReader() {
    }

    /** How one slow read ended: the status line, the bytes read and why it stopped. */
    public record Outcome(String statusLine, long bytes, String ended) {
    }

    /**
     * Reads the list slowly.
     *
     * @param port the port of the application
     * @param pauseMillis the pause after every chunk
     * @param deadlineMillis the longest time to keep reading
     * @param stop released to end the read early
     * @return the outcome
     * @throws IOException when the connection cannot be opened or the request cannot be sent
     * @throws InterruptedException when the pause is interrupted
     */
    public static Outcome readList(int port, long pauseMillis, long deadlineMillis, CountDownLatch stop)
            throws IOException, InterruptedException {
        long deadline = System.nanoTime() + TimeUnit.MILLISECONDS.toNanos(deadlineMillis);
        try (Socket socket = new Socket(InetAddress.getLoopbackAddress(), port)) {
            socket.setSoTimeout((int) deadlineMillis);
            socket.setReceiveBufferSize(4_096);
            socket.getOutputStream().write(("GET /api/v1/books HTTP/1.1\r\nHost: localhost:" + port
                    + "\r\nConnection: close\r\n\r\n").getBytes(StandardCharsets.US_ASCII));
            InputStream stream = socket.getInputStream();
            byte[] buffer = new byte[CHUNK];
            long total = 0;
            String statusLine = "";
            String ended = "deadline";
            try {
                while (System.nanoTime() < deadline && !stop.await(pauseMillis, TimeUnit.MILLISECONDS)) {
                    int read = stream.read(buffer);
                    if (read < 0) {
                        ended = "complete";
                        break;
                    }
                    if (total == 0) {
                        statusLine = new String(buffer, 0, Math.min(read, 15), StandardCharsets.US_ASCII);
                    }
                    total += read;
                }
                if (stop.getCount() == 0) {
                    ended = "released";
                }
            } catch (IOException reset) {
                ended = "closed by the server (" + reset.getClass().getSimpleName() + ")";
            }
            return new Outcome(statusLine, total, ended);
        }
    }
}
