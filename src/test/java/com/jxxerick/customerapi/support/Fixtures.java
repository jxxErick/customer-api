package com.jxxerick.customerapi.support;

import com.jxxerick.customerapi.domain.model.Address;
import com.jxxerick.customerapi.domain.model.Customer;
import com.jxxerick.customerapi.domain.model.Email;
import com.jxxerick.customerapi.domain.model.PostalAddress;
import com.jxxerick.customerapi.domain.model.ZipCode;

import java.time.Instant;

public final class Fixtures {

    public static final Instant NOW = Instant.parse("2026-01-10T12:00:00Z");

    private Fixtures() {
    }

    public static PostalAddress postalAddress() {
        return new PostalAddress(new ZipCode("01001-000"), "Praça da Sé", "Sé", "São Paulo", "SP");
    }

    public static Address address() {
        return new Address(postalAddress(), "100", "Apto 12");
    }

    public static Customer customer() {
        return Customer.create("Maria Silva", new Email("maria@example.com"), address(), NOW);
    }
}
