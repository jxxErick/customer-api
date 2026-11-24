package com.jxxerick.customerapi.web.csv;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.io.Writer;
import java.util.List;

/**
 * RFC 4180 writer. Cells that a spreadsheet would run as a formula (=, +, -, @, tab, CR) get a
 * leading apostrophe, which {@link CustomerCsvReader} strips back on import.
 */
public final class CsvWriter {

    static final char FORMULA_GUARD = '\'';
    private static final String FORMULA_STARTS = "=+-@\t\r";

    private final Writer out;
    private final char delimiter;

    public CsvWriter(Writer out, char delimiter) {
        this.out = out;
        this.delimiter = delimiter;
    }

    /** Null cells are written empty; rows end with CRLF. */
    public void writeRow(List<String> cells) {
        try {
            for (int i = 0; i < cells.size(); i++) {
                if (i > 0) {
                    out.write(delimiter);
                }
                out.write(escape(cells.get(i)));
            }
            out.write("\r\n");
        } catch (IOException exception) {
            throw new UncheckedIOException(exception);
        }
    }

    String escape(String cell) {
        if (cell == null || cell.isEmpty()) {
            return "";
        }
        String value = startsLikeFormula(cell) ? FORMULA_GUARD + cell : cell;
        boolean needsQuotes = value.indexOf(delimiter) >= 0 || value.indexOf('"') >= 0
                || value.indexOf('\n') >= 0 || value.indexOf('\r') >= 0;
        return needsQuotes ? '"' + value.replace("\"", "\"\"") + '"' : value;
    }

    static boolean startsLikeFormula(String cell) {
        return !cell.isEmpty() && FORMULA_STARTS.indexOf(cell.charAt(0)) >= 0;
    }
}
