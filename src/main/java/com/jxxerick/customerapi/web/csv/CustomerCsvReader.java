package com.jxxerick.customerapi.web.csv;

import com.jxxerick.customerapi.application.command.CreateCustomerCommand;
import com.jxxerick.customerapi.application.command.ImportRow;
import com.jxxerick.customerapi.web.csv.CsvParser.CsvRecord;

import java.text.Normalizer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/** Maps a customer CSV (header in English or Portuguese, any column order) to import rows. */
public final class CustomerCsvReader {

    private static final Map<String, Column> HEADER_ALIASES = Map.ofEntries(
            Map.entry("name", Column.NAME), Map.entry("nome", Column.NAME),
            Map.entry("email", Column.EMAIL), Map.entry("e-mail", Column.EMAIL),
            Map.entry("zipcode", Column.ZIP_CODE), Map.entry("cep", Column.ZIP_CODE),
            Map.entry("number", Column.NUMBER), Map.entry("numero", Column.NUMBER),
            Map.entry("complement", Column.COMPLEMENT), Map.entry("complemento", Column.COMPLEMENT),
            Map.entry("cpf", Column.CPF));

    private CustomerCsvReader() {
    }

    public static List<ImportRow> read(String content) {
        List<CsvRecord> records = CsvParser.parse(content);
        if (records.isEmpty()) {
            throw new InvalidCsvException("Arquivo vazio");
        }
        Map<Column, Integer> positions = mapHeader(records.getFirst());

        List<ImportRow> rows = new ArrayList<>(records.size() - 1);
        for (CsvRecord record : records.subList(1, records.size())) {
            rows.add(new ImportRow(record.line(), new CreateCustomerCommand(
                    value(record, positions, Column.NAME),
                    value(record, positions, Column.EMAIL),
                    value(record, positions, Column.ZIP_CODE),
                    value(record, positions, Column.NUMBER),
                    value(record, positions, Column.COMPLEMENT),
                    value(record, positions, Column.CPF))));
        }
        return rows;
    }

    private static Map<Column, Integer> mapHeader(CsvRecord header) {
        Map<Column, Integer> positions = new HashMap<>();
        for (int i = 0; i < header.values().size(); i++) {
            Column column = HEADER_ALIASES.get(normalize(header.values().get(i)));
            if (column != null && positions.putIfAbsent(column, i) != null) {
                throw new InvalidCsvException("Coluna repetida no cabeçalho: " + header.values().get(i).strip());
            }
        }
        List<String> missing = Column.REQUIRED.stream()
                .filter(column -> !positions.containsKey(column))
                .map(column -> column.header)
                .toList();
        if (!missing.isEmpty()) {
            throw new InvalidCsvException("Cabeçalho sem as colunas obrigatórias: " + String.join(", ", missing));
        }
        return positions;
    }

    /** Missing trailing cells and blank cells become null. */
    private static String value(CsvRecord record, Map<Column, Integer> positions, Column column) {
        Integer index = positions.get(column);
        if (index == null || index >= record.values().size()) {
            return null;
        }
        String value = unguard(record.values().get(index).strip());
        return value.isEmpty() ? null : value;
    }

    /** Undoes the apostrophe {@link CsvWriter} puts before formula-like cells. */
    private static String unguard(String value) {
        return value.length() > 1 && value.charAt(0) == CsvWriter.FORMULA_GUARD
                && CsvWriter.startsLikeFormula(value.substring(1)) ? value.substring(1) : value;
    }

    /** "Número", " CEP " and "zipCode" all become simple lowercase keys. */
    private static String normalize(String header) {
        String withoutAccents = Normalizer.normalize(header.strip(), Normalizer.Form.NFD).replaceAll("\\p{M}", "");
        return withoutAccents.toLowerCase(Locale.ROOT).replace("_", "");
    }

    private enum Column {
        NAME("name"), EMAIL("email"), ZIP_CODE("zipCode"), NUMBER("number"), COMPLEMENT("complement"), CPF("cpf");

        static final List<Column> REQUIRED = List.of(NAME, EMAIL, ZIP_CODE, NUMBER);

        private final String header;

        Column(String header) {
            this.header = header;
        }
    }
}
