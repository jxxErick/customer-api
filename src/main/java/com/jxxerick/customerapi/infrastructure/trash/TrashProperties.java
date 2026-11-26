package com.jxxerick.customerapi.infrastructure.trash;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

/** {@code retention}: how long a deleted customer can be restored; {@code purgeInterval}: how often the trash is emptied. */
@ConfigurationProperties(prefix = "trash")
public record TrashProperties(Duration retention, Duration purgeInterval) {
}
