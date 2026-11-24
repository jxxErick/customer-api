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

    /** One line, e.g. "Praça da Sé, 100 - Apto 12, Sé, São Paulo/SP, 01001-000". */
    public String summary() {
        StringBuilder line = new StringBuilder();
        if (postal.street() != null && !postal.street().isBlank()) {
            line.append(postal.street()).append(", ");
        }
        line.append(number);
        if (complement != null) {
            line.append(" - ").append(complement);
        }
        if (postal.neighborhood() != null && !postal.neighborhood().isBlank()) {
            line.append(", ").append(postal.neighborhood());
        }
        return line.append(", ").append(postal.city()).append('/').append(postal.state())
                .append(", ").append(postal.zipCode().formatted()).toString();
    }
}
