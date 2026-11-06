package com.globant.globerbooks.support;

import java.io.IOException;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

/**
 * The packaged application as a separate operating-system process (Task 33). The command is the documented
 * one: {@code java -Xmx512m -jar target/globerbooks.jar} with the bind, the port and the data source as
 * command-line arguments, loopback only, on a free port chosen by the operating system (never 8080, and not
 * 18080, which is reserved for the end-to-end suite). The environment is built from nothing but an allowlist,
 * like the end-to-end launcher. The console (standard output and error) goes to a file outside the data
 * directory. Every process started here is also stopped by a shutdown hook, so none outlives the test JVM.
 */
public final class AppProcess {

    private static final Set<Process> LIVE = ConcurrentHashMap.newKeySet();
    private static final List<String> ALLOWED_ENVIRONMENT = List.of("PATH", "HOME", "JAVA_HOME", "LANG", "TZ");
    private static final long READY_SECONDS = 90;
    private static final long POLL_MILLIS = 100;
    private static final long STOP_SECONDS = 30;

    static {
        Runtime.getRuntime().addShutdownHook(new Thread(() -> LIVE.forEach(Process::destroyForcibly)));
    }

    private final Process process;
    private final int port;
    private final Path console;
    private final long startupMillis;

    private AppProcess(Process process, int port, Path console, long startupMillis) {
        this.process = process;
        this.port = port;
        this.console = console;
        this.startupMillis = startupMillis;
    }

    /**
     * Starts the packaged jar on a free loopback port against a database file in the given directory and
     * waits until it answers (polling with a deadline).
     *
     * @param dataDir the data directory (created by the application when missing)
     * @param consoleFile the file that receives standard output and error
     * @return the running process
     * @throws Exception when the jar is missing, the process cannot start or it does not become ready
     */
    public static AppProcess start(Path dataDir, Path consoleFile) throws Exception {
        Path jar = Path.of("target", "globerbooks.jar").toAbsolutePath();
        if (!Files.isRegularFile(jar)) {
            throw new IllegalStateException("The packaged jar is missing: " + jar);
        }
        int port = freePort();
        ProcessBuilder builder = new ProcessBuilder(
                Path.of(System.getProperty("java.home"), "bin", "java").toString(), "-Xmx512m", "-jar",
                jar.toString(), "--server.address=127.0.0.1", "--server.port=" + port,
                "--spring.datasource.url=jdbc:sqlite:" + dataDir.resolve("library.db"))
                .redirectErrorStream(true).redirectOutput(consoleFile.toFile());
        Map<String, String> environment = builder.environment();
        Map<String, String> inherited = Map.copyOf(environment);
        environment.clear();
        ALLOWED_ENVIRONMENT.stream().filter(inherited::containsKey)
                .forEach(key -> environment.put(key, inherited.get(key)));
        long begin = System.nanoTime();
        Process process = builder.start();
        LIVE.add(process);
        AppProcess app = new AppProcess(process, port, consoleFile, 0);
        try {
            app.awaitReady();
        } catch (Exception failure) {
            app.kill();
            throw failure;
        }
        return new AppProcess(process, port, consoleFile, (System.nanoTime() - begin) / 1_000_000);
    }

    private static int freePort() throws IOException {
        try (ServerSocket socket = new ServerSocket(0, 1, InetAddress.getLoopbackAddress())) {
            return socket.getLocalPort();
        }
    }

    private void awaitReady() throws Exception {
        ApiCalls probe = new ApiCalls(port);
        long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(READY_SECONDS);
        while (System.nanoTime() < deadline) {
            if (process.waitFor(POLL_MILLIS, TimeUnit.MILLISECONDS)) {
                throw new IllegalStateException("The application ended before it was ready, exit "
                        + process.exitValue());
            }
            try {
                if (probe.call("GET", "/api/v1/books", null).status() == 200) {
                    return;
                }
            } catch (IOException notYet) {
                continue;
            }
        }
        throw new IllegalStateException("The application was not ready within " + READY_SECONDS + " s");
    }

    /**
     * Returns a client for this process.
     *
     * @return the client
     */
    public ApiCalls api() {
        return new ApiCalls(port);
    }

    /**
     * Returns the milliseconds from the start of the process to its first answer.
     *
     * @return the start-up time
     */
    public long startupMillis() {
        return startupMillis;
    }

    /**
     * Returns whether the process is still running.
     *
     * @return true while it runs
     */
    public boolean isAlive() {
        return process.isAlive();
    }

    /**
     * Stops the process normally (SIGTERM, then the application's own shutdown) and waits for it.
     *
     * @return the exit status
     * @throws InterruptedException when the wait is interrupted
     */
    public int stop() throws InterruptedException {
        process.destroy();
        if (!process.waitFor(STOP_SECONDS, TimeUnit.SECONDS)) {
            kill();
        }
        forget();
        return process.exitValue();
    }

    /**
     * Kills the process at once (SIGKILL) and waits until it is gone.
     *
     * @throws InterruptedException when the wait is interrupted
     */
    public void kill() throws InterruptedException {
        process.destroyForcibly();
        process.waitFor(STOP_SECONDS, TimeUnit.SECONDS);
        forget();
    }

    private void forget() {
        if (!process.isAlive()) {
            LIVE.remove(process);
        }
    }

    /**
     * Returns everything the process has written to its console so far.
     *
     * @return the console text
     * @throws IOException when the file cannot be read
     */
    public String console() throws IOException {
        return Files.readString(console, StandardCharsets.UTF_8);
    }
}
