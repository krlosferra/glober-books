package com.globant.globerbooks.support;

import java.io.IOException;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
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
        return start("jdbc:sqlite:" + dataDir.resolve("library.db"), null, null, consoleFile);
    }

    /**
     * Starts the packaged jar with a given data source URL, an optional umask and an optional working
     * directory, and waits until it answers.
     *
     * @param dataUrl the value of {@code --spring.datasource.url}, or null to use the shipped default
     * @param umask the umask the child runs under (for example {@code 022}), or null for the inherited one
     * @param workDir the working directory of the child, or null for the current one
     * @param consoleFile the file that receives standard output and error
     * @return the running process
     * @throws Exception when the process cannot start or does not become ready
     */
    public static AppProcess start(String dataUrl, String umask, Path workDir, Path consoleFile)
            throws Exception {
        return start(dataUrl, umask, workDir, consoleFile, List.of());
    }

    /**
     * Starts the packaged jar like {@link #start(String, String, Path, Path)} and adds JVM options after the
     * fixed {@code -Xmx512m} (for example a garbage-collector log of the child, Task 41+42).
     *
     * @param dataUrl the value of {@code --spring.datasource.url}, or null to use the shipped default
     * @param umask the umask the child runs under, or null for the inherited one
     * @param workDir the working directory of the child, or null for the current one
     * @param consoleFile the file that receives standard output and error
     * @param jvmOptions extra JVM options
     * @return the running process
     * @throws Exception when the process cannot start or does not become ready
     */
    public static AppProcess start(String dataUrl, String umask, Path workDir, Path consoleFile,
            List<String> jvmOptions) throws Exception {
        int port = freePort();
        long begin = System.nanoTime();
        Process process = launch(dataUrl, umask, workDir, consoleFile, port, jvmOptions);
        AppProcess app = new AppProcess(process, port, consoleFile, 0);
        try {
            app.awaitReady();
        } catch (Exception failure) {
            app.kill();
            throw failure;
        }
        return new AppProcess(process, port, consoleFile, (System.nanoTime() - begin) / 1_000_000);
    }

    /**
     * Starts the packaged jar and waits for it to end by itself (a refused start or a stop).
     *
     * @param dataUrl the value of {@code --spring.datasource.url}, or null to use the shipped default
     * @param umask the umask of the child, or null
     * @param workDir the working directory of the child, or null
     * @param consoleFile the file that receives standard output and error
     * @return the exit status
     * @throws Exception when the process cannot start or is still running after the deadline
     */
    public static int runToExit(String dataUrl, String umask, Path workDir, Path consoleFile) throws Exception {
        Process process = launch(dataUrl, umask, workDir, consoleFile, freePort(), List.of());
        if (!process.waitFor(READY_SECONDS, TimeUnit.SECONDS)) {
            process.destroyForcibly().waitFor(STOP_SECONDS, TimeUnit.SECONDS);
            LIVE.remove(process);
            throw new IllegalStateException("The application did not end within " + READY_SECONDS + " s");
        }
        LIVE.remove(process);
        return process.exitValue();
    }

    private static Process launch(String dataUrl, String umask, Path workDir, Path consoleFile, int port,
            List<String> jvmOptions) throws IOException {
        Path jar = Path.of("target", "globerbooks.jar").toAbsolutePath();
        if (!Files.isRegularFile(jar)) {
            throw new IllegalStateException("The packaged jar is missing: " + jar);
        }
        List<String> command = new ArrayList<>();
        if (umask != null) {
            command.addAll(List.of("sh", "-c", "umask \"$1\" && shift && exec \"$@\"", "sh", umask));
        }
        command.addAll(List.of(Path.of(System.getProperty("java.home"), "bin", "java").toString(), "-Xmx512m"));
        command.addAll(jvmOptions);
        command.addAll(List.of("-jar", jar.toString(), "--server.address=127.0.0.1", "--server.port=" + port));
        if (dataUrl != null) {
            command.add("--spring.datasource.url=" + dataUrl);
        }
        ProcessBuilder builder = new ProcessBuilder(command)
                .redirectErrorStream(true).redirectOutput(consoleFile.toFile());
        if (workDir != null) {
            builder.directory(workDir.toFile());
        }
        Map<String, String> environment = builder.environment();
        Map<String, String> inherited = Map.copyOf(environment);
        environment.clear();
        ALLOWED_ENVIRONMENT.stream().filter(inherited::containsKey)
                .forEach(key -> environment.put(key, inherited.get(key)));
        Process process = builder.start();
        LIVE.add(process);
        return process;
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
     * Returns the operating-system process id.
     *
     * @return the pid
     */
    public long pid() {
        return process.pid();
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
