package com.jxxerick.customerapi.infrastructure.persistence;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.UUID;

interface SpringDataCustomerRepository extends JpaRepository<CustomerEntity, UUID> {

    Page<CustomerEntity> findByNameContainingIgnoreCase(String name, Pageable pageable);

    boolean existsByEmail(String email);

    @Query("""
            select c.state as state, c.city as city, count(c) as customers
            from CustomerEntity c
            group by c.state, c.city""")
    List<LocationCountView> countByLocation();
}
