package com.tracktrove.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tracktrove.dto.TransactionDTO;
import com.tracktrove.entity.Trace;
import com.tracktrove.repository.TraceRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
public class TraceService {

    private final TraceRepository traceRepo;
    private final ObjectMapper objectMapper;

    public TraceService(TraceRepository traceRepo) {
        this.traceRepo = traceRepo;
        this.objectMapper = new ObjectMapper();
    }

    @Transactional
    public Trace createAndSaveTrace(
            String stepName, // Changed order to match how it's used in RetryJob
            String dtoBeforeJson, // JSON string of DTO before
            String dtoAfterJson,  // JSON string of DTO after
            UUID transactionId,
            String errorMessage, // Changed from Exception ex to String errorMessage
            int retryCount
    ) {
        Trace trace = new Trace();
        trace.setTransactionId(transactionId);
        trace.setStepName(stepName);
        trace.setDtoBefore(dtoBeforeJson);
        trace.setDtoAfter(dtoAfterJson);
        trace.setErrorStack(errorMessage); // Set the error message string
        trace.setRetryCount(retryCount);
        trace.setTraceTime(Instant.now());

        return this.traceRepo.save(trace);
    }

    public void captureTrace(String stepName, TransactionDTO dtoBefore, TransactionDTO dtoAfter, UUID txnId, String ex, int retryCount)
    {
        try {
            String beforeJson = objectMapper.writeValueAsString(dtoBefore);
            String afterJson = objectMapper.writeValueAsString(dtoAfter);

            Trace trace = new Trace();
            trace.setTransactionId(txnId);
            trace.setDtoBefore(beforeJson);
            trace.setDtoAfter(afterJson);
            trace.setErrorStack(ex);
            trace.setRetryCount(retryCount);
            trace.setTraceTime(Instant.now());
            trace.setStepName(stepName);

            traceRepo.save(trace);

            

        } catch (JsonProcessingException e) {
            System.err.println("Failed to serialize DTO for trace: " + e.getMessage());
        }
    }

    @Transactional(readOnly = true) // Read-only transaction for fetching data
    public List<Trace> getTracesForTransaction(UUID transactionId) {
        return traceRepo.findByTransactionIdOrderByTraceTimeDesc(transactionId);
    }

    @Transactional(readOnly = true)
    public String getLastErrorStackForTransaction(UUID transactionId) {
        Trace lastErrorTrace = traceRepo.findFirstByTransactionIdAndErrorStackIsNotNullOrderByTraceTimeDesc(transactionId);
        return lastErrorTrace != null ? lastErrorTrace.getErrorStack() : null;
    }

    @Transactional(readOnly = true)
    public List<Trace> getRetryTracesForTransaction(UUID transactionId) {
        return traceRepo.findByTransactionIdOrderByTraceTimeDesc(transactionId); // Fetch all and let client filter or refine query
    }

}
