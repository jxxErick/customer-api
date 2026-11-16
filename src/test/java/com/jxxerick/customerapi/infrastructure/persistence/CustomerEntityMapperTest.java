package com.jxxerick.customerapi.infrastructure.persistence;

import com.jxxerick.customerapi.domain.model.Customer;
import com.jxxerick.customerapi.support.Fixtures;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class CustomerEntityMapperTest {

    @Test
    void shouldRoundTripCustomer() {
        Customer original = Fixtures.customer();

        Customer restored = CustomerEntityMapper.toDomain(CustomerEntityMapper.toEntity(original));

        assertThat(restored).usingRecursiveComparison().isEqualTo(original);
    }
}
