package com.jxxerick.customerapi.domain.exception;

public class AddressNotFoundException extends DomainException {

    public AddressNotFoundException(String zipCode) {
        super("Endereço não encontrado para o CEP: " + zipCode);
    }
}
