package com.jxxerick.customerapi.web.dto;

import com.jxxerick.customerapi.domain.model.CustomerStats;

import java.math.BigDecimal;
import java.util.List;

public record CustomerStatsResponse(long totalCustomers, List<StateResponse> states) {

    public static CustomerStatsResponse from(CustomerStats stats) {
        return new CustomerStatsResponse(stats.totalCustomers(), stats.states().stream()
                .map(state -> new StateResponse(state.state(), state.customers(), state.percentage(),
                        state.cities().stream().map(c -> new CityResponse(c.city(), c.customers())).toList()))
                .toList());
    }

    public record StateResponse(String state, long customers, BigDecimal percentage, List<CityResponse> cities) {
    }

    public record CityResponse(String city, long customers) {
    }
}
