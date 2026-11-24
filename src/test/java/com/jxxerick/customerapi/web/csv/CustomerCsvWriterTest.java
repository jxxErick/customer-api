package com.jxxerick.customerapi.web.csv;

import com.jxxerick.customerapi.application.command.CreateCustomerCommand;
import com.jxxerick.customerapi.application.command.ImportRow;
import com.jxxerick.customerapi.domain.model.Address;
import com.jxxerick.customerapi.domain.model.Cpf;
import com.jxxerick.customerapi.domain.model.Customer;
import com.jxxerick.customerapi.domain.model.Email;
import com.jxxerick.customerapi.support.Fixtures;
import org.junit.jupiter.api.Test;

import java.io.StringWriter;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class CustomerCsvWriterTest {

    @Test
    void shouldWriteHeaderAndAddressColumnsWithoutCpf() {
        StringWriter out = new StringWriter();
        CustomerCsvWriter writer = new CustomerCsvWriter(new CsvWriter(out, ';'));
        Customer customer = Customer.create("Maria Silva", new Email("maria@example.com"),
                new Cpf("529.982.247-25"), Fixtures.address(), Fixtures.NOW);

        writer.writeHeader();
        writer.write(customer);

        assertThat(out.toString().split("\r\n")).containsExactly(
                "name;email;zipCode;number;complement;street;neighborhood;city;state;createdAt",
                "Maria Silva;maria@example.com;01001-000;100;Apto 12;Praça da Sé;Sé;São Paulo;SP;2026-01-10T12:00:00Z");
        assertThat(out.toString()).doesNotContain("529");
    }

    @Test
    void exportShouldBeImportedBackUnchanged() {
        Customer tricky = Customer.create("=Equipe; \"Vendas\"", new Email("vendas@example.com"),
                new Address(Fixtures.postalAddress(), "-", "Bloco B, sala 2"), Fixtures.NOW);
        Customer plain = Fixtures.customer();
        StringWriter out = new StringWriter();
        CustomerCsvWriter writer = new CustomerCsvWriter(new CsvWriter(out, ';'));
        writer.writeHeader();
        writer.write(tricky);
        writer.write(plain);

        List<ImportRow> rows = CustomerCsvReader.read(out.toString());

        assertThat(rows).extracting(ImportRow::command).containsExactly(
                new CreateCustomerCommand("=Equipe; \"Vendas\"", "vendas@example.com", "01001-000", "-",
                        "Bloco B, sala 2", null),
                new CreateCustomerCommand("Maria Silva", "maria@example.com", "01001-000", "100", "Apto 12", null));
    }
}
