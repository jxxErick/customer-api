package com.jxxerick.customerapi.domain.model;

import com.jxxerick.customerapi.domain.exception.DomainValidationException;

/** Brazilian CEP, stored as 8 digits. */
public record ZipCode(String value) {

    private static final int LENGTH = 8;

    public ZipCode {
        String digits = Validations.requireText(value, "CEP é obrigatório").replaceAll("\\D", "");
        if (digits.length() != LENGTH) {
            throw new DomainValidationException("CEP inválido: " + value);
        }
        value = digits;
    }

    public String formatted() {
        return value.substring(0, 5) + "-" + value.substring(5);
    }

    @Override
    public String toString() {
        return value;
    }
}
