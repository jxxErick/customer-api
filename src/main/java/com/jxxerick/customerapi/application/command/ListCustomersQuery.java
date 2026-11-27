package com.jxxerick.customerapi.application.command;

import com.jxxerick.customerapi.domain.model.Tag;

import java.util.Collection;
import java.util.Set;
import java.util.stream.Collectors;

/** Optional name filter, tags the customer must all have, plus zero-based paging. */
public record ListCustomersQuery(String name, Set<Tag> tags, int page, int size) {

    public static final int MAX_SIZE = 100;

    public ListCustomersQuery {
        name = name == null || name.isBlank() ? null : name.strip();
        tags = tags == null ? Set.of() : Set.copyOf(tags);
        page = Math.max(page, 0);
        size = Math.clamp(size, 1, MAX_SIZE);
    }

    public ListCustomersQuery(String name, int page, int size) {
        this(name, Set.of(), page, size);
    }

    /** Blank tags are ignored; an invalid one fails the whole query. */
    public static ListCustomersQuery of(String name, Collection<String> rawTags, int page, int size) {
        Set<Tag> tags = rawTags == null ? Set.of() : rawTags.stream()
                .filter(tag -> tag != null && !tag.isBlank())
                .map(Tag::new)
                .collect(Collectors.toSet());
        return new ListCustomersQuery(name, tags, page, size);
    }
}
