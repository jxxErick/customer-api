package com.jxxerick.customerapi.domain.port;

import com.jxxerick.customerapi.domain.model.PostalAddress;
import com.jxxerick.customerapi.domain.model.ZipCode;

import java.util.Optional;

/** Resolves a zip code into a postal address (e.g. external CEP services). */
public interface AddressProvider {

    Optional<PostalAddress> findByZipCode(ZipCode zipCode);
}
