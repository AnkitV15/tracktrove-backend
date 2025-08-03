package com.tracktrove.service;

import com.tracktrove.entity.Transaction;
import com.tracktrove.entity.enums.TransactionStatus;
import com.tracktrove.repository.TransactionRepository;
import com.tracktrove.dto.TransactionDTO;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@Service
public class TransactionService {

    private final TransactionRepository transactionRepository;
    private final TraceService traceService; // Inject TraceService

    private static final int MAX_RETRIES = 3;

    public TransactionService(TransactionRepository transactionRepository, TraceService traceService) {
        this.transactionRepository = transactionRepository;
        this.traceService = traceService;
    }

    @Transactional
    private Transaction createAndSaveTransaction(
            UUID vendorId,
            BigDecimal amount,
            String currency,
            String channel,
            String initialPayloadJson, // This is the 5th String argument
            Double simulatedSuccessRate // This is the 6th argument
    ) {
        Transaction txn = new Transaction();
        txn.setVendorId(vendorId);
        txn.setAmount(amount);
        txn.setCurrency(currency);
        txn.setChannel(channel);
        txn.setInitialPayload(initialPayloadJson);
        txn.setSimulatedSuccessRate(simulatedSuccessRate);
        txn.setRetryCount(0);

        if (Math.random() < simulatedSuccessRate) {
            txn.setCurrentStatus(TransactionStatus.INITIATED);
            System.out.println("Transaction " + txn.getId() + " initially INITIATED (simulated success).");
        } else {
            txn.setCurrentStatus(TransactionStatus.FAILED);
            System.out.println("Transaction " + txn.getId() + " initially FAILED (simulated failure).");
            traceService.createAndSaveTrace(
                    "INITIAL_FAILURE",
                    initialPayloadJson,
                    null,
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
            String initialPayloadJson, // This is the 5th String argument
            Double simulatedSuccessRate
    ) {
        // FIX: Ensure all 6 arguments are passed to createAndSaveTransaction
        return createAndSaveTransaction(vendorId, amount, currency, channel, initialPayloadJson, simulatedSuccessRate);
    }

    @Transactional
    public Transaction initiateTransaction(TransactionDTO transactionDTO) {
        return createAndSaveTransaction(
                transactionDTO.getVendorId(),
                transactionDTO.getAmount(),
                transactionDTO.getCurrency(),
                transactionDTO.getChannel(),
                transactionDTO.getInitialPayloadJson(), // This was already correct in the DTO version
                transactionDTO.getSimulatedSuccessRate()
        );
    }

    @Transactional
    public Transaction updateTransactionStatus(UUID transactionId, TransactionStatus newStatus) {
        return transactionRepository.findById(transactionId).map(txn -> {
            txn.setCurrentStatus(newStatus);
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

    @Transactional(readOnly = true)
    public List<Transaction> getAllTransactions() {
        return transactionRepository.findAll();
    }

    @Transactional
    public Transaction forceManualRetry(UUID transactionId) {
        return transactionRepository.findById(transactionId).map(txn -> {
            if (txn.getCurrentStatus() == TransactionStatus.SETTLED ||
                    txn.getCurrentStatus() == TransactionStatus.PERMANENTLY_FAILED ||
                    txn.getCurrentStatus() == TransactionStatus.COMPLETED) {
                throw new IllegalStateException("Cannot manually retry a transaction in status: " + txn.getCurrentStatus());
            }

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
