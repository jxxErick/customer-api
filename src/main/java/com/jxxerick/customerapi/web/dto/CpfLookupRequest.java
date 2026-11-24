package com.jxxerick.customerapi.web.dto;

import jakarta.validation.constraints.NotBlank;

/** Sent in the body so the document never shows up in URLs or access logs. */
public record CpfLookupRequest(
        @NotBlank(message = "CPF é obrigatório")
        String cpf
) {
}
