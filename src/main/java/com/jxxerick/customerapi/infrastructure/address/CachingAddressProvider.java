package com.jxxerick.customerapi.infrastructure.address;

import com.jxxerick.customerapi.domain.model.PostalAddress;
import com.jxxerick.customerapi.domain.model.ZipCode;
import com.jxxerick.customerapi.domain.port.AddressProvider;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/** Decorator that caches successful lookups of any {@link AddressProvider} for a fixed TTL. */
public class CachingAddressProvider implements AddressProvider {

    private final AddressProvider delegate;
    private final Duration ttl;
    private final Clock clock;
    private final Map<ZipCode, CacheEntry> cache = new ConcurrentHashMap<>();

    public CachingAddressProvider(AddressProvider delegate, Duration ttl, Clock clock) {
        this.delegate = delegate;
        this.ttl = ttl;
        this.clock = clock;
    }

    @Override
    public Optional<PostalAddress> findByZipCode(ZipCode zipCode) {
        Instant now = clock.instant();
        CacheEntry cached = cache.get(zipCode);
        if (cached != null && cached.isValidAt(now)) {
            return Optional.of(cached.address());
        }

        Optional<PostalAddress> result = delegate.findByZipCode(zipCode);
        result.ifPresentOrElse(
                address -> cache.put(zipCode, new CacheEntry(address, now.plus(ttl))),
                () -> cache.remove(zipCode));
        return result;
    }

    private record CacheEntry(PostalAddress address, Instant expiresAt) {

        boolean isValidAt(Instant instant) {
            return instant.isBefore(expiresAt);
        }
    }
}
