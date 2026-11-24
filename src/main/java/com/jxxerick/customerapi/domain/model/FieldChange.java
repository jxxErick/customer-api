package com.jxxerick.customerapi.domain.model;

/** One field before and after a change; {@code previous} is null on creation. */
public record FieldChange(String field, String previous, String current) {

    public FieldChange {
        Validations.requireText(field, "Campo é obrigatório");
    }
}
