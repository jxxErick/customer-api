package com.jxxerick.customerapi.application.command;

import java.util.UUID;

/** Folds {@code duplicateId} into {@code targetId}; {@code expectedVersion} applies to the target. */
public record MergeCustomersCommand(UUID targetId, UUID duplicateId, Long expectedVersion) {
}
