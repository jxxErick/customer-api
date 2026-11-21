package com.jxxerick.customerapi.web.csv;

import com.jxxerick.customerapi.application.command.CreateCustomerCommand;
import com.jxxerick.customerapi.application.command.ImportRow;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class CustomerCsvReaderTest {

    @Test
    void shouldReadEnglishHeaderInAnyOrder() {
        List<ImportRow> rows = CustomerCsvReader.read("""
                email,name,number,zipCode,cpf
                maria@example.com,Maria Silva,100,01001-000,529.982.247-25
                """);

        assertThat(rows).containsExactly(new ImportRow(2, new CreateCustomerCommand(
                "Maria Silva", "maria@example.com", "01001-000", "100", null, "529.982.247-25")));
    }

    @Test
    void shouldAcceptPortugueseHeadersWithAccentsAndIgnoreUnknownColumns() {
        List<ImportRow> rows = CustomerCsvReader.read("""
                Nome;E-mail;CEP;Número;Complemento;Observação
                João Pereira;joao@example.com;20040020;5;Sala 3;cliente antigo
                """);

        assertThat(rows.getFirst().command()).isEqualTo(new CreateCustomerCommand(
                "João Pereira", "joao@example.com", "20040020", "5", "Sala 3", null));
    }

    @Test
    void shouldTurnBlankAndMissingCellsIntoNull() {
        List<ImportRow> rows = CustomerCsvReader.read("""
                name,email,zipCode,number,complement
                Ana,  ,01001000
                """);

        CreateCustomerCommand command = rows.getFirst().command();
        assertThat(command.email()).isNull();
        assertThat(command.number()).isNull();
        assertThat(command.complement()).isNull();
    }

    @Test
    void shouldListMissingRequiredColumns() {
        assertThatThrownBy(() -> CustomerCsvReader.read("name,cpf\nAna,52998224725"))
                .isInstanceOf(InvalidCsvException.class)
                .hasMessage("Cabeçalho sem as colunas obrigatórias: email, zipCode, number");
    }

    @Test
    void shouldRejectRepeatedColumns() {
        assertThatThrownBy(() -> CustomerCsvReader.read("name,nome,email,cep,numero\n"))
                .isInstanceOf(InvalidCsvException.class)
                .hasMessageContaining("nome");
    }

    @Test
    void shouldRejectEmptyFile() {
        assertThatThrownBy(() -> CustomerCsvReader.read(" \n "))
                .isInstanceOf(InvalidCsvException.class)
                .hasMessage("Arquivo vazio");
    }
}
