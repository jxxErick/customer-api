package com.jxxerick.customerapi.application.usecase;

import com.jxxerick.customerapi.domain.exception.CustomerNotFoundException;
import com.jxxerick.customerapi.domain.model.Customer;
import com.jxxerick.customerapi.domain.port.CustomerRepository;
import com.jxxerick.customerapi.support.Fixtures;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CustomerQueriesUseCaseTest {

    @Mock
    private CustomerRepository customerRepository;

    @Test
    void getShouldReturnExistingCustomer() {
        Customer customer = Fixtures.customer();
        when(customerRepository.findById(customer.getId())).thenReturn(Optional.of(customer));

        assertThat(new GetCustomerUseCase(customerRepository).execute(customer.getId())).isSameAs(customer);
    }

    @Test
    void getShouldFailWhenCustomerDoesNotExist() {
        UUID id = UUID.randomUUID();
        when(customerRepository.findById(id)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> new GetCustomerUseCase(customerRepository).execute(id))
                .isInstanceOf(CustomerNotFoundException.class);
    }

    @Test
    void listShouldReturnAllCustomers() {
        List<Customer> customers = List.of(Fixtures.customer());
        when(customerRepository.findAll()).thenReturn(customers);

        assertThat(new ListCustomersUseCase(customerRepository).execute()).isEqualTo(customers);
    }

    @Test
    void deleteShouldRemoveExistingCustomer() {
        Customer customer = Fixtures.customer();
        when(customerRepository.findById(customer.getId())).thenReturn(Optional.of(customer));

        new DeleteCustomerUseCase(new GetCustomerUseCase(customerRepository), customerRepository)
                .execute(customer.getId());

        verify(customerRepository).delete(customer);
    }

    @Test
    void deleteShouldFailWhenCustomerDoesNotExist() {
        UUID id = UUID.randomUUID();
        when(customerRepository.findById(id)).thenReturn(Optional.empty());
        DeleteCustomerUseCase useCase =
                new DeleteCustomerUseCase(new GetCustomerUseCase(customerRepository), customerRepository);

        assertThatThrownBy(() -> useCase.execute(id)).isInstanceOf(CustomerNotFoundException.class);
        verify(customerRepository, never()).delete(any());
    }
}
