package com.jxxerick.customerapi.web.csv;

import org.junit.jupiter.api.Test;

import java.io.StringWriter;
import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class CsvWriterTest {

    private final StringWriter out = new StringWriter();
    private final CsvWriter writer = new CsvWriter(out, ';');

    @Test
    void shouldWriteRowsWithCrlfAndEmptyNulls() {
        writer.writeRow(Arrays.asList("Maria", null, "", "SP"));

        assertThat(out).hasToString("Maria;;;SP\r\n");
    }

    @Test
    void shouldQuoteDelimitersQuotesAndLineBreaks() {
        writer.writeRow(List.of("Bloco A; Apto 3", "Casa \"amarela\"", "linha 1\nlinha 2"));

        assertThat(out).hasToString("\"Bloco A; Apto 3\";\"Casa \"\"amarela\"\"\";\"linha 1\nlinha 2\"\r\n");
    }

    @Test
    void shouldGuardCellsThatLookLikeFormulas() {
        assertThat(writer.escape("=HYPERLINK(\"x\")")).isEqualTo("\"'=HYPERLINK(\"\"x\"\")\"");
        assertThat(writer.escape("+5511")).isEqualTo("'+5511");
        assertThat(writer.escape("@SUM(A1)")).isEqualTo("'@SUM(A1)");
        assertThat(writer.escape("-")).isEqualTo("'-");
        assertThat(writer.escape("Ana-Maria")).isEqualTo("Ana-Maria");
    }

    @Test
    void commaDelimiterShouldQuoteCommasOnly() {
        CsvWriter commas = new CsvWriter(new StringWriter(), ',');

        assertThat(commas.escape("Rua A, 10")).isEqualTo("\"Rua A, 10\"");
        assertThat(commas.escape("Bloco A; Apto 3")).isEqualTo("Bloco A; Apto 3");
    }
}
