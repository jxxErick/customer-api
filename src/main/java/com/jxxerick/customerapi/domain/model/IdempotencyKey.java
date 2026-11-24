package com.jxxerick.customerapi.domain.model;

import com.jxxerick.customerapi.domain.exception.DomainValidationException;

import java.util.regex.Pattern;

/** Client-chosen token (usually a UUID) that makes a retried POST safe. */
public record IdempotencyKey(String value) {

    private static final Pattern FORMAT = Pattern.compile("^[A-Za-z0-9._:-]{8,64}$");

    public IdempotencyKey {
        value = Validations.requireText(value, "Idempotency-Key não pode ser vazio");
        if (!FORMAT.matcher(value).matches()) {
            throw new DomainValidationException(
                    "Idempotency-Key deve ter de 8 a 64 caracteres (letras, números, . _ : -)");
        }
    }
}
