package com.jxxerick.customerapi.domain.model;

import com.jxxerick.customerapi.domain.model.CustomerStats.CityCount;
import com.jxxerick.customerapi.domain.model.CustomerStats.StateStats;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class CustomerStatsTest {

    @Test
    void shouldGroupByStateAndSortBiggestFirst() {
        CustomerStats stats = CustomerStats.from(List.of(
                new LocationCount("RJ", "Rio de Janeiro", 2),
                new LocationCount("SP", "Campinas", 1),
                new LocationCount("SP", "São Paulo", 4),
                new LocationCount("MG", "Belo Horizonte", 2)));

        assertThat(stats.totalCustomers()).isEqualTo(9);
        assertThat(stats.states()).extracting(StateStats::state).containsExactly("SP", "MG", "RJ");
        assertThat(stats.states().get(0).customers()).isEqualTo(5);
        assertThat(stats.states().get(0).percentage()).isEqualByComparingTo("55.6");
        assertThat(stats.states().get(0).cities())
                .containsExactly(new CityCount("São Paulo", 4), new CityCount("Campinas", 1));
    }

    @Test
    void shouldReturnEmptyStatsWhenThereAreNoCustomers() {
        CustomerStats stats = CustomerStats.from(List.of());

        assertThat(stats.totalCustomers()).isZero();
        assertThat(stats.states()).isEmpty();
    }

    @Test
    void percentagesOfSingleStateShouldBeHundred() {
        CustomerStats stats = CustomerStats.from(List.of(new LocationCount("BA", "Salvador", 3)));

        assertThat(stats.states().get(0).percentage()).isEqualTo(new BigDecimal("100.0"));
    }
}
