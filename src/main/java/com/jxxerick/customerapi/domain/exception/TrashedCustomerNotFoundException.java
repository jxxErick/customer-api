package com.jxxerick.customerapi.domain.exception;

import java.util.UUID;

public class TrashedCustomerNotFoundException extends DomainException {

    public TrashedCustomerNotFoundException(UUID id) {
        super("Cliente não está na lixeira: " + id);
    }
}
