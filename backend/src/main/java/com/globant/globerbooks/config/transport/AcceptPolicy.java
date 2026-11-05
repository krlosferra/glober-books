package com.globant.globerbooks.config.transport;

import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;

/**
 * The {@code Accept} grammar of spec 5.0 for API paths. Absent or blank is accepted. Otherwise the
 * header is a comma-separated list of {@code type/subtype} ranges with optional parameters; only
 * {@code q} is read, a range with {@code q} of zero is ignored, and the request is acceptable when
 * a remaining range is {@code application/json}, {@code application/*} or {@code *}{@code /*}. An
 * unparseable header is not acceptable.
 */
public final class AcceptPolicy {

    private static final Pattern TOKEN = Pattern.compile("[!#$%&'*+.^_`|~0-9A-Za-z-]+");
    private static final Pattern Q_VALUE = Pattern.compile("0(\\.[0-9]{0,3})?|1(\\.0{0,3})?");
    private static final Pattern ZERO = Pattern.compile("0(\\.0*)?");

    private AcceptPolicy() {
    }

    /**
     * Judges the {@code Accept} header lines of a request. Several lines are joined as one list.
     *
     * @param values the values of every {@code Accept} header line (empty when absent)
     * @return true when the request is acceptable
     */
    public static boolean acceptable(List<String> values) {
        String joined = String.join(",", values);
        if (joined.isBlank()) {
            return true;
        }
        boolean matched = false;
        for (String element : joined.split(",", -1)) {
            if (element.isBlank()) {
                continue;
            }
            Range range = parse(element);
            if (range == null) {
                return false;
            }
            matched |= !range.excluded && range.matchesJson();
        }
        return matched;
    }

    private static Range parse(String element) {
        String[] parts = element.split(";", -1);
        String[] names = parts[0].trim().toLowerCase(Locale.ROOT).split("/", -1);
        if (names.length != 2 || !TOKEN.matcher(names[0]).matches() || !TOKEN.matcher(names[1]).matches()
                || "*".equals(names[0]) && !"*".equals(names[1])) {
            return null;
        }
        boolean excluded = false;
        for (int index = 1; index < parts.length; index++) {
            String parameter = parts[index].trim();
            int equals = parameter.indexOf('=');
            String name = (equals < 0 ? parameter : parameter.substring(0, equals)).trim();
            if (!"q".equalsIgnoreCase(name)) {
                continue;
            }
            String value = equals < 0 ? "" : parameter.substring(equals + 1).trim();
            if (!Q_VALUE.matcher(value).matches()) {
                return null;
            }
            excluded = ZERO.matcher(value).matches();
        }
        return new Range(names[0], names[1], excluded);
    }

    private record Range(String type, String subtype, boolean excluded) {

        boolean matchesJson() {
            return "*".equals(type) || "application".equals(type) && ("json".equals(subtype) || "*".equals(subtype));
        }
    }
}
