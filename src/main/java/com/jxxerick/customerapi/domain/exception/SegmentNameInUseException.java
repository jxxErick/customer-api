package com.jxxerick.customerapi.domain.exception;

public class SegmentNameInUseException extends DomainException {

    public SegmentNameInUseException(String name) {
        super("Já existe um segmento chamado " + name);
    }
}
