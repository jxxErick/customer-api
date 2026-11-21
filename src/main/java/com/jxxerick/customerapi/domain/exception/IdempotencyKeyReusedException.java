package com.jxxerick.customerapi.domain.exception;

public class IdempotencyKeyReusedException extends DomainException {

    public IdempotencyKeyReusedException(String key) {
        super("A Idempotency-Key " + key + " já foi usada com outro corpo de requisição");
    }
}
