package com.jxxerick.customerapi.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.UUID;

interface SpringDataTrashedCustomerRepository extends JpaRepository<TrashedCustomerEntity, UUID> {

    @Modifying
    @Query("delete from TrashedCustomerEntity t where t.purgeAt <= :now")
    int deletePurgeDue(@Param("now") Instant now);
}
