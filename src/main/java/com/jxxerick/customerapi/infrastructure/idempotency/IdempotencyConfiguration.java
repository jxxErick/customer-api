package com.jxxerick.customerapi.infrastructure.idempotency;

import com.jxxerick.customerapi.application.usecase.CreateCustomerUseCase;
import com.jxxerick.customerapi.application.usecase.GetCustomerUseCase;
import com.jxxerick.customerapi.application.usecase.IdempotentCreateCustomerUseCase;
import com.jxxerick.customerapi.domain.port.IdempotencyStore;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

import java.time.Clock;

@Configuration
@EnableScheduling
@EnableConfigurationProperties(IdempotencyProperties.class)
public class IdempotencyConfiguration {

    @Bean
    IdempotentCreateCustomerUseCase idempotentCreateCustomerUseCase(CreateCustomerUseCase createCustomer,
                                                                    GetCustomerUseCase getCustomer,
                                                                    IdempotencyStore store, Clock clock,
                                                                    IdempotencyProperties properties) {
        return new IdempotentCreateCustomerUseCase(createCustomer, getCustomer, store, clock, properties.ttl());
    }
}
