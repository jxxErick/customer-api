package com.jxxerick.customerapi.domain.model;

import com.jxxerick.customerapi.domain.exception.DomainValidationException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class EmailTest {

    @Test
    void shouldNormalizeToLowerCaseAndTrim() {
        assertThat(new Email("  John.Doe@Example.COM ").value()).isEqualTo("john.doe@example.com");
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"   ", "plainaddress", "a@b", "@example.com", "john@.com"})
    void shouldRejectInvalidEmails(String value) {
        assertThatThrownBy(() -> new Email(value)).isInstanceOf(DomainValidationException.class);
    }
}
