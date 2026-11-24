package com.jxxerick.customerapi.domain.model;

import com.jxxerick.customerapi.support.Fixtures;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class CustomerChangeTest {

    @Test
    void createdShouldListInitialValues() {
        Customer customer = Fixtures.customer();

        CustomerChange change = CustomerChange.created(customer, Fixtures.NOW);

        assertThat(change.type()).isEqualTo(ChangeType.CREATED);
        assertThat(change.fields()).containsExactly(
                new FieldChange("name", null, "Maria Silva"),
                new FieldChange("email", null, "maria@example.com"),
                new FieldChange("address", null, "Praça da Sé, 100 - Apto 12, Sé, São Paulo/SP, 01001-000"));
    }

    @Test
    void deletedShouldCarryNoFields() {
        assertThat(CustomerChange.deleted(Fixtures.customer(), Fixtures.NOW).fields()).isEmpty();
    }

    @Test
    void summaryShouldSkipMissingParts() {
        PostalAddress postal = new PostalAddress(new ZipCode("78890-000"), null, " ", "Sorriso", "MT");

        assertThat(new Address(postal, "S/N", null).summary()).isEqualTo("S/N, Sorriso/MT, 78890-000");
    }
}
