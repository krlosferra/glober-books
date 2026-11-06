package com.globant.globerbooks.support;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

/** Starts {@code ClaimChildMain} in its own JVM (no shell string) and hashes files. */
public final class ChildJvm {

    private static final int WAIT_SECONDS = 60;

    private ChildJvm() {
    }

    private static ProcessBuilder childBuilder(String mode, Path file) {
        String java = Path.of(System.getProperty("java.home"), "bin", "java").toString();
        return new ProcessBuilder(java, "-cp", System.getProperty("java.class.path"),
                "com.globant.globerbooks.config.sqlite.ClaimChildMain", mode, file.toString())
                .redirectErrorStream(true);
    }

    public static Process startHolder(Path file) throws Exception {
        Process process = childBuilder("hold", file).start();
        try {
            BufferedReader reader = new BufferedReader(
                    new InputStreamReader(process.getInputStream(), StandardCharsets.UTF_8));
            CompletableFuture<Boolean> ready = CompletableFuture.supplyAsync(() -> awaitReady(reader));
            if (!ready.get(WAIT_SECONDS, TimeUnit.SECONDS)) {
                throw new IllegalStateException("The holder JVM ended before READY");
            }
            return process;
        } catch (Exception failure) {
            process.destroyForcibly();
            throw failure;
        }
    }

    private static boolean awaitReady(BufferedReader reader) {
        try {
            String line = reader.readLine();
            while (line != null) {
                if ("READY".equals(line)) {
                    return true;
                }
                line = reader.readLine();
            }
            return false;
        } catch (IOException failure) {
            return false;
        }
    }

    public static void stop(Process holder) throws Exception {
        if (holder == null) {
            return;
        }
        try {
            holder.getOutputStream().write('\n');
            holder.getOutputStream().flush();
            if (!holder.waitFor(WAIT_SECONDS, TimeUnit.SECONDS)) {
                holder.destroyForcibly();
            }
        } finally {
            holder.destroyForcibly().waitFor(WAIT_SECONDS, TimeUnit.SECONDS);
        }
    }

    public static ChildResult runChild(String mode, Path file) throws Exception {
        Process process = childBuilder(mode, file).start();
        try {
            CompletableFuture<String> output = CompletableFuture.supplyAsync(() -> readAll(process));
            boolean finished = process.waitFor(WAIT_SECONDS, TimeUnit.SECONDS);
            if (!finished) {
                throw new IllegalStateException("The child JVM did not finish");
            }
            return new ChildResult(process.exitValue(), output.get(WAIT_SECONDS, TimeUnit.SECONDS));
        } finally {
            process.destroyForcibly();
        }
    }

    private static String readAll(Process process) {
        try {
            return new String(process.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException failure) {
            return "";
        }
    }

    public record ChildResult(int exit, String output) {
    }
}
