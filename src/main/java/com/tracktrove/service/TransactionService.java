package com.tracktrove.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tracktrove.dto.TransactionDTO;
import com.tracktrove.entity.Transaction;
import com.tracktrove.entity.enums.TransactionStatus;
import com.tracktrove.repository.TransactionRepository;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.UUID;

@Service
public class TransactionService {

    private final TransactionRepository transactionRepository;
    private final TraceService traceService;
    private final ObjectMapper objectMapper;
    private final RedisTemplate<String, String> redisTemplate;

    // ⏱️ Delay before INITIATED becomes ESCROW via TTL (adjust as needed)
    private static final Duration ESCROW_DELAY = Duration.ofMinutes(1); // dev version

    public TransactionService(TransactionRepository transactionRepository,
                              TraceService traceService,
                              ObjectMapper objectMapper,
                              RedisTemplate<String, String> redisTemplate) {
        this.transactionRepository = transactionRepository;
        this.traceService = traceService;
        this.objectMapper = objectMapper;
        this.redisTemplate = redisTemplate;
    }

    // ✅ Create transaction from DTO & schedule TTL for auto-ESCROW
    public Transaction initiateTransaction(TransactionDTO dto) {
        Transaction txn = new Transaction();
        txn.setVendorId(dto.getVendorId());
        txn.setAmount(dto.getAmount());
        txn.setCurrency(dto.getCurrency());
        txn.setChannel(dto.getChannel());
        txn.setSimulatedSuccessRate(dto.getSimulatedSuccessRate());
        txn.setServiceContext(dto.getServiceContext());
        txn.setCurrentStatus(TransactionStatus.INITIATED);

        try {
            String payloadJson = objectMapper.writeValueAsString(dto);
            txn.setInitialPayload(payloadJson);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Failed to serialize DTO: " + e.getMessage());
        }

        Transaction saved = transactionRepository.save(txn);

        // 🔬 Log trace for INITIATED step
        traceService.captureTrace("INITIATED", dto, dto, saved.getId(), null, 0);

        // ⏱️ Schedule Redis TTL key for automatic ESCROW
        String redisKey = "txn:" + saved.getId();
        redisTemplate.opsForValue()
                .set(redisKey, "INITIATED", ESCROW_DELAY);

        return saved;
    }

    // 🔁 Status transition + trace log
    public Transaction updateTransactionStatus(UUID transactionId, TransactionStatus newStatus) {
        return transactionRepository.findById(transactionId).map(txn -> {
            txn.setCurrentStatus(newStatus);
            Transaction updated = transactionRepository.save(txn);

            traceService.captureTrace("STATUS_UPDATED", null, null, updated.getId(), null, 0);

            return updated;
        }).orElseThrow(() -> new RuntimeException("Transaction not found: " + transactionId));
    }

    // 🔎 Read-only fetch
    public Transaction getById(UUID transactionId) {
        return transactionRepository.findById(transactionId)
                .orElseThrow(() -> new RuntimeException("Transaction not found: " + transactionId));
    }
}
