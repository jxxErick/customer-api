package com.jxxerick.customerapi.infrastructure.persistence;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

interface SpringDataCustomerChangeRepository extends JpaRepository<CustomerChangeEntity, Long> {

    Page<CustomerChangeEntity> findByCustomerId(UUID customerId, Pageable pageable);
}
