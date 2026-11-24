package com.jxxerick.customerapi.web.dto;

import com.jxxerick.customerapi.domain.model.DuplicateCandidate;
import com.jxxerick.customerapi.domain.model.DuplicateReason;

import java.util.Set;

public record DuplicateCandidateResponse(CustomerResponse first, CustomerResponse second, double score,
                                         Set<DuplicateReason> reasons) {

    public static DuplicateCandidateResponse from(DuplicateCandidate candidate) {
        return new DuplicateCandidateResponse(CustomerResponse.from(candidate.first()),
                CustomerResponse.from(candidate.second()), candidate.score(), candidate.reasons());
    }
}
