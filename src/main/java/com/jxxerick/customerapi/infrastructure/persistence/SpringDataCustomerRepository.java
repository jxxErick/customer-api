package com.jxxerick.customerapi.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

interface SpringDataCustomerRepository extends JpaRepository<CustomerEntity, UUID>,
        JpaSpecificationExecutor<CustomerEntity> {

    boolean existsByEmail(String email);

    boolean existsByCpf(String cpf);

    Optional<CustomerEntity> findByCpf(String cpf);

    @Query("""
            select c.state as state, c.city as city, count(c) as customers
            from CustomerEntity c
            group by c.state, c.city""")
    List<LocationCountView> countByLocation();

    @Query("""
            select t as tag, count(c) as customers
            from CustomerEntity c join c.tags t
            group by t
            order by count(c) desc, t""")
    List<TagCountView> countByTag();
}
