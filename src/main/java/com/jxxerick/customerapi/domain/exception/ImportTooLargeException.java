package com.jxxerick.customerapi.domain.exception;

public class ImportTooLargeException extends DomainException {

    public ImportTooLargeException(int rows, int max) {
        super("O arquivo tem %d clientes; o máximo por importação é %d".formatted(rows, max));
    }
}
