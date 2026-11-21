package com.jxxerick.customerapi.web.csv;

import java.util.ArrayList;
import java.util.List;

/**
 * Minimal RFC 4180 reader: quoted fields may hold delimiters, line breaks and doubled quotes.
 * The delimiter (comma or semicolon, as Excel uses in pt-BR) is detected from the header.
 */
public final class CsvParser {

    private static final char QUOTE = '"';

    private CsvParser() {
    }

    /** Blank lines are skipped; {@link CsvRecord#line()} is 1-based and counts physical lines. */
    public static List<CsvRecord> parse(String content) {
        String text = content.startsWith("﻿") ? content.substring(1) : content;
        char delimiter = detectDelimiter(text);

        List<CsvRecord> records = new ArrayList<>();
        List<String> fields = new ArrayList<>();
        StringBuilder field = new StringBuilder();
        boolean inQuotes = false;
        boolean wasQuoted = false;
        int line = 1;
        int recordStart = 1;

        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            if (inQuotes) {
                if (c == QUOTE && i + 1 < text.length() && text.charAt(i + 1) == QUOTE) {
                    field.append(QUOTE);
                    i++;
                } else if (c == QUOTE) {
                    inQuotes = false;
                } else {
                    if (c == '\n') {
                        line++;
                    }
                    field.append(c);
                }
            } else if (c == QUOTE && field.isEmpty() && !wasQuoted) {
                inQuotes = true;
                wasQuoted = true;
            } else if (c == delimiter) {
                fields.add(field.toString());
                field.setLength(0);
                wasQuoted = false;
            } else if (c == '\r' || c == '\n') {
                if (c == '\r' && i + 1 < text.length() && text.charAt(i + 1) == '\n') {
                    i++;
                }
                fields.add(field.toString());
                addIfNotBlank(records, recordStart, fields);
                fields = new ArrayList<>();
                field.setLength(0);
                wasQuoted = false;
                line++;
                recordStart = line;
            } else {
                field.append(c);
            }
        }
        if (inQuotes) {
            throw new InvalidCsvException("Aspas sem fechamento no registro da linha " + recordStart);
        }
        fields.add(field.toString());
        addIfNotBlank(records, recordStart, fields);
        return records;
    }

    private static void addIfNotBlank(List<CsvRecord> records, int line, List<String> fields) {
        boolean blank = fields.stream().allMatch(String::isBlank);
        if (!blank) {
            records.add(new CsvRecord(line, List.copyOf(fields)));
        }
    }

    /** Semicolon wins only when the header has more semicolons than commas. */
    static char detectDelimiter(String text) {
        int end = text.indexOf('\n');
        String header = end < 0 ? text : text.substring(0, end);
        long semicolons = header.chars().filter(c -> c == ';').count();
        long commas = header.chars().filter(c -> c == ',').count();
        return semicolons > commas ? ';' : ',';
    }

    public record CsvRecord(int line, List<String> values) {
    }
}
