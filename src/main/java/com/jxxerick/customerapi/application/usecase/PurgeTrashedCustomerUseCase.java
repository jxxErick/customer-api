package com.jxxerick.customerapi.application.usecase;

import com.jxxerick.customerapi.domain.exception.TrashedCustomerNotFoundException;
import com.jxxerick.customerapi.domain.port.CustomerTrash;

import java.util.UUID;

/** Empties one slot of the trash right away; the history is kept. */
public class PurgeTrashedCustomerUseCase {

    private final CustomerTrash trash;

    public PurgeTrashedCustomerUseCase(CustomerTrash trash) {
        this.trash = trash;
    }

    public void execute(UUID id) {
        if (trash.find(id).isEmpty()) {
            throw new TrashedCustomerNotFoundException(id);
        }
        trash.remove(id);
    }
}
