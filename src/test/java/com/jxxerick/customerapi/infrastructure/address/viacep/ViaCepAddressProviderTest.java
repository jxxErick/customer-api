package com.jxxerick.customerapi.infrastructure.address.viacep;

import com.jxxerick.customerapi.domain.exception.AddressProviderUnavailableException;
import com.jxxerick.customerapi.domain.model.PostalAddress;
import com.jxxerick.customerapi.domain.model.ZipCode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class ViaCepAddressProviderTest {

    private static final String BASE_URL = "https://viacep.test/ws";

    private MockRestServiceServer server;
    private ViaCepAddressProvider provider;

    @BeforeEach
    void setUp() {
        RestClient.Builder builder = RestClient.builder().baseUrl(BASE_URL);
        server = MockRestServiceServer.bindTo(builder).build();
        provider = new ViaCepAddressProvider(builder.build());
    }

    @Test
    void shouldMapSuccessfulResponse() {
        server.expect(requestTo(BASE_URL + "/01001000/json"))
                .andExpect(method(org.springframework.http.HttpMethod.GET))
                .andRespond(withSuccess("""
                        {
                          "cep": "01001-000",
                          "logradouro": "Praça da Sé",
                          "complemento": "lado ímpar",
                          "bairro": "Sé",
                          "localidade": "São Paulo",
                          "uf": "SP"
                        }
                        """, MediaType.APPLICATION_JSON));

        Optional<PostalAddress> result = provider.findByZipCode(new ZipCode("01001000"));

        assertThat(result).contains(
                new PostalAddress(new ZipCode("01001000"), "Praça da Sé", "Sé", "São Paulo", "SP"));
        server.verify();
    }

    @Test
    void shouldReturnEmptyWhenApiFlagsError() {
        server.expect(requestTo(BASE_URL + "/99999999/json"))
                .andRespond(withSuccess("{\"erro\": \"true\"}", MediaType.APPLICATION_JSON));

        assertThat(provider.findByZipCode(new ZipCode("99999999"))).isEmpty();
    }

    @Test
    void shouldReturnEmptyOnBadRequest() {
        server.expect(requestTo(BASE_URL + "/00000000/json")).andRespond(withStatus(HttpStatus.BAD_REQUEST));

        assertThat(provider.findByZipCode(new ZipCode("00000000"))).isEmpty();
    }

    @Test
    void shouldTranslateServerErrorToUnavailable() {
        server.expect(requestTo(BASE_URL + "/01001000/json")).andRespond(withStatus(HttpStatus.SERVICE_UNAVAILABLE));

        assertThatThrownBy(() -> provider.findByZipCode(new ZipCode("01001000")))
                .isInstanceOf(AddressProviderUnavailableException.class);
    }
}
