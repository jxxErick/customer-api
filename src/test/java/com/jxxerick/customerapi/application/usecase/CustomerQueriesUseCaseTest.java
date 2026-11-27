package com.jxxerick.customerapi.application.usecase;

import com.jxxerick.customerapi.application.command.ListCustomersQuery;
import com.jxxerick.customerapi.domain.exception.CustomerNotFoundException;
import com.jxxerick.customerapi.domain.model.Cpf;
import com.jxxerick.customerapi.domain.model.Customer;
import com.jxxerick.customerapi.domain.model.PageResult;
import com.jxxerick.customerapi.domain.port.CustomerRepository;
import com.jxxerick.customerapi.support.Fixtures;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
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
    void findByCpfShouldNormalizeTheDocument() {
        Customer customer = Fixtures.customer();
        when(customerRepository.findByCpf(new Cpf("52998224725"))).thenReturn(Optional.of(customer));

        assertThat(new FindCustomerByCpfUseCase(customerRepository).execute("529.982.247-25")).isSameAs(customer);
    }

    @Test
    void findByCpfShouldFailWithMaskedDocument() {
        when(customerRepository.findByCpf(any())).thenReturn(Optional.empty());

        assertThatThrownBy(() -> new FindCustomerByCpfUseCase(customerRepository).execute("52998224725"))
                .isInstanceOf(CustomerNotFoundException.class)
                .hasMessage("Nenhum cliente com o CPF ***.982.247-**");
    }

    @Test
    void getShouldFailWhenCustomerDoesNotExist() {
        UUID id = UUID.randomUUID();
        when(customerRepository.findById(id)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> new GetCustomerUseCase(customerRepository).execute(id))
                .isInstanceOf(CustomerNotFoundException.class);
    }

    @Test
    void listShouldSearchWithNormalizedQuery() {
        PageResult<Customer> page = new PageResult<>(List.of(Fixtures.customer()), 0, 100, 1);
        when(customerRepository.search("maria", Set.of(), 0, 100)).thenReturn(page);

        assertThat(new ListCustomersUseCase(customerRepository).execute(new ListCustomersQuery("  maria ", -3, 500)))
                .isEqualTo(page);
    }

    @Test
    void listShouldTreatBlankNameAsNoFilter() {
        PageResult<Customer> empty = new PageResult<>(List.of(), 0, 1, 0);
        when(customerRepository.search(null, Set.of(), 0, 1)).thenReturn(empty);

        assertThat(new ListCustomersUseCase(customerRepository).execute(new ListCustomersQuery(" ", 0, 0)))
                .isEqualTo(empty);
    }
}
