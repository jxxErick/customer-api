package com.jxxerick.customerapi.infrastructure.address.viacep;

import com.jxxerick.customerapi.domain.exception.AddressProviderUnavailableException;
import com.jxxerick.customerapi.domain.model.PostalAddress;
import com.jxxerick.customerapi.domain.model.ZipCode;
import com.jxxerick.customerapi.domain.port.AddressProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;

import java.util.Optional;

/** Adapter from the ViaCEP HTTP API to the {@link AddressProvider} port. */
public class ViaCepAddressProvider implements AddressProvider {

    private static final Logger log = LoggerFactory.getLogger(ViaCepAddressProvider.class);

    private final RestClient restClient;

    public ViaCepAddressProvider(RestClient restClient) {
        this.restClient = restClient;
    }

    @Override
    public Optional<PostalAddress> findByZipCode(ZipCode zipCode) {
        try {
            ViaCepResponse response = restClient.get()
                    .uri("/{zipCode}/json", zipCode.value())
                    .retrieve()
                    .body(ViaCepResponse.class);
            return Optional.ofNullable(response)
                    .filter(body -> !body.isNotFound())
                    .map(ViaCepResponse::toPostalAddress);
        } catch (RestClientResponseException e) {
            if (e.getStatusCode().isSameCodeAs(HttpStatus.BAD_REQUEST)) {
                return Optional.empty();
            }
            throw unavailable(zipCode, e);
        } catch (RestClientException e) {
            throw unavailable(zipCode, e);
        }
    }

    private AddressProviderUnavailableException unavailable(ZipCode zipCode, Exception cause) {
        log.warn("ViaCEP lookup failed for zip code {}: {}", zipCode, cause.getMessage());
        return new AddressProviderUnavailableException(cause);
    }
}
