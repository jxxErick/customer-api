package com.jxxerick.customerapi.web.dto;

import com.jxxerick.customerapi.application.command.MergeCustomersCommand;
import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record MergeCustomersRequest(
        @NotNull(message = "Cliente duplicado é obrigatório")
        UUID duplicateId
) {

    public MergeCustomersCommand toCommand(UUID targetId, Long expectedVersion) {
        return new MergeCustomersCommand(targetId, duplicateId, expectedVersion);
    }
}
