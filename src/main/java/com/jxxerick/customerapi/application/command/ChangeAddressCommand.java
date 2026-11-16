package com.jxxerick.customerapi.application.command;

import java.util.UUID;

public record ChangeAddressCommand(UUID customerId, String zipCode, String number, String complement) {
}
