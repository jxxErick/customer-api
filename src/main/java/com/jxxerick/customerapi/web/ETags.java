package com.jxxerick.customerapi.web;

/** Converts between the customer version and the ETag / If-Match header values. */
final class ETags {

    private ETags() {
    }

    static String of(Long version) {
        return "\"" + version + "\"";
    }

    /** Accepts {@code "3"} or {@code W/"3"}; a missing header or {@code *} means "no check" (null). */
    static Long parseIfMatch(String header) {
        if (header == null || header.isBlank() || header.strip().equals("*")) {
            return null;
        }
        String value = header.strip();
        if (value.startsWith("W/")) {
            value = value.substring(2);
        }
        if (value.length() < 3 || !value.startsWith("\"") || !value.endsWith("\"")) {
            throw new InvalidIfMatchException(header);
        }
        try {
            return Long.parseLong(value.substring(1, value.length() - 1));
        } catch (NumberFormatException exception) {
            throw new InvalidIfMatchException(header);
        }
    }
}
