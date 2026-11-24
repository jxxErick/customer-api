package com.jxxerick.customerapi.application.usecase;

import com.jxxerick.customerapi.domain.model.Customer;
import com.jxxerick.customerapi.domain.model.PageResult;
import com.jxxerick.customerapi.domain.port.CustomerRepository;
import com.jxxerick.customerapi.support.Fixtures;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ExportCustomersUseCaseTest {

    private static final int SIZE = ExportCustomersUseCase.PAGE_SIZE;

    @Mock
    private CustomerRepository customerRepository;

    @Test
    void shouldWalkEveryPageInOrder() {
        List<Customer> first = customers(SIZE);
        List<Customer> second = customers(3);
        when(customerRepository.search("Maria", 0, SIZE)).thenReturn(new PageResult<>(first, 0, SIZE, SIZE + 3L));
        when(customerRepository.search("Maria", 1, SIZE)).thenReturn(new PageResult<>(second, 1, SIZE, SIZE + 3L));
        List<Customer> received = new ArrayList<>();

        long exported = new ExportCustomersUseCase(customerRepository).execute("  Maria ", received::add);

        assertThat(exported).isEqualTo(SIZE + 3L);
        assertThat(received).hasSize(SIZE + 3).startsWith(first.getFirst()).endsWith(second.getLast());
        verify(customerRepository, never()).search(eq("Maria"), eq(2), anyInt());
    }

    @Test
    void shouldStopAfterAnEmptyFirstPage() {
        when(customerRepository.search(null, 0, SIZE)).thenReturn(new PageResult<>(List.of(), 0, SIZE, 0));
        List<Customer> received = new ArrayList<>();

        long exported = new ExportCustomersUseCase(customerRepository).execute(" ", received::add);

        assertThat(exported).isZero();
        assertThat(received).isEmpty();
    }

    private static List<Customer> customers(int count) {
        return IntStream.range(0, count).mapToObj(i -> Fixtures.customer()).toList();
    }
}
