package com.jxxerick.customerapi.application.command;

public record CreateCustomerCommand(
        String name,
        String email,
        String zipCode,
        String number,
        String complement
) {
}
