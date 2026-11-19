package com.jxxerick.customerapi.infrastructure.persistence;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

interface SpringDataCustomerRepository extends JpaRepository<CustomerEntity, UUID> {

    Page<CustomerEntity> findByNameContainingIgnoreCase(String name, Pageable pageable);

    boolean existsByEmail(String email);
}
