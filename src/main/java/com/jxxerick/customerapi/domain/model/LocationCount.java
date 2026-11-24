package com.jxxerick.customerapi.domain.model;

/** How many customers live in one city. */
public record LocationCount(String state, String city, long customers) {
}
