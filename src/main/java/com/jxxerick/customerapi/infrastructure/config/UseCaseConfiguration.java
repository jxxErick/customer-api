package com.jxxerick.customerapi.infrastructure.config;

import com.jxxerick.customerapi.application.usecase.AddressResolver;
import com.jxxerick.customerapi.application.usecase.ChangeCustomerAddressUseCase;
import com.jxxerick.customerapi.application.usecase.CreateCustomerUseCase;
import com.jxxerick.customerapi.application.usecase.DeleteCustomerUseCase;
import com.jxxerick.customerapi.application.usecase.GetCustomerUseCase;
import com.jxxerick.customerapi.application.usecase.ListCustomersUseCase;
import com.jxxerick.customerapi.domain.port.AddressProvider;
import com.jxxerick.customerapi.domain.port.CustomerRepository;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;

/** Wires framework-free use cases into the Spring context. */
@Configuration
public class UseCaseConfiguration {

    @Bean
    Clock clock() {
        return Clock.systemUTC();
    }

    @Bean
    AddressResolver addressResolver(AddressProvider addressProvider) {
        return new AddressResolver(addressProvider);
    }

    @Bean
    CreateCustomerUseCase createCustomerUseCase(CustomerRepository repository, AddressResolver resolver, Clock clock) {
        return new CreateCustomerUseCase(repository, resolver, clock);
    }

    @Bean
    GetCustomerUseCase getCustomerUseCase(CustomerRepository repository) {
        return new GetCustomerUseCase(repository);
    }

    @Bean
    ListCustomersUseCase listCustomersUseCase(CustomerRepository repository) {
        return new ListCustomersUseCase(repository);
    }

    @Bean
    ChangeCustomerAddressUseCase changeCustomerAddressUseCase(GetCustomerUseCase getCustomer,
                                                              CustomerRepository repository,
                                                              AddressResolver resolver) {
        return new ChangeCustomerAddressUseCase(getCustomer, repository, resolver);
    }

    @Bean
    DeleteCustomerUseCase deleteCustomerUseCase(GetCustomerUseCase getCustomer, CustomerRepository repository) {
        return new DeleteCustomerUseCase(getCustomer, repository);
    }
}
