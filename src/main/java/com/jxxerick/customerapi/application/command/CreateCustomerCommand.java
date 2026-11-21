package com.jxxerick.customerapi.application.command;

/** {@code cpf} is optional. */
public record CreateCustomerCommand(
        String name,
        String email,
        String zipCode,
        String number,
        String complement,
        String cpf
) {

    public CreateCustomerCommand(String name, String email, String zipCode, String number, String complement) {
        this(name, email, zipCode, number, complement, null);
    }
}
