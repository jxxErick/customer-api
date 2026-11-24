package com.jxxerick.customerapi.domain.exception;

import com.jxxerick.customerapi.domain.model.Cpf;

public class CpfAlreadyInUseException extends DomainException {

    public CpfAlreadyInUseException(Cpf cpf) {
        super("CPF já cadastrado: " + cpf.masked());
    }
}
