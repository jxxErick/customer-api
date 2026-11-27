package com.jxxerick.customerapi.infrastructure.persistence;

import com.jxxerick.customerapi.domain.model.CustomerFilter;
import com.jxxerick.customerapi.domain.model.Tag;
import org.springframework.data.jpa.domain.Specification;

import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;

/** Composable filters for the customer listing. */
final class CustomerSpecifications {

    private CustomerSpecifications() {
    }

    /** Null fragment matches everyone. */
    static Specification<CustomerEntity> nameContains(String fragment) {
        if (fragment == null) {
            return Specification.where(null);
        }
        String pattern = "%" + escapeLike(fragment.toLowerCase(Locale.ROOT)) + "%";
        return (root, query, cb) -> cb.like(cb.lower(root.get("name")), pattern, '\\');
    }

    /** The customer must carry every tag (AND); an empty set matches everyone. */
    static Specification<CustomerEntity> hasAllTags(Set<String> tags) {
        Specification<CustomerEntity> spec = Specification.where(null);
        for (String tag : tags) {
            spec = spec.and((root, query, cb) -> cb.isMember(tag, root.get("tags")));
        }
        return spec;
    }

    /** Two-letter code, already upper-cased; null matches everyone. */
    static Specification<CustomerEntity> inState(String state) {
        return state == null ? Specification.where(null) : (root, query, cb) -> cb.equal(root.get("state"), state);
    }

    /** Case-insensitive exact match; null matches everyone. */
    static Specification<CustomerEntity> inCity(String city) {
        if (city == null) {
            return Specification.where(null);
        }
        String lower = city.toLowerCase(Locale.ROOT);
        return (root, query, cb) -> cb.equal(cb.lower(root.get("city")), lower);
    }

    /** Every criterion of the filter, combined with AND. */
    static Specification<CustomerEntity> matching(CustomerFilter filter) {
        Set<String> tags = filter.tags().stream().map(Tag::value).collect(Collectors.toSet());
        return nameContains(filter.nameContains())
                .and(hasAllTags(tags))
                .and(inState(filter.state()))
                .and(inCity(filter.city()));
    }

    private static String escapeLike(String value) {
        return value.replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_");
    }
}
