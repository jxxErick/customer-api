package com.jxxerick.customerapi.web.dto;

import com.jxxerick.customerapi.domain.model.Address;
import com.jxxerick.customerapi.domain.model.Cpf;
import com.jxxerick.customerapi.domain.model.Customer;

import java.time.Instant;
import java.util.UUID;

/** {@code cpf} is masked (***.982.247-**) and null when the customer has none. */
public record CustomerResponse(UUID id, String name, String email, String cpf, AddressResponse address,
                               Instant createdAt) {

    public static CustomerResponse from(Customer customer) {
        return new CustomerResponse(
                customer.getId(),
                customer.getName(),
                customer.getEmail().value(),
                customer.getCpf().map(Cpf::masked).orElse(null),
                AddressResponse.from(customer.getAddress()),
                customer.getCreatedAt());
    }

    public record AddressResponse(String zipCode, String street, String number, String complement,
                                  String neighborhood, String city, String state) {

        static AddressResponse from(Address address) {
            var postal = address.postal();
            return new AddressResponse(
                    postal.zipCode().formatted(),
                    postal.street(),
                    address.number(),
                    address.complement(),
                    postal.neighborhood(),
                    postal.city(),
                    postal.state());
        }
    }
}
