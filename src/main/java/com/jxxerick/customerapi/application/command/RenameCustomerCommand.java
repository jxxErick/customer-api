package com.jxxerick.customerapi.application.command;

import java.util.UUID;

/** {@code expectedVersion} comes from If-Match; null skips the concurrency check. */
public record RenameCustomerCommand(UUID customerId, String name, Long expectedVersion) {
}
