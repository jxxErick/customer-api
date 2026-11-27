package com.jxxerick.customerapi.application.command;

import java.util.List;
import java.util.UUID;

/** Raw tags as typed; {@code expectedVersion} comes from If-Match and null skips the check. */
public record TagCustomerCommand(UUID customerId, List<String> tags, Long expectedVersion) {

    public TagCustomerCommand {
        tags = tags == null ? List.of() : List.copyOf(tags);
    }
}
