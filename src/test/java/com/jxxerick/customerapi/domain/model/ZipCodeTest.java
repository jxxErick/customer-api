package com.jxxerick.customerapi.domain.model;

import com.jxxerick.customerapi.domain.exception.DomainValidationException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ZipCodeTest {

    @ParameterizedTest
    @ValueSource(strings = {"01001000", "01001-000", " 01001.000 "})
    void shouldKeepOnlyDigits(String raw) {
        assertThat(new ZipCode(raw).value()).isEqualTo("01001000");
    }

    @Test
    void shouldFormatWithHyphen() {
        assertThat(new ZipCode("01001000").formatted()).isEqualTo("01001-000");
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"1234567", "123456789", "abcdefgh"})
    void shouldRejectInvalidZipCodes(String raw) {
        assertThatThrownBy(() -> new ZipCode(raw)).isInstanceOf(DomainValidationException.class);
    }
}
