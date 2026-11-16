package com.jxxerick.customerapi.domain.model;

/** Address data resolved from a zip code, before the customer's number/complement. */
public record PostalAddress(
        ZipCode zipCode,
        String street,
        String neighborhood,
        String city,
        String state
) {

    public PostalAddress {
        Validations.requireNonNull(zipCode, "CEP é obrigatório");
        city = Validations.requireText(city, "Cidade é obrigatória");
        state = Validations.requireText(state, "UF é obrigatória");
    }
}
