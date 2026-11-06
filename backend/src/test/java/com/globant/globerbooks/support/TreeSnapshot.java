package com.globant.globerbooks.support;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

/** Lists names and describes whole trees (type, mode, owner, link target, SHA-256) without following links. */
public final class TreeSnapshot {

    private TreeSnapshot() {
    }

    public static List<String> names(Path directory) throws IOException {
        try (Stream<Path> entries = Files.list(directory)) {
            return entries.map(entry -> entry.getFileName().toString()).sorted().toList();
        }
    }

    public static List<String> snapshot(Path base) throws IOException {
        List<String> lines = new ArrayList<>();
        try (Stream<Path> walk = Files.walk(base)) {
            for (Path entry : walk.sorted().toList()) {
                lines.add(describe(base, entry));
            }
        }
        return lines;
    }

    private static String describe(Path base, Path entry) throws IOException {
        String owner = Files.getOwner(entry, LinkOption.NOFOLLOW_LINKS).getName();
        if (Files.isSymbolicLink(entry)) {
            return base.relativize(entry) + " link->" + Files.readSymbolicLink(entry) + " owner=" + owner;
        }
        String mode = FileFacts.mode(entry);
        if (Files.isRegularFile(entry, LinkOption.NOFOLLOW_LINKS)) {
            try {
                return base.relativize(entry) + " file " + mode + " " + owner + " " + FileFacts.sha256(entry);
            } catch (Exception failure) {
                throw new IOException(failure);
            }
        }
        return base.relativize(entry) + " dir " + mode + " " + owner;
    }
}
