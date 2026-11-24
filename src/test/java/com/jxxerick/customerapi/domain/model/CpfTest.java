package com.jxxerick.customerapi.domain.model;

import com.jxxerick.customerapi.domain.exception.DomainValidationException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class CpfTest {

    @ParameterizedTest
    @ValueSource(strings = {"529.982.247-25", "52998224725", " 529 982 247 25 ", "111.444.777-35"})
    void shouldAcceptValidCpfWithOrWithoutPunctuation(String raw) {
        assertThat(new Cpf(raw).value()).hasSize(11).containsOnlyDigits();
    }

    @ParameterizedTest
    @ValueSource(strings = {"529.982.247-24", "529.982.247-15", "111.111.111-11", "000.000.000-00", "5299822472",
            "529982247251"})
    void shouldRejectWrongCheckDigitsRepeatedDigitsAndWrongLength(String raw) {
        assertThatThrownBy(() -> new Cpf(raw))
                .isInstanceOf(DomainValidationException.class)
                .hasMessage("CPF inválido");
    }

    @Test
    void shouldRejectBlank() {
        assertThatThrownBy(() -> new Cpf(" ")).isInstanceOf(DomainValidationException.class);
    }

    @Test
    void shouldFormatAndMask() {
        Cpf cpf = new Cpf("52998224725");

        assertThat(cpf.formatted()).isEqualTo("529.982.247-25");
        assertThat(cpf.masked()).isEqualTo("***.982.247-**");
    }

    @Test
    void toStringShouldNeverLeakTheFullDocument() {
        assertThat(new Cpf("52998224725")).hasToString("***.982.247-**");
    }

    @Test
    void shouldCompareByDigits() {
        assertThat(new Cpf("529.982.247-25")).isEqualTo(new Cpf("52998224725"));
    }
}
