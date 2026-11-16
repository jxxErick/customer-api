package com.jxxerick.customerapi.domain.exception;

public class EmailAlreadyInUseException extends DomainException {

    public EmailAlreadyInUseException(String email) {
        super("E-mail já cadastrado: " + email);
    }
}
