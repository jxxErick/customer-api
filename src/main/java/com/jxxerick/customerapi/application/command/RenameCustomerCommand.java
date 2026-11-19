package com.jxxerick.customerapi.application.command;

import java.util.UUID;

public record RenameCustomerCommand(UUID customerId, String name) {
}
