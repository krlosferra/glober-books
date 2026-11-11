package com.globant.globerbooks.support;

import java.io.IOException;
import java.io.InputStream;
import java.net.InetAddress;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

/**
 * A client that sends a request one byte every two seconds and watches whether the server cuts the connection
 * (Task 49+50, SEC-F-147, R-24). The two-second pace comes from a blocking read with a two-second timeout, never
 * from a sleep. Loopback only.
 */
public final class DripClient {

    private static final int PACE_MILLIS = 2000;

    private DripClient() {
    }

    /** How a drip ended: closed or not, the time, the first answer line, when it came (-1: never), bytes sent. */
    public record Result(boolean closed, long millis, String firstLine, long firstByteMillis, int bytesSent) {
    }

    /**
     * Sends the prefix at once, then the rest one byte every two seconds, until the server closes the
     * connection or the window passes.
     *
     * @param port the loopback port
     * @param prefix text sent at once (may be empty)
     * @param dripped text sent one byte at a time
     * @param windowMillis the longest time to keep the connection open
     * @param connected counted down once the prefix was sent (may be null)
     * @return what happened
     * @throws IOException when the connection cannot be opened
     */
    public static Result run(int port, String prefix, String dripped, long windowMillis,
            CountDownLatch connected) throws IOException {
        byte[] bytes = dripped.getBytes(StandardCharsets.ISO_8859_1);
        long start = System.nanoTime();
        StringBuilder seen = new StringBuilder();
        int sent = 0;
        long firstByte = -1;
        boolean closed = false;
        try (Socket socket = new Socket(InetAddress.getLoopbackAddress(), port)) {
            socket.setSoTimeout(PACE_MILLIS);
            socket.getOutputStream().write(prefix.getBytes(StandardCharsets.ISO_8859_1));
            if (connected != null) {
                connected.countDown();
            }
            InputStream input = socket.getInputStream();
            while (!closed && elapsedMillis(start) < windowMillis) {
                try {
                    int next = input.read();
                    if (next < 0) {
                        closed = true;
                    } else if (seen.length() < 64) {
                        firstByte = firstByte < 0 ? elapsedMillis(start) : firstByte;
                        seen.append((char) next);
                    }
                } catch (SocketTimeoutException pace) {
                    if (sent < bytes.length) {
                        closed = !writeByte(socket, bytes[sent]);
                        sent += closed ? 0 : 1;
                    }
                } catch (IOException reset) {
                    closed = true;
                }
            }
        }
        return new Result(closed, elapsedMillis(start), seen.toString().split("\r\n", 2)[0].trim(),
                firstByte, sent);
    }

    private static boolean writeByte(Socket socket, byte value) {
        try {
            socket.getOutputStream().write(value);
            socket.getOutputStream().flush();
            return true;
        } catch (IOException closedByServer) {
            return false;
        }
    }

    private static long elapsedMillis(long start) {
        return TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - start);
    }
}
