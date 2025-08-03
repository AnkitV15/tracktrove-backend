package com.tracktrove.controller;

import com.tracktrove.dto.TransactionDTO; // Import the DTO
import com.tracktrove.entity.Trace;
import com.tracktrove.entity.Transaction; // Import the Entity
import com.tracktrove.service.TraceService;
import com.tracktrove.service.TransactionService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid; // Import for validation

import java.util.List;
import java.util.UUID;

@CrossOrigin(origins = "http://172.25.160.1:5500")
@RestController
@RequestMapping("/api/transactions") // Base path for transaction related APIs
public class TransactionController {

    private final TransactionService transactionService;
    private final TraceService traceService;

    public TransactionController(TransactionService transactionService,TraceService traceService) {
        this.transactionService = transactionService;
        this.traceService = traceService;
    }

    // API: POST /api/transactions/initiate
    // Goal: Creates an INITIATED transaction
    @PostMapping("/initiate")
    public ResponseEntity<Transaction> initiateTransaction(
            @Valid @RequestBody TransactionDTO transactionDTO
    ) {
        Transaction newTransaction = transactionService.initiateTransaction(transactionDTO);
        return new ResponseEntity<>(newTransaction, HttpStatus.CREATED);
    }

    @GetMapping("/retry-pending")
    public ResponseEntity<List<Transaction>> getRetryPendingTransactions() {
        List<Transaction> pendingTransactions = transactionService.getRetryPendingTransactions();
        return ResponseEntity.ok(pendingTransactions);
    }

    @GetMapping("/{id}/retries")
    public ResponseEntity<List<Trace>> getTransactionRetryHistory(@PathVariable UUID id) {
        // Here, you might want to filter traces specific to retry attempts
        // For now, get all traces and let the client filter or refine TraceService method
        List<Trace> retryHistory = traceService.getTracesForTransaction(id); // Using general trace method
        // If you want only specific retry traces, you'd refine getRetryTracesForTransaction in TraceService
        // List<Trace> retryHistory = traceService.getRetryTracesForTransaction(id);
        if (retryHistory.isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(retryHistory);
    }

    @GetMapping("/{id}/error-stack")
    public ResponseEntity<String> getTransactionErrorStack(@PathVariable UUID id) {
        String errorStack = traceService.getLastErrorStackForTransaction(id);
        if (errorStack == null || errorStack.isEmpty()) {
            return ResponseEntity.notFound().build(); // Or HttpStatus.NO_CONTENT
        }
        return ResponseEntity.ok(errorStack);
    }

    @PatchMapping("/{id}/force-retry")
    public ResponseEntity<Transaction> forceManualRetry(@PathVariable UUID id) {
        try {
            Transaction updatedTransaction = transactionService.forceManualRetry(id);
            return ResponseEntity.ok(updatedTransaction);
        } catch (RuntimeException e) {
            // Handle case where transaction is not found or other errors
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(null); // Or a custom error response
        }
    }

    @GetMapping
    public ResponseEntity<List<Transaction>> getAllTransactions() {
        List<Transaction> transactions = transactionService.getAllTransactions();
        return ResponseEntity.ok(transactions);
    }

    @GetMapping("/{id}")
    public ResponseEntity<Transaction> getTransactionById(@PathVariable UUID id) {
        try {
            Transaction transaction = transactionService.getById(id);
            return ResponseEntity.ok(transaction);
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(null);
        }
    }
    // You will add other API endpoints here as you build out features
    // For example:
    // @GetMapping("/{id}")
    // public ResponseEntity<Transaction> getTransactionById(@PathVariable UUID id) { ... }

    // @PatchMapping("/{id}/status")
    // public ResponseEntity<Transaction> updateTransactionStatus(@PathVariable UUID id, @RequestBody Map<String, String> statusUpdate) { ... }
}
