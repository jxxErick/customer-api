package com.jxxerick.customerapi.application.usecase;

import com.jxxerick.customerapi.application.command.ImportRow;
import com.jxxerick.customerapi.domain.exception.AddressProviderUnavailableException;
import com.jxxerick.customerapi.domain.exception.DomainException;
import com.jxxerick.customerapi.domain.exception.ImportTooLargeException;
import com.jxxerick.customerapi.domain.model.Customer;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Creates customers one by one so a bad row never blocks the good ones. Each row goes
 * through the same rules as the single create, including duplicates inside the file.
 */
public class ImportCustomersUseCase {

    public static final int MAX_ROWS = 1000;

    private final CreateCustomerUseCase createCustomer;

    public ImportCustomersUseCase(CreateCustomerUseCase createCustomer) {
        this.createCustomer = createCustomer;
    }

    public Report execute(List<ImportRow> rows) {
        if (rows.size() > MAX_ROWS) {
            throw new ImportTooLargeException(rows.size(), MAX_ROWS);
        }
        List<RowResult> results = new ArrayList<>(rows.size());
        String providerDown = null;
        for (ImportRow row : rows) {
            String email = row.command().email();
            // once the zip code service is down, every remaining row would wait for the timeout
            if (providerDown != null) {
                results.add(RowResult.failed(row.line(), email, providerDown));
                continue;
            }
            try {
                Customer customer = createCustomer.execute(row.command());
                results.add(RowResult.created(row.line(), email, customer.getId()));
            } catch (AddressProviderUnavailableException e) {
                providerDown = e.getMessage();
                results.add(RowResult.failed(row.line(), email, providerDown));
            } catch (DomainException e) {
                results.add(RowResult.failed(row.line(), email, e.getMessage()));
            }
        }
        return Report.of(results);
    }

    public enum RowStatus {
        CREATED,
        FAILED
    }

    public record RowResult(int line, RowStatus status, String email, UUID customerId, String error) {

        static RowResult created(int line, String email, UUID customerId) {
            return new RowResult(line, RowStatus.CREATED, email, customerId, null);
        }

        static RowResult failed(int line, String email, String error) {
            return new RowResult(line, RowStatus.FAILED, email, null, error);
        }
    }

    public record Report(int total, int created, int failed, List<RowResult> rows) {

        static Report of(List<RowResult> rows) {
            int created = (int) rows.stream().filter(r -> r.status() == RowStatus.CREATED).count();
            return new Report(rows.size(), created, rows.size() - created, List.copyOf(rows));
        }
    }
}
