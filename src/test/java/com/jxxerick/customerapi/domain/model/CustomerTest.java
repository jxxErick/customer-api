package com.jxxerick.customerapi.domain.model;

import com.jxxerick.customerapi.domain.exception.DomainValidationException;
import com.jxxerick.customerapi.support.Fixtures;
import org.junit.jupiter.api.Test;

import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class CustomerTest {

    @Test
    void shouldCreateCustomerWithGeneratedId() {
        Customer customer = Fixtures.customer();

        assertThat(customer.getId()).isNotNull();
        assertThat(customer.getName()).isEqualTo("Maria Silva");
        assertThat(customer.getCreatedAt()).isEqualTo(Fixtures.NOW);
    }

    @Test
    void shouldRejectBlankName() {
        Email email = new Email("maria@example.com");

        assertThatThrownBy(() -> Customer.create(" ", email, Fixtures.address(), Fixtures.NOW))
                .isInstanceOf(DomainValidationException.class)
                .hasMessage("Nome é obrigatório");
    }

    @Test
    void shouldMoveToNewAddress() {
        Customer customer = Fixtures.customer();
        Address newAddress = new Address(Fixtures.postalAddress(), "200", null);

        customer.moveTo(newAddress);

        assertThat(customer.getAddress()).isEqualTo(newAddress);
    }

    @Test
    void shouldBeEqualByIdentity() {
        UUID id = UUID.randomUUID();
        Email email = new Email("maria@example.com");
        Customer first = Customer.restore(id, "Maria", email, null, Fixtures.address(), Fixtures.NOW, 0L);
        Customer second = Customer.restore(id, "Other name", email, null, Fixtures.address(), Fixtures.NOW, 0L);

        assertThat(first).isEqualTo(second).hasSameHashCodeAs(second);
    }

    @Test
    void addressShouldDropBlankComplement() {
        assertThat(new Address(Fixtures.postalAddress(), "10", "  ").complement()).isNull();
    }

    @Test
    void changeTagsShouldKeepUntouchedTagsAndReportNoOp() {
        Customer customer = Fixtures.customer();
        customer.retag(Set.of(new Tag("vip"), new Tag("inativo")));

        assertThat(customer.changeTags(Set.of(new Tag("atacado")), Set.of(new Tag("inativo"))))
                .isTrue();
        assertThat(customer.getTags()).containsExactly(new Tag("atacado"), new Tag("vip"));
        assertThat(customer.changeTags(Set.of(new Tag("vip")), Set.of(new Tag("outra"))))
                .isFalse();
    }
}
