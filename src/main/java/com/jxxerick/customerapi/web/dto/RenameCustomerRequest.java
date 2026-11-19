package com.jxxerick.customerapi.web.dto;

import com.jxxerick.customerapi.application.command.RenameCustomerCommand;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.util.UUID;

public record RenameCustomerRequest(
        @NotBlank(message = "Nome é obrigatório")
        @Size(max = 120, message = "Nome deve ter no máximo 120 caracteres")
        String name
) {

    public RenameCustomerCommand toCommand(UUID customerId) {
        return new RenameCustomerCommand(customerId, name);
    }
}
