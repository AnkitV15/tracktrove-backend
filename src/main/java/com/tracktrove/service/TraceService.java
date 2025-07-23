package com.tracktrove.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tracktrove.dto.TransactionDTO;
import com.tracktrove.entity.Trace;
import com.tracktrove.repository.TraceRepository;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.UUID;

@Service
public class TraceService {

    private final TraceRepository traceRepo;
    private final ObjectMapper objectMapper;

    public TraceService(TraceRepository traceRepo) {
        this.traceRepo = traceRepo;
        this.objectMapper = new ObjectMapper();
    }

    public void captureTrace(String stepName, TransactionDTO dtoBefore, TransactionDTO dtoAfter, UUID txnId, Exception ex, int retryCount)
    {
        try {
            String beforeJson = objectMapper.writeValueAsString(dtoBefore);
            String afterJson = objectMapper.writeValueAsString(dtoAfter);

            Trace trace = new Trace();
            trace.setTransactionId(txnId);
            trace.setDtoBefore(beforeJson);
            trace.setDtoAfter(afterJson);
            trace.setErrorStack(ex != null ? ex.toString() : null);
            trace.setRetryCount(retryCount);
            trace.setTraceTime(Instant.now());
            trace.setStepName(stepName);

            traceRepo.save(trace);

            

        } catch (JsonProcessingException e) {
            System.err.println("Failed to serialize DTO for trace: " + e.getMessage());
        }
    }
}
