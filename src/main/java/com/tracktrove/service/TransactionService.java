package com.tracktrove.service;

import com.tracktrove.entity.Transaction;
import com.tracktrove.entity.enums.TransactionStatus;
import com.tracktrove.repository.TransactionRepository;
import com.tracktrove.dto.TransactionDTO;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Duration;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

@Service
public class TransactionService {

    private final TransactionRepository transactionRepository;
    private final TraceService traceService;
    private final RedisTemplate<String, String> redisTemplate;
    private final WebSocketService webSocketService;

    // Use a dedicated scheduler for a more robust simulation
    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
    private static final int MAX_RETRIES = 3;

    public TransactionService(TransactionRepository transactionRepository,
                              TraceService traceService,
                              RedisTemplate<String, String> redisTemplate,
                              WebSocketService webSocketService) {
        this.transactionRepository = transactionRepository;
        this.traceService = traceService;
        this.redisTemplate = redisTemplate;
        this.webSocketService = webSocketService; // Injected WebSocket service
    }

    // Redis TTL extractor
    private void enqueueTTLForInitiated(Transaction txn) {
        String redisKey = "txn:" + txn.getId();
        Duration ttl = Duration.ofMinutes(2);

        // Set a blank value with TTL to trigger key expiration event
        redisTemplate.opsForValue().set(redisKey, "", ttl);
        System.out.println("[Redis] TTL key set for transaction: " + redisKey + " (expires in " + ttl.toSeconds() + "s)");
    }

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
        txn.setRetryCount(0);

        Transaction savedTxn;

        if (Math.random() < simulatedSuccessRate) {
            txn.setCurrentStatus(TransactionStatus.INITIATED);
            savedTxn = transactionRepository.save(txn);
            System.out.println("Transaction " + savedTxn.getId() + " initially INITIATED (simulated success).");

            // Broadcast the new transaction status to all connected clients
            webSocketService.broadcast(
                    String.format("New transaction initiated for %s %s with ID: %s",
                            savedTxn.getAmount(), savedTxn.getCurrency(), savedTxn.getId().toString().substring(0, 8))
            );

            // Schedule the simulated status change to ESCROW
            scheduler.schedule(() -> {
                // This transaction logic would be more complex in a real app
                // but for simulation, we'll just update the status after a delay.
                updateTransactionStatusAndBroadcast(savedTxn.getId(), TransactionStatus.ESCROW);
            }, 5, TimeUnit.SECONDS);

            enqueueTTLForInitiated(savedTxn);
        } else {
            txn.setCurrentStatus(TransactionStatus.FAILED);
            savedTxn = transactionRepository.save(txn);
            System.out.println("Transaction " + savedTxn.getId() + " initially FAILED (simulated failure).");
            traceService.createAndSaveTrace(
                    "INITIAL_FAILURE",
                    initialPayloadJson,
                    null,
                    savedTxn.getId(),
                    "Initial transaction simulation failed.",
                    0
            );

            // Broadcast the failed transaction status
            webSocketService.broadcast(
                    String.format("Transaction %s FAILED on initiation.", savedTxn.getId().toString().substring(0, 8))
            );
        }

        return savedTxn;
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
        System.out.println("UUID-based transaction initiated, calling createAndSaveTransaction");
        return createAndSaveTransaction(vendorId, amount, currency, channel, initialPayloadJson, simulatedSuccessRate);
    }

    @Transactional
    public Transaction initiateTransaction(TransactionDTO transactionDTO) {
        System.out.println("DTO-based transaction initiated, calling createAndSaveTransaction");
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
            Transaction updatedTxn = transactionRepository.save(txn);

            // Optional: enforce Redis TTL if status changes to INITIATED
            if (newStatus == TransactionStatus.INITIATED) {
                enqueueTTLForInitiated(updatedTxn);
            }

            return updatedTxn;
        }).orElseThrow(() -> new RuntimeException("Transaction not found: " + transactionId));
    }

    @Transactional
    public void updateTransactionStatusAndBroadcast(UUID transactionId, TransactionStatus newStatus) {
        transactionRepository.findById(transactionId).ifPresent(txn -> {
            txn.setCurrentStatus(newStatus);
            transactionRepository.save(txn);

            // Broadcast the status change
            webSocketService.broadcast(
                    String.format("Transaction %s has moved to %s.",
                            txn.getId().toString().substring(0, 8),
                            newStatus.toString())
            );

            // Simulate the next step if the status is ESCROW
            if (newStatus == TransactionStatus.ESCROW) {
                scheduler.schedule(() -> {
                    updateTransactionStatusAndBroadcast(txn.getId(), TransactionStatus.SETTLED);
                }, 15, TimeUnit.SECONDS);
            }
        });
    }

    @Transactional(readOnly = true)
    public List<Transaction> getByStatus(TransactionStatus status) {
        return transactionRepository.findByCurrentStatus(status);
    }

    @Transactional
    public Transaction save(Transaction transaction) {
        Transaction saved = transactionRepository.save(transaction);
        if (saved.getCurrentStatus() == TransactionStatus.INITIATED) {
            enqueueTTLForInitiated(saved);
        }
        return saved;
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

            // Broadcast the manual retry event
            webSocketService.broadcast(
                    String.format("Manual retry forced for transaction %s.", transactionId.toString().substring(0, 8))
            );
            return updatedTxn;
        }).orElseThrow(() -> new RuntimeException("Transaction not found for manual retry: " + transactionId));
    }

    public static int getMaxRetries() {
        return MAX_RETRIES;
    }
}