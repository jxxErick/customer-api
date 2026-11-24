package com.jxxerick.customerapi.application.command;

/** One customer read from an import file; {@code line} is where it starts in the file. */
public record ImportRow(int line, CreateCustomerCommand command) {
}
