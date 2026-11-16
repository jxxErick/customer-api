package com.jxxerick.customerapi.application.usecase;

import com.jxxerick.customerapi.domain.exception.AddressNotFoundException;
import com.jxxerick.customerapi.domain.model.Address;
import com.jxxerick.customerapi.domain.model.ZipCode;
import com.jxxerick.customerapi.domain.port.AddressProvider;

/** Builds a full address from a zip code plus customer-supplied details. */
public class AddressResolver {

    private final AddressProvider addressProvider;

    public AddressResolver(AddressProvider addressProvider) {
        this.addressProvider = addressProvider;
    }

    public Address resolve(String rawZipCode, String number, String complement) {
        ZipCode zipCode = new ZipCode(rawZipCode);
        return addressProvider.findByZipCode(zipCode)
                .map(postal -> new Address(postal, number, complement))
                .orElseThrow(() -> new AddressNotFoundException(zipCode.formatted()));
    }
}
