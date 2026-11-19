package com.jxxerick.customerapi.application.usecase;

import com.jxxerick.customerapi.application.command.RenameCustomerCommand;
import com.jxxerick.customerapi.domain.exception.CustomerNotFoundException;
import com.jxxerick.customerapi.domain.model.Customer;
import com.jxxerick.customerapi.domain.port.CustomerRepository;
import com.jxxerick.customerapi.support.Fixtures;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RenameCustomerUseCaseTest {

    @Mock
    private CustomerRepository customerRepository;

    private RenameCustomerUseCase useCase;

    @BeforeEach
    void setUp() {
        useCase = new RenameCustomerUseCase(new GetCustomerUseCase(customerRepository), customerRepository);
    }

    @Test
    void shouldRenameAndTrimName() {
        Customer customer = Fixtures.customer();
        when(customerRepository.findById(customer.getId())).thenReturn(Optional.of(customer));
        when(customerRepository.save(customer)).thenReturn(customer);

        Customer updated = useCase.execute(new RenameCustomerCommand(customer.getId(), "  Maria Souza  "));

        assertThat(updated.getName()).isEqualTo("Maria Souza");
    }

    @Test
    void shouldFailWhenCustomerIsMissing() {
        UUID id = UUID.randomUUID();
        when(customerRepository.findById(id)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> useCase.execute(new RenameCustomerCommand(id, "Maria")))
                .isInstanceOf(CustomerNotFoundException.class);
        verify(customerRepository, never()).save(any());
    }
}
