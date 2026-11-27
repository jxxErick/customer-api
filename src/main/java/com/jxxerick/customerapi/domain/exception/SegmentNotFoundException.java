package com.jxxerick.customerapi.domain.exception;

import java.util.UUID;

public class SegmentNotFoundException extends DomainException {

    public SegmentNotFoundException(UUID id) {
        super("Segmento não encontrado: " + id);
    }
}
