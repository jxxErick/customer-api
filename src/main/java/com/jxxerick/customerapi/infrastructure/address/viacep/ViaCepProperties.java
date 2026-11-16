package com.jxxerick.customerapi.infrastructure.address.viacep;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

@ConfigurationProperties(prefix = "integration.viacep")
public record ViaCepProperties(String baseUrl, Duration connectTimeout, Duration readTimeout) {
}
