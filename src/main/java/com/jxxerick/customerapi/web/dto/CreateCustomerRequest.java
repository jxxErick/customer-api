package com.jxxerick.customerapi.web.dto;

import com.jxxerick.customerapi.application.command.CreateCustomerCommand;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateCustomerRequest(
        @NotBlank(message = "Nome é obrigatório") @Size(max = 120, message = "Nome deve ter até 120 caracteres")
        String name,
        @NotBlank(message = "E-mail é obrigatório")
        String email,
        @NotBlank(message = "CEP é obrigatório")
        String zipCode,
        @NotBlank(message = "Número é obrigatório")
        String number,
        String complement
) {

    public CreateCustomerCommand toCommand() {
        return new CreateCustomerCommand(name, email, zipCode, number, complement);
    }
}
