package com.ecommerce.exception;

public class OutOfStockException extends RuntimeException {
    public OutOfStockException(String productName, int available, int requested) {
        super(String.format(
                "Insufficient stock for '%s': requested %d but only %d available",
                productName, requested, available));
    }
}