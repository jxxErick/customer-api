package com.jxxerick.customerapi.web.dto;

import com.jxxerick.customerapi.application.command.ChangeAddressCommand;
import jakarta.validation.constraints.NotBlank;

import java.util.UUID;

public record ChangeAddressRequest(
        @NotBlank(message = "CEP é obrigatório")
        String zipCode,
        @NotBlank(message = "Número é obrigatório")
        String number,
        String complement
) {

    public ChangeAddressCommand toCommand(UUID customerId) {
        return new ChangeAddressCommand(customerId, zipCode, number, complement);
    }
}
