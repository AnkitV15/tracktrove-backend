package com.tracktrove.controller;

import com.tracktrove.dto.TransactionDTO; // Import the DTO
import com.tracktrove.entity.Trace;
import com.tracktrove.entity.Transaction; // Import the Entity
import com.tracktrove.entity.enums.TransactionStatus;
import com.tracktrove.service.TraceService;
import com.tracktrove.service.TransactionService;
import com.tracktrove.service.WebSocketService; // Import the WebSocketService
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid; // Import for validation

import java.util.List;
import java.util.UUID;

@CrossOrigin(origins = "http://localhost:5173/")
@RestController
@RequestMapping("/api/transactions") // Base path for transaction related APIs
public class TransactionController {

    private final TransactionService transactionService;
    private final TraceService traceService;
    private final WebSocketService webSocketService; // Inject the WebSocketService

    public TransactionController(TransactionService transactionService, TraceService traceService, WebSocketService webSocketService) {
        this.transactionService = transactionService;
        this.traceService = traceService;
        this.webSocketService = webSocketService;
    }

    // API: POST /api/transactions/initiate
    // Goal: Creates an INITIATED transaction
    @PostMapping("/initiate")
    public ResponseEntity<Transaction> initiateTransaction(
            @Valid @RequestBody TransactionDTO transactionDTO
    ) {
        Transaction newTransaction = transactionService.initiateTransaction(transactionDTO);
        System.out.println("🛬 Controller: /initiate endpoint hit");

        return new ResponseEntity<>(newTransaction, HttpStatus.CREATED);
    }

    @GetMapping("/retry-pending")
    public ResponseEntity<List<Transaction>> getRetryPendingTransactions() {
        List<Transaction> pendingTransactions = transactionService.getRetryPendingTransactions();
        return ResponseEntity.ok(pendingTransactions);
    }

    @GetMapping("/{id}/retries")
    public ResponseEntity<List<Trace>> getTransactionRetryHistory(@PathVariable UUID id) {
        List<Trace> retryHistory = traceService.getTracesForTransaction(id);
        if (retryHistory.isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(retryHistory);
    }

    @GetMapping("/{id}/error-stack")
    public ResponseEntity<String> getTransactionErrorStack(@PathVariable UUID id) {
        String errorStack = traceService.getLastErrorStackForTransaction(id);
        if (errorStack == null || errorStack.isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(errorStack);
    }

    @PatchMapping("/{id}/force-retry")
    public ResponseEntity<Transaction> forceManualRetry(@PathVariable UUID id) {
        try {
            Transaction updatedTransaction = transactionService.forceManualRetry(id);
            return ResponseEntity.ok(updatedTransaction);
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(null);
        }
    }

    @PatchMapping("/{id}/open-dispute")
    public ResponseEntity<Transaction> openDispute(@PathVariable UUID id) {
        try {
            Transaction transaction = transactionService.getById(id);
            if (transaction.getCurrentStatus() == TransactionStatus.SETTLED) {
                Transaction updatedTransaction = transactionService.updateTransactionStatus(id, TransactionStatus.DISPUTE_OPEN);
                webSocketService.broadcast(String.format("Dispute opened for transaction %s.", id.toString().substring(0, 8)));
                return ResponseEntity.ok(updatedTransaction);
            } else {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(null);
            }
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(null);
        }
    }

    @PatchMapping("/{id}/resolve-dispute")
    public ResponseEntity<Transaction> resolveDispute(@PathVariable UUID id) {
        try {
            Transaction transaction = transactionService.getById(id);
            if (transaction.getCurrentStatus() == TransactionStatus.DISPUTE_OPEN) {
                Transaction updatedTransaction = transactionService.updateTransactionStatus(id, TransactionStatus.DISPUTE_RESOLVED);
                webSocketService.broadcast(String.format("Dispute resolved for transaction %s.", id.toString().substring(0, 8)));
                return ResponseEntity.ok(updatedTransaction);
            } else {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(null);
            }
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(null);
        }
    }

    @GetMapping
    public ResponseEntity<List<Transaction>> getAllTransactions() {
        List<Transaction> transactions = transactionService.getAllTransactions();
        return ResponseEntity.ok(transactions);
    }

    @GetMapping("/{id}")
    public ResponseEntity<Transaction> getStatus(@PathVariable UUID id) {
        try {
            Transaction transaction = transactionService.getById(id);
            return ResponseEntity.ok(transaction);
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(null);
        }
    }

    @GetMapping("/{id}/status")
    public ResponseEntity<String> getTransactionById(@PathVariable UUID id) {
        try {
            Transaction transaction = transactionService.getById(id);
            return ResponseEntity.ok(transaction.getCurrentStatus().toString());
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(null);
        }
    }
}