package com.jxxerick.customerapi.application.command;

import java.util.UUID;

/** {@code expectedVersion} comes from If-Match; null skips the concurrency check. */
public record ChangeAddressCommand(UUID customerId, String zipCode, String number, String complement,
                                   Long expectedVersion) {
}
