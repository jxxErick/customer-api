package com.jxxerick.customerapi.web.csv;

import com.jxxerick.customerapi.web.csv.CsvParser.CsvRecord;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class CsvParserTest {

    @Test
    void shouldSplitSimpleRecords() {
        List<CsvRecord> records = CsvParser.parse("a,b,c\n1,2,3\n");

        assertThat(records).containsExactly(
                new CsvRecord(1, List.of("a", "b", "c")),
                new CsvRecord(2, List.of("1", "2", "3")));
    }

    @Test
    void shouldKeepDelimitersQuotesAndLineBreaksInsideQuotedFields() {
        List<CsvRecord> records = CsvParser.parse("""
                name,complement
                "Silva, Maria","Bloco ""B""
                fundos"
                João,
                """);

        assertThat(records.get(1).values()).containsExactly("Silva, Maria", "Bloco \"B\"\nfundos");
        assertThat(records.get(2)).isEqualTo(new CsvRecord(4, List.of("João", "")));
    }

    @Test
    void shouldHandleCrlfBomAndBlankLines() {
        List<CsvRecord> records = CsvParser.parse("﻿a,b\r\n\r\n1,2\r\n  ,  \r\n3,4");

        assertThat(records).extracting(CsvRecord::line).containsExactly(1, 3, 5);
        assertThat(records.getFirst().values()).containsExactly("a", "b");
        assertThat(records.getLast().values()).containsExactly("3", "4");
    }

    @Test
    void shouldDetectSemicolonFromExcelExports() {
        List<CsvRecord> records = CsvParser.parse("nome;cep;complemento\nMaria;01001-000;Apto 1, fundos");

        assertThat(records.get(1).values()).containsExactly("Maria", "01001-000", "Apto 1, fundos");
    }

    @Test
    void shouldRejectUnterminatedQuote() {
        assertThatThrownBy(() -> CsvParser.parse("a,b\n\"oops,1\n2,3"))
                .isInstanceOf(InvalidCsvException.class)
                .hasMessageContaining("linha 2");
    }
}
