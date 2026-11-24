package com.jxxerick.customerapi.web;

import com.jxxerick.customerapi.domain.exception.AddressNotFoundException;
import com.jxxerick.customerapi.domain.exception.AddressProviderUnavailableException;
import com.jxxerick.customerapi.domain.exception.ConcurrentUpdateException;
import com.jxxerick.customerapi.domain.exception.CpfAlreadyInUseException;
import com.jxxerick.customerapi.domain.exception.CustomerMergeConflictException;
import com.jxxerick.customerapi.domain.exception.CustomerNotFoundException;
import com.jxxerick.customerapi.domain.exception.DomainException;
import com.jxxerick.customerapi.domain.exception.DomainValidationException;
import com.jxxerick.customerapi.domain.exception.EmailAlreadyInUseException;
import com.jxxerick.customerapi.domain.exception.IdempotencyKeyReusedException;
import com.jxxerick.customerapi.domain.exception.ImportTooLargeException;
import com.jxxerick.customerapi.web.csv.InvalidCsvException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.LinkedHashMap;
import java.util.Map;

/** Maps exceptions to RFC 7807 problem details. */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Map<Class<? extends DomainException>, HttpStatus> STATUS_BY_EXCEPTION = Map.of(
            CustomerNotFoundException.class, HttpStatus.NOT_FOUND,
            ConcurrentUpdateException.class, HttpStatus.PRECONDITION_FAILED,
            EmailAlreadyInUseException.class, HttpStatus.CONFLICT,
            CpfAlreadyInUseException.class, HttpStatus.CONFLICT,
            CustomerMergeConflictException.class, HttpStatus.CONFLICT,
            IdempotencyKeyReusedException.class, HttpStatus.UNPROCESSABLE_ENTITY,
            ImportTooLargeException.class, HttpStatus.PAYLOAD_TOO_LARGE,
            AddressNotFoundException.class, HttpStatus.UNPROCESSABLE_ENTITY,
            DomainValidationException.class, HttpStatus.BAD_REQUEST,
            AddressProviderUnavailableException.class, HttpStatus.SERVICE_UNAVAILABLE);

    @ExceptionHandler(DomainException.class)
    ProblemDetail handleDomain(DomainException exception) {
        HttpStatus status = STATUS_BY_EXCEPTION.getOrDefault(exception.getClass(), HttpStatus.UNPROCESSABLE_ENTITY);
        return ProblemDetail.forStatusAndDetail(status, exception.getMessage());
    }

    @ExceptionHandler(InvalidIfMatchException.class)
    ProblemDetail handleInvalidIfMatch(InvalidIfMatchException exception) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, exception.getMessage());
    }

    @ExceptionHandler(InvalidCsvException.class)
    ProblemDetail handleInvalidCsv(InvalidCsvException exception) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, exception.getMessage());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    ProblemDetail handleValidation(MethodArgumentNotValidException exception) {
        Map<String, String> errors = new LinkedHashMap<>();
        exception.getBindingResult().getFieldErrors()
                .forEach(error -> errors.putIfAbsent(error.getField(), error.getDefaultMessage()));

        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, "Dados inválidos");
        problem.setProperty("errors", errors);
        return problem;
    }
}
