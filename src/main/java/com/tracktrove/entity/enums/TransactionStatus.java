package com.tracktrove.entity.enums;

public enum TransactionStatus {
    INITIATED,
    ESCROW,
    COMPLETED,
    FAILED,
    REFUNDED,
    SETTLED,
    DISPUTED,
    RETRIED, // Add any other statuses you plan to use
    RETRY_PENDING,
    PERMANENTLY_FAILED
}
