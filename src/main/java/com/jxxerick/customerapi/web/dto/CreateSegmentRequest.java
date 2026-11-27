package com.jxxerick.customerapi.web.dto;

import com.jxxerick.customerapi.application.command.CreateSegmentCommand;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.util.List;

/** At least one criterion is required; the ones present are combined with AND. */
public record CreateSegmentRequest(
        @NotBlank(message = "Nome é obrigatório")
        @Size(max = 60, message = "Nome pode ter no máximo 60 caracteres")
        String name,
        String nameContains,
        List<String> tags,
        String state,
        String city
) {

    public CreateSegmentCommand toCommand() {
        return new CreateSegmentCommand(name, nameContains, tags, state, city);
    }
}
