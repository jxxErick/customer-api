package com.jxxerick.customerapi.infrastructure.address.viacep;

import com.jxxerick.customerapi.domain.port.AddressProvider;
import com.jxxerick.customerapi.infrastructure.address.CachingAddressProvider;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

import java.time.Clock;

@Configuration
@EnableConfigurationProperties(ViaCepProperties.class)
public class ViaCepConfiguration {

    @Bean
    AddressProvider addressProvider(RestClient.Builder builder, ViaCepProperties properties, Clock clock) {
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(properties.connectTimeout());
        requestFactory.setReadTimeout(properties.readTimeout());

        RestClient restClient = builder
                .baseUrl(properties.baseUrl())
                .requestFactory(requestFactory)
                .build();
        return new CachingAddressProvider(new ViaCepAddressProvider(restClient), properties.cacheTtl(), clock);
    }
}
