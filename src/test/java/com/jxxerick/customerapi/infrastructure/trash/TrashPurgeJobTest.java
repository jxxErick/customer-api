package com.jxxerick.customerapi.infrastructure.trash;

import com.jxxerick.customerapi.domain.port.CustomerTrash;
import com.jxxerick.customerapi.support.Fixtures;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.ZoneOffset;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class TrashPurgeJobTest {

    @Test
    void shouldPurgeEverythingDueNow() {
        CustomerTrash trash = mock(CustomerTrash.class);

        new TrashPurgeJob(trash, Clock.fixed(Fixtures.NOW, ZoneOffset.UTC)).purgeDue();

        verify(trash).purgeDue(Fixtures.NOW);
    }
}
