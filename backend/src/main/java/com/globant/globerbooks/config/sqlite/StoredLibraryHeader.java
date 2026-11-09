package com.globant.globerbooks.config.sqlite;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
import java.nio.channels.OverlappingFileLockException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.Arrays;

/**
 * The first step of the stored-library validation (Task 34b, US-009.5, SEC-F-074): the 100-byte header
 * of a non-empty database file, read with plain I/O before the pool opens, so that a non-SQLite or
 * WAL-marked file is refused before SQLite touches it and no journal is applied to it.
 */
final class StoredLibraryHeader {

    private static final byte[] MAGIC = "SQLite format 3\0".getBytes(StandardCharsets.ISO_8859_1);
    private static final int HEADER_BYTES = 100;

    private StoredLibraryHeader() {
    }

    /**
     * Reads the 100-byte header of a non-empty file with plain I/O. It must run before the pool opens,
     * because it takes and releases a shared lock on the whole file (a lock held by another process
     * means the file is in use) and closing a descriptor would drop locks held by this process.
     * The size test (file size equals page size times page count) is skipped when a non-empty
     * {@code -journal} exists, because the page count is valid only after a commit.
     *
     * @param databaseFile the vetted database file
     * @throws StoredLibraryValidator.Refusal for a file that is not a database, is WAL-marked or is damaged
     * @throws DataDirectoryGuard.Refusal with the unavailable reason when another process holds the file
     */
    static void check(Path databaseFile) {
        try (FileChannel channel = FileChannel.open(databaseFile, StandardOpenOption.READ)) {
            long size = channel.size();
            if (size == 0) {
                return;
            }
            FileLock lock = channel.tryLock(0, Long.MAX_VALUE, true);
            if (lock == null) {
                throw new DataDirectoryGuard.Refusal(DataDirectoryGuard.Reason.DATABASE_UNAVAILABLE);
            }
            ByteBuffer header = ByteBuffer.allocate(HEADER_BYTES);
            while (header.hasRemaining() && channel.read(header) > 0) {
                continue;
            }
            require(header.array(), header.position(), size, hasHotJournal(databaseFile));
        } catch (IOException | OverlappingFileLockException failure) {
            throw new DataDirectoryGuard.Refusal(DataDirectoryGuard.Reason.DATABASE_UNAVAILABLE);
        }
    }

    static void require(byte[] header, int length, long size, boolean journalPresent) {
        if (length < HEADER_BYTES || !Arrays.equals(Arrays.copyOf(header, MAGIC.length), MAGIC)) {
            throw new StoredLibraryValidator.Refusal(StoredLibraryValidator.Reason.NOT_A_DATABASE);
        }
        int writeVersion = header[18] & 0xFF;
        int readVersion = header[19] & 0xFF;
        if (writeVersion == 2 || readVersion == 2) {
            throw new StoredLibraryValidator.Refusal(StoredLibraryValidator.Reason.WAL_MARKED);
        }
        if (writeVersion != 1 || readVersion != 1) {
            throw new StoredLibraryValidator.Refusal(StoredLibraryValidator.Reason.NOT_A_DATABASE);
        }
        ByteBuffer fields = ByteBuffer.wrap(header);
        int rawPageSize = fields.getShort(16) & 0xFFFF;
        long pageSize = rawPageSize == 1 ? 65_536 : rawPageSize;
        if (pageSize < 512 || pageSize > 65_536 || Long.bitCount(pageSize) != 1) {
            throw new StoredLibraryValidator.Refusal(StoredLibraryValidator.Reason.DAMAGED);
        }
        long pageCount = fields.getInt(28) & 0xFFFFFFFFL;
        boolean countIsValid = pageCount != 0 && fields.getInt(24) == fields.getInt(92);
        if (!journalPresent && countIsValid && size != pageSize * pageCount) {
            throw new StoredLibraryValidator.Refusal(StoredLibraryValidator.Reason.DAMAGED);
        }
    }

    private static boolean hasHotJournal(Path databaseFile) throws IOException {
        Path journal = databaseFile.resolveSibling(databaseFile.getFileName() + "-journal");
        return Files.isRegularFile(journal) && Files.size(journal) > 0;
    }
}
