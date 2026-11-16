package com.jxxerick.customerapi.domain.model;

import com.jxxerick.customerapi.domain.exception.DomainValidationException;

final class Validations {

    private Validations() {
    }

    static String requireText(String value, String message) {
        if (value == null || value.isBlank()) {
            throw new DomainValidationException(message);
        }
        return value.strip();
    }

    static <T> T requireNonNull(T value, String message) {
        if (value == null) {
            throw new DomainValidationException(message);
        }
        return value;
    }
}
