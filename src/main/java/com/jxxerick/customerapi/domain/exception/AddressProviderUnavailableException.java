package com.jxxerick.customerapi.domain.exception;

public class AddressProviderUnavailableException extends DomainException {

    public AddressProviderUnavailableException(Throwable cause) {
        super("Serviço de consulta de CEP indisponível. Tente novamente mais tarde.");
        initCause(cause);
    }
}
