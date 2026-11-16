package com.jxxerick.customerapi.infrastructure.address.viacep;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.jxxerick.customerapi.domain.model.PostalAddress;
import com.jxxerick.customerapi.domain.model.ZipCode;

/** ViaCEP payload; JSON names follow the external API contract. */
@JsonIgnoreProperties(ignoreUnknown = true)
record ViaCepResponse(
        @JsonProperty("cep") String zipCode,
        @JsonProperty("logradouro") String street,
        @JsonProperty("bairro") String neighborhood,
        @JsonProperty("localidade") String city,
        @JsonProperty("uf") String state,
        @JsonProperty("erro") Boolean error
) {

    boolean isNotFound() {
        return Boolean.TRUE.equals(error) || zipCode == null;
    }

    PostalAddress toPostalAddress() {
        return new PostalAddress(new ZipCode(zipCode), street, neighborhood, city, state);
    }
}
