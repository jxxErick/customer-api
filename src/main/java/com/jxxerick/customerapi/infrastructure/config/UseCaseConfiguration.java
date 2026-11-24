package com.jxxerick.customerapi.infrastructure.config;

import com.jxxerick.customerapi.application.usecase.AddressResolver;
import com.jxxerick.customerapi.application.usecase.ChangeCustomerAddressUseCase;
import com.jxxerick.customerapi.application.usecase.CreateCustomerUseCase;
import com.jxxerick.customerapi.application.usecase.DeleteCustomerUseCase;
import com.jxxerick.customerapi.application.usecase.ExportCustomersUseCase;
import com.jxxerick.customerapi.application.usecase.FindCustomerByCpfUseCase;
import com.jxxerick.customerapi.application.usecase.FindDuplicateCustomersUseCase;
import com.jxxerick.customerapi.application.usecase.GetCustomerHistoryUseCase;
import com.jxxerick.customerapi.application.usecase.GetCustomerStatsUseCase;
import com.jxxerick.customerapi.application.usecase.GetCustomerUseCase;
import com.jxxerick.customerapi.application.usecase.ImportCustomersUseCase;
import com.jxxerick.customerapi.application.usecase.ListCustomersUseCase;
import com.jxxerick.customerapi.application.usecase.MergeCustomersUseCase;
import com.jxxerick.customerapi.application.usecase.RenameCustomerUseCase;
import com.jxxerick.customerapi.domain.port.AddressProvider;
import com.jxxerick.customerapi.domain.port.CustomerHistory;
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
    CreateCustomerUseCase createCustomerUseCase(CustomerRepository repository, AddressResolver resolver,
                                                CustomerHistory history, Clock clock) {
        return new CreateCustomerUseCase(repository, resolver, history, clock);
    }

    @Bean
    GetCustomerUseCase getCustomerUseCase(CustomerRepository repository) {
        return new GetCustomerUseCase(repository);
    }

    @Bean
    FindCustomerByCpfUseCase findCustomerByCpfUseCase(CustomerRepository repository) {
        return new FindCustomerByCpfUseCase(repository);
    }

    @Bean
    ImportCustomersUseCase importCustomersUseCase(CreateCustomerUseCase createCustomer) {
        return new ImportCustomersUseCase(createCustomer);
    }

    @Bean
    ExportCustomersUseCase exportCustomersUseCase(CustomerRepository repository) {
        return new ExportCustomersUseCase(repository);
    }

    @Bean
    ListCustomersUseCase listCustomersUseCase(CustomerRepository repository) {
        return new ListCustomersUseCase(repository);
    }

    @Bean
    GetCustomerStatsUseCase getCustomerStatsUseCase(CustomerRepository repository) {
        return new GetCustomerStatsUseCase(repository);
    }

    @Bean
    ChangeCustomerAddressUseCase changeCustomerAddressUseCase(GetCustomerUseCase getCustomer,
                                                              CustomerRepository repository,
                                                              AddressResolver resolver,
                                                              CustomerHistory history, Clock clock) {
        return new ChangeCustomerAddressUseCase(getCustomer, repository, resolver, history, clock);
    }

    @Bean
    RenameCustomerUseCase renameCustomerUseCase(GetCustomerUseCase getCustomer, CustomerRepository repository,
                                                CustomerHistory history, Clock clock) {
        return new RenameCustomerUseCase(getCustomer, repository, history, clock);
    }

    @Bean
    DeleteCustomerUseCase deleteCustomerUseCase(GetCustomerUseCase getCustomer, CustomerRepository repository,
                                                CustomerHistory history, Clock clock) {
        return new DeleteCustomerUseCase(getCustomer, repository, history, clock);
    }

    @Bean
    GetCustomerHistoryUseCase getCustomerHistoryUseCase(CustomerHistory history) {
        return new GetCustomerHistoryUseCase(history);
    }

    @Bean
    FindDuplicateCustomersUseCase findDuplicateCustomersUseCase(CustomerRepository repository) {
        return new FindDuplicateCustomersUseCase(repository);
    }

    @Bean
    MergeCustomersUseCase mergeCustomersUseCase(GetCustomerUseCase getCustomer, CustomerRepository repository,
                                                CustomerHistory history, Clock clock) {
        return new MergeCustomersUseCase(getCustomer, repository, history, clock);
    }
}
