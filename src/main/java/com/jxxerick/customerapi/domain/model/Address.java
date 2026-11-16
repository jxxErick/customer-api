package com.jxxerick.customerapi.domain.model;

public record Address(PostalAddress postal, String number, String complement) {

    public Address {
        Validations.requireNonNull(postal, "Endereço é obrigatório");
        number = Validations.requireText(number, "Número do endereço é obrigatório");
        complement = complement == null || complement.isBlank() ? null : complement.strip();
    }

    public ZipCode zipCode() {
        return postal.zipCode();
    }
}
