package com.jxxerick.customerapi.web.csv;

import com.jxxerick.customerapi.domain.model.Address;
import com.jxxerick.customerapi.domain.model.Customer;

import java.util.Arrays;
import java.util.List;

/**
 * Writes customers with the same headers the import reads, so an export can be imported back.
 * The CPF is left out on purpose: the API never hands it out unmasked.
 */
public final class CustomerCsvWriter {

    static final List<String> HEADER = List.of("name", "email", "zipCode", "number", "complement",
            "street", "neighborhood", "city", "state", "createdAt");

    private final CsvWriter csv;

    public CustomerCsvWriter(CsvWriter csv) {
        this.csv = csv;
    }

    public void writeHeader() {
        csv.writeRow(HEADER);
    }

    public void write(Customer customer) {
        Address address = customer.getAddress();
        var postal = address.postal();
        csv.writeRow(Arrays.asList(
                customer.getName(),
                customer.getEmail().value(),
                postal.zipCode().formatted(),
                address.number(),
                address.complement(),
                postal.street(),
                postal.neighborhood(),
                postal.city(),
                postal.state(),
                customer.getCreatedAt().toString()));
    }
}
