package com.tracktrove.service;

import com.tracktrove.entity.Transaction;
import com.tracktrove.entity.enums.TransactionStatus;
import com.tracktrove.repository.TransactionRepository;
import com.tracktrove.dto.TransactionDTO;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
public class TransactionService {

    private final TransactionRepository transactionRepository;
    private final TraceService traceService;

    // Define a maximum number of retries
    private static final int MAX_RETRIES = 3; // Example: allow up to 3 retries

    public TransactionService(TransactionRepository transactionRepository, TraceService traceService) {
        this.transactionRepository = transactionRepository;
        this.traceService = traceService;
    }

    // Helper method to create and save a transaction
    @Transactional
    private Transaction createAndSaveTransaction(
            UUID vendorId,
            BigDecimal amount,
            String currency,
            String channel,
            String initialPayloadJson,
            Double simulatedSuccessRate
    ) {
        Transaction txn = new Transaction();
        txn.setVendorId(vendorId);
        txn.setAmount(amount);
        txn.setCurrency(currency);
        txn.setChannel(channel);
        txn.setInitialPayload(initialPayloadJson);
        txn.setSimulatedSuccessRate(simulatedSuccessRate);
        txn.setRetryCount(0); // Initialize retry count to 0

        // Simulate initial success or failure
        if (Math.random() < simulatedSuccessRate) {
            txn.setCurrentStatus(TransactionStatus.INITIATED);
            System.out.println("Transaction " + txn.getId() + " initially INITIATED (simulated success).");
        } else {
            txn.setCurrentStatus(TransactionStatus.FAILED); // Set to FAILED if initial simulation fails
            System.out.println("Transaction " + txn.getId() + " initially FAILED (simulated failure).");
            // Log an initial failure trace
            traceService.createAndSaveTrace(
                    "INITIAL_FAILURE",
                    initialPayloadJson, // DTO before (initial payload)
                    null, // DTO after (no transformation yet)
                    txn.getId(),
                    "Initial transaction simulation failed.",
                    0
            );
        }

        return transactionRepository.save(txn);
    }


    @Transactional
    public Transaction initiateTransaction(
            UUID vendorId,
            BigDecimal amount,
            String currency,
            String channel,
            String initialPayloadJson,
            Double simulatedSuccessRate
    ) {
        return createAndSaveTransaction(vendorId, amount, currency, channel, initialPayloadJson, simulatedSuccessRate);
    }

    @Transactional
    public Transaction initiateTransaction(TransactionDTO transactionDTO) {
        return createAndSaveTransaction(
                transactionDTO.getVendorId(),
                transactionDTO.getAmount(),
                transactionDTO.getCurrency(),
                transactionDTO.getChannel(),
                transactionDTO.getInitialPayloadJson(),
                transactionDTO.getSimulatedSuccessRate()
        );
    }

    @Transactional
    public Transaction updateTransactionStatus(UUID transactionId, TransactionStatus newStatus) {
        return transactionRepository.findById(transactionId).map(txn -> {
            txn.setCurrentStatus(newStatus);
            // updatedAt is handled by @PreUpdate
            return transactionRepository.save(txn);
        }).orElseThrow(() -> new RuntimeException("Transaction not found: " + transactionId));
    }

    @Transactional(readOnly = true)
    public List<Transaction> getByStatus(TransactionStatus status) {
        return transactionRepository.findByCurrentStatus(status);
    }

    @Transactional
    public Transaction save(Transaction transaction) {
        return transactionRepository.save(transaction);
    }

    public Transaction getById(UUID id) {
        return transactionRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Transaction not found with ID: " + id));
    }

    @Transactional(readOnly = true)
    public List<Transaction> getRetryPendingTransactions() {
        return transactionRepository.findByCurrentStatus(TransactionStatus.RETRY_PENDING);
    }

    @Transactional
    public Transaction forceManualRetry(UUID transactionId) {
        return transactionRepository.findById(transactionId).map(txn -> {
            // Only allow manual retry if not already settled or permanently failed
            if (txn.getCurrentStatus() == TransactionStatus.SETTLED ||
                    txn.getCurrentStatus() == TransactionStatus.PERMANENTLY_FAILED ||
                    txn.getCurrentStatus() == TransactionStatus.COMPLETED) {
                throw new IllegalStateException("Cannot manually retry a transaction in status: " + txn.getCurrentStatus());
            }

            // Increment retry count and set to RETRY_PENDING
            txn.setRetryCount(txn.getRetryCount() + 1);
            txn.setCurrentStatus(TransactionStatus.RETRY_PENDING);
            Transaction updatedTxn = transactionRepository.save(txn);

            traceService.createAndSaveTrace(
                    "MANUAL_RETRY_FORCED",
                    null,
                    null,
                    txn.getId(),
                    "Manual retry initiated by admin.",
                    updatedTxn.getRetryCount()
            );
            System.out.println("Manual retry forced for transaction: " + transactionId);
            return updatedTxn;
        }).orElseThrow(() -> new RuntimeException("Transaction not found for manual retry: " + transactionId));
    }

    public static int getMaxRetries() {
        return MAX_RETRIES;
    }
}
