package com.jxxerick.customerapi.infrastructure.persistence;

import org.springframework.data.jpa.domain.Specification;

import java.util.Locale;
import java.util.Set;

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

    private static String escapeLike(String value) {
        return value.replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_");
    }
}
