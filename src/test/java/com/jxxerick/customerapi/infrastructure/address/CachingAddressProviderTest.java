package com.jxxerick.customerapi.infrastructure.address;

import com.jxxerick.customerapi.domain.model.PostalAddress;
import com.jxxerick.customerapi.domain.model.ZipCode;
import com.jxxerick.customerapi.domain.port.AddressProvider;
import com.jxxerick.customerapi.support.Fixtures;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CachingAddressProviderTest {

    private static final ZipCode ZIP = new ZipCode("01001000");
    private static final Duration TTL = Duration.ofMinutes(10);

    @Mock
    private AddressProvider delegate;

    private MutableClock clock;
    private CachingAddressProvider provider;

    @BeforeEach
    void setUp() {
        clock = new MutableClock(Fixtures.NOW);
        provider = new CachingAddressProvider(delegate, TTL, clock);
    }

    @Test
    void shouldServeRepeatedLookupsFromCache() {
        PostalAddress address = Fixtures.postalAddress();
        when(delegate.findByZipCode(ZIP)).thenReturn(Optional.of(address));

        provider.findByZipCode(ZIP);
        Optional<PostalAddress> second = provider.findByZipCode(ZIP);

        assertThat(second).contains(address);
        verify(delegate, times(1)).findByZipCode(ZIP);
    }

    @Test
    void shouldHitDelegateAgainAfterTtlExpires() {
        when(delegate.findByZipCode(ZIP)).thenReturn(Optional.of(Fixtures.postalAddress()));

        provider.findByZipCode(ZIP);
        clock.advance(TTL);
        provider.findByZipCode(ZIP);

        verify(delegate, times(2)).findByZipCode(ZIP);
    }

    @Test
    void shouldNotCacheMisses() {
        when(delegate.findByZipCode(ZIP)).thenReturn(Optional.empty());

        provider.findByZipCode(ZIP);
        provider.findByZipCode(ZIP);

        verify(delegate, times(2)).findByZipCode(ZIP);
    }

    /** Test clock that can move forward. */
    private static final class MutableClock extends Clock {

        private Instant instant;

        MutableClock(Instant instant) {
            this.instant = instant;
        }

        void advance(Duration duration) {
            instant = instant.plus(duration);
        }

        @Override
        public ZoneId getZone() {
            return ZoneOffset.UTC;
        }

        @Override
        public Clock withZone(ZoneId zone) {
            return this;
        }

        @Override
        public Instant instant() {
            return instant;
        }
    }
}
