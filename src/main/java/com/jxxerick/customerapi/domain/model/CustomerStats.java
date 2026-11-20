package com.jxxerick.customerapi.domain.model;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/** Customers grouped by state and city, biggest first. */
public record CustomerStats(long totalCustomers, List<StateStats> states) {

    public CustomerStats {
        states = List.copyOf(states);
    }

    public record StateStats(String state, long customers, BigDecimal percentage, List<CityCount> cities) {

        public StateStats {
            cities = List.copyOf(cities);
        }
    }

    public record CityCount(String city, long customers) {
    }

    public static CustomerStats from(List<LocationCount> counts) {
        long total = counts.stream().mapToLong(LocationCount::customers).sum();
        Map<String, List<LocationCount>> byState = new TreeMap<>();
        counts.forEach(count -> byState.computeIfAbsent(count.state(), k -> new ArrayList<>()).add(count));

        List<StateStats> states = byState.entrySet().stream()
                .map(entry -> toState(entry.getKey(), entry.getValue(), total))
                .sorted(Comparator.comparingLong(StateStats::customers).reversed().thenComparing(StateStats::state))
                .toList();
        return new CustomerStats(total, states);
    }

    private static StateStats toState(String state, List<LocationCount> counts, long total) {
        long customers = counts.stream().mapToLong(LocationCount::customers).sum();
        List<CityCount> cities = counts.stream()
                .map(count -> new CityCount(count.city(), count.customers()))
                .sorted(Comparator.comparingLong(CityCount::customers).reversed().thenComparing(CityCount::city))
                .toList();
        return new StateStats(state, customers, percentage(customers, total), cities);
    }

    private static BigDecimal percentage(long part, long total) {
        return BigDecimal.valueOf(part * 100L).divide(BigDecimal.valueOf(total), 1, RoundingMode.HALF_UP);
    }
}
