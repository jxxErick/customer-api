package com.jxxerick.customerapi.domain.exception;

import java.util.UUID;

/** The client changed an outdated copy of the customer. */
public class ConcurrentUpdateException extends DomainException {

    public ConcurrentUpdateException(UUID id) {
        super("O cliente " + id + " foi alterado por outra requisição. Busque a versão atual e tente novamente.");
    }
}
