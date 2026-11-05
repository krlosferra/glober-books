package com.globant.globerbooks.config.transport;

import java.io.ByteArrayOutputStream;
import java.nio.ByteBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.StandardCharsets;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * The query grammar of the gate (spec 5.0, BR-012 steps 3 to 5) and the id rule (BR-011). Pure
 * functions over the raw query string and the raw path segment: nothing is read through the
 * servlet parameter API. On the list endpoint only {@code status} is allowed, once, with a value of
 * exactly {@code unread}, {@code reading} or {@code completed} after percent-decoding.
 */
public final class QueryPolicy {

    /** Request attribute that carries the decoded {@code status} value to the controller. */
    public static final String STATUS_ATTRIBUTE = "globerbooks.status";

    /** Pair count from which a query is refused. */
    public static final int PAIR_LIMIT = 2_000;

    private static final Pattern ID = Pattern.compile("[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}");
    private static final String STATUS = "status";
    private static final Set<String> STATUS_VALUES = Set.of("unread", "reading", "completed");

    private QueryPolicy() {
    }

    /** Outcome of the query check. */
    public enum Outcome {
        /** The query is acceptable. */
        OK,
        /** The shape is wrong: {@code MALFORMED_REQUEST}. */
        MALFORMED,
        /** The shape is right and the {@code status} value is not: {@code INVALID_STATUS}. */
        INVALID_STATUS
    }

    /**
     * Result of the query check.
     *
     * @param outcome the outcome
     * @param status the decoded {@code status} value when the outcome is {@code OK} and one was sent
     */
    public record Verdict(Outcome outcome, Optional<String> status) {
    }

    /**
     * Judges an id segment exactly as received: a lower-case, hyphenated, 36-character UUID. Any
     * {@code %}, upper-case letter or other length is malformed; the segment is never decoded.
     *
     * @param rawSegment the raw path segment
     * @return true when the id is well formed
     */
    public static boolean idWellFormed(String rawSegment) {
        return rawSegment != null && rawSegment.length() == 36 && ID.matcher(rawSegment).matches();
    }

    /**
     * Judges the raw query string of a request.
     *
     * @param rawQuery the query as received, without the {@code ?}; null or empty means none
     * @param listEndpoint true for the list endpoint, the only one that takes a query
     * @return the verdict
     */
    public static Verdict check(String rawQuery, boolean listEndpoint) {
        if (rawQuery == null || rawQuery.isEmpty()) {
            return new Verdict(Outcome.OK, Optional.empty());
        }
        if (!listEndpoint) {
            return malformed();
        }
        String[] pairs = rawQuery.split("&", -1);
        if (pairs.length >= PAIR_LIMIT) {
            return malformed();
        }
        Set<String> seen = new HashSet<>();
        String value = null;
        for (String pair : pairs) {
            int equals = pair.indexOf('=');
            Optional<String> name = decode(equals < 0 ? pair : pair.substring(0, equals));
            Optional<String> decoded = decode(equals < 0 ? "" : pair.substring(equals + 1));
            if (pair.isEmpty() || name.isEmpty() || decoded.isEmpty() || !STATUS.equals(name.get())
                    || !seen.add(name.get())) {
                return malformed();
            }
            value = decoded.get();
        }
        if (!STATUS_VALUES.contains(value)) {
            return new Verdict(Outcome.INVALID_STATUS, Optional.empty());
        }
        return new Verdict(Outcome.OK, Optional.of(value));
    }

    private static Verdict malformed() {
        return new Verdict(Outcome.MALFORMED, Optional.empty());
    }

    private static int hex(char digit) {
        return digit < 0x80 ? Character.digit(digit, 16) : -1;
    }

    /** Percent-decodes ({@code +} is a space); empty when the escape or the UTF-8 is invalid. */
    private static Optional<String> decode(String raw) {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream(raw.length());
        for (int index = 0; index < raw.length(); index++) {
            char current = raw.charAt(index);
            if (current == '%') {
                int high = index + 2 < raw.length() ? hex(raw.charAt(index + 1)) : -1;
                int low = index + 2 < raw.length() ? hex(raw.charAt(index + 2)) : -1;
                if (high < 0 || low < 0) {
                    return Optional.empty();
                }
                bytes.write(high * 16 + low);
                index += 2;
            } else if (current > 0xFF) {
                return Optional.empty();
            } else {
                bytes.write(current == '+' ? ' ' : current);
            }
        }
        try {
            return Optional.of(StandardCharsets.UTF_8.newDecoder()
                    .onMalformedInput(CodingErrorAction.REPORT)
                    .onUnmappableCharacter(CodingErrorAction.REPORT)
                    .decode(ByteBuffer.wrap(bytes.toByteArray())).toString());
        } catch (CharacterCodingException invalid) {
            return Optional.empty();
        }
    }
}
