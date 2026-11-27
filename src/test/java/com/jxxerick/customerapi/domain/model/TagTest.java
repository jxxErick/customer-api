package com.jxxerick.customerapi.domain.model;

import com.jxxerick.customerapi.domain.exception.DomainValidationException;
import com.jxxerick.customerapi.support.Fixtures;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.Collections;
import java.util.List;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class TagTest {

    @ParameterizedTest
    @CsvSource({
            "vip, vip",
            "'  VIP ', vip",
            "Zona Sul, zona-sul",
            "zona_sul, zona-sul",
            "Área VIP, area-vip",
            "cliente-2024, cliente-2024"
    })
    void shouldNormalize(String raw, String expected) {
        assertThat(new Tag(raw).value()).isEqualTo(expected);
    }

    @ParameterizedTest
    @ValueSource(strings = {"", "   ", "vip!", "-vip", "vip-", "a--b", "zona,sul"})
    void shouldRejectInvalid(String raw) {
        assertThatThrownBy(() -> new Tag(raw)).isInstanceOf(DomainValidationException.class);
    }

    @Test
    void shouldRejectTooLong() {
        String tooLong = String.join("", Collections.nCopies(Tag.MAX_LENGTH + 1, "a"));

        assertThatThrownBy(() -> new Tag(tooLong)).isInstanceOf(DomainValidationException.class);
    }

    @Test
    void customerShouldKeepTagsSortedAndCollapseDuplicates() {
        Customer customer = Fixtures.customer();

        customer.retag(List.of(new Tag("zona-sul"), new Tag("VIP"), new Tag("vip")));

        assertThat(customer.getTags()).extracting(Tag::value).containsExactly("vip", "zona-sul");
    }

    @Test
    void customerShouldRejectMoreThanTheMaximum() {
        Customer customer = Fixtures.customer();
        List<Tag> tooMany = IntStream.rangeClosed(1, Customer.MAX_TAGS + 1).mapToObj(i -> new Tag("t" + i)).toList();

        assertThatThrownBy(() -> customer.retag(tooMany)).isInstanceOf(DomainValidationException.class);
        assertThat(customer.getTags()).isEmpty();
    }

    @Test
    void retaggedChangeShouldListBeforeAndAfter() {
        Customer customer = Fixtures.customer();
        customer.retag(List.of(new Tag("vip"), new Tag("atacado")));

        CustomerChange change = CustomerChange.retagged(customer, List.of(), Fixtures.NOW);

        assertThat(change.type()).isEqualTo(ChangeType.TAGS_CHANGED);
        assertThat(change.fields()).containsExactly(new FieldChange("tags", null, "atacado, vip"));
    }
}
