package com.jxxerick.customerapi.infrastructure.persistence;

import com.jxxerick.customerapi.domain.model.Address;
import com.jxxerick.customerapi.domain.model.Cpf;
import com.jxxerick.customerapi.domain.model.Customer;
import com.jxxerick.customerapi.domain.model.Email;
import com.jxxerick.customerapi.domain.model.PostalAddress;
import com.jxxerick.customerapi.domain.model.ZipCode;

final class CustomerEntityMapper {

    private CustomerEntityMapper() {
    }

    static CustomerEntity toEntity(Customer customer) {
        Address address = customer.getAddress();
        PostalAddress postal = address.postal();
        return new CustomerEntity(
                customer.getId(),
                customer.getName(),
                customer.getEmail().value(),
                customer.getCpf().map(Cpf::value).orElse(null),
                postal.zipCode().value(),
                postal.street(),
                address.number(),
                address.complement(),
                postal.neighborhood(),
                postal.city(),
                postal.state(),
                customer.getCreatedAt(),
                customer.getVersion());
    }

    static Customer toDomain(CustomerEntity entity) {
        PostalAddress postal = new PostalAddress(
                new ZipCode(entity.getZipCode()),
                entity.getStreet(),
                entity.getNeighborhood(),
                entity.getCity(),
                entity.getState());
        Address address = new Address(postal, entity.getNumber(), entity.getComplement());
        return Customer.restore(
                entity.getId(), entity.getName(), new Email(entity.getEmail()),
                entity.getCpf() == null ? null : new Cpf(entity.getCpf()), address, entity.getCreatedAt(),
                entity.getVersion());
    }
}
