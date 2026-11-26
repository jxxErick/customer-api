package com.jxxerick.customerapi.web.dto;

import com.jxxerick.customerapi.application.command.TagCustomerCommand;
import jakarta.validation.constraints.NotNull;

import java.util.List;
import java.util.UUID;

/** The full new set of tags; send an empty list to clear them. */
public record TagCustomerRequest(
        @NotNull(message = "Tags são obrigatórias")
        List<String> tags
) {

    public TagCustomerCommand toCommand(UUID customerId, Long expectedVersion) {
        return new TagCustomerCommand(customerId, tags, expectedVersion);
    }
}
