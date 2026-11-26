package com.jxxerick.customerapi.application.usecase;

import com.jxxerick.customerapi.domain.model.PageResult;
import com.jxxerick.customerapi.domain.model.TrashedCustomer;
import com.jxxerick.customerapi.domain.port.CustomerTrash;

public class ListTrashUseCase {

    public static final int MAX_SIZE = 100;

    private final CustomerTrash trash;

    public ListTrashUseCase(CustomerTrash trash) {
        this.trash = trash;
    }

    public PageResult<TrashedCustomer> execute(int page, int size) {
        return trash.list(Math.max(page, 0), Math.clamp(size, 1, MAX_SIZE));
    }
}
