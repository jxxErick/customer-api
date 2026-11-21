package com.jxxerick.customerapi.domain.exception;

import com.jxxerick.customerapi.domain.model.Cpf;

import java.util.UUID;

public class CustomerNotFoundException extends DomainException {

    public CustomerNotFoundException(UUID id) {
        super("Cliente não encontrado: " + id);
    }

    private CustomerNotFoundException(String message) {
        super(message);
    }

    public static CustomerNotFoundException withCpf(Cpf cpf) {
        return new CustomerNotFoundException("Nenhum cliente com o CPF " + cpf.masked());
    }
}
