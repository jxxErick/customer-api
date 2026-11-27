package com.jxxerick.customerapi.domain.model;

import com.jxxerick.customerapi.domain.exception.DomainValidationException;

import java.util.Collection;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * Criteria shared by the listing and the saved segments; every null or empty field matches everyone
 * and the ones present are combined with AND.
 */
public record CustomerFilter(String nameContains, Set<Tag> tags, String state, String city) {

    public static final CustomerFilter ALL = new CustomerFilter(null, Set.of(), null, null);

    private static final Pattern STATE = Pattern.compile("[A-Z]{2}");

    public CustomerFilter {
        nameContains = blankToNull(nameContains);
        tags = tags == null ? Set.of() : Set.copyOf(tags);
        state = blankToNull(state);
        if (state != null) {
            state = state.toUpperCase(Locale.ROOT);
            if (!STATE.matcher(state).matches()) {
                throw new DomainValidationException("UF inválida: " + state);
            }
        }
        city = blankToNull(city);
    }

    /** Blank tags are ignored; an invalid one fails the whole filter. */
    public static CustomerFilter of(String nameContains, Collection<String> rawTags, String state, String city) {
        Set<Tag> tags = rawTags == null ? Set.of() : rawTags.stream()
                .filter(tag -> tag != null && !tag.isBlank())
                .map(Tag::new)
                .collect(Collectors.toSet());
        return new CustomerFilter(nameContains, tags, state, city);
    }

    public static CustomerFilter byName(String nameContains) {
        return new CustomerFilter(nameContains, Set.of(), null, null);
    }

    /** True when nothing is filtered. */
    public boolean isEmpty() {
        return nameContains == null && tags.isEmpty() && state == null && city == null;
    }

    private static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.strip();
    }
}
