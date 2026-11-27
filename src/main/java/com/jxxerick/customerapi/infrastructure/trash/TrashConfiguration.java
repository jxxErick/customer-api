package com.jxxerick.customerapi.infrastructure.trash;

import com.jxxerick.customerapi.application.usecase.DeleteCustomerUseCase;
import com.jxxerick.customerapi.application.usecase.GetCustomerUseCase;
import com.jxxerick.customerapi.application.usecase.ListTrashUseCase;
import com.jxxerick.customerapi.application.usecase.PurgeTrashedCustomerUseCase;
import com.jxxerick.customerapi.application.usecase.RestoreCustomerUseCase;
import com.jxxerick.customerapi.domain.port.CustomerHistory;
import com.jxxerick.customerapi.domain.port.CustomerRepository;
import com.jxxerick.customerapi.domain.port.CustomerTrash;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;

@Configuration
@EnableConfigurationProperties(TrashProperties.class)
public class TrashConfiguration {

    @Bean
    DeleteCustomerUseCase deleteCustomerUseCase(GetCustomerUseCase getCustomer, CustomerRepository repository,
                                                CustomerTrash trash, CustomerHistory history, Clock clock,
                                                TrashProperties properties) {
        return new DeleteCustomerUseCase(getCustomer, repository, trash, history, clock, properties.retention());
    }

    @Bean
    RestoreCustomerUseCase restoreCustomerUseCase(CustomerTrash trash, CustomerRepository repository,
                                                  CustomerHistory history, Clock clock) {
        return new RestoreCustomerUseCase(trash, repository, history, clock);
    }

    @Bean
    ListTrashUseCase listTrashUseCase(CustomerTrash trash) {
        return new ListTrashUseCase(trash);
    }

    @Bean
    PurgeTrashedCustomerUseCase purgeTrashedCustomerUseCase(CustomerTrash trash) {
        return new PurgeTrashedCustomerUseCase(trash);
    }
}
