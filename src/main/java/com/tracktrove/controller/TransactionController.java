package com.tracktrove.controller;

import com.tracktrove.dto.TransactionDTO; // Import the DTO
import com.tracktrove.entity.Transaction; // Import the Entity
import com.tracktrove.service.TransactionService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import jakarta.validation.Valid; // Import for validation

@RestController
@RequestMapping("/api/transactions") // Base path for transaction related APIs
public class TransactionController {

    private final TransactionService transactionService;

    public TransactionController(TransactionService transactionService) {
        this.transactionService = transactionService;
    }

    // API: POST /api/transactions/initiate
    // Goal: Creates an INITIATED transaction
    @PostMapping("/initiate")
    public ResponseEntity<Transaction> initiateTransaction(
            @Valid @RequestBody TransactionDTO transactionDTO // Now accepts TransactionDTO
    ) {
        // Call the service method that accepts the DTO
        Transaction newTransaction = transactionService.initiateTransaction(transactionDTO);
        return new ResponseEntity<>(newTransaction, HttpStatus.CREATED);
    }

    // You will add other API endpoints here as you build out features
    // For example:
    // @GetMapping("/{id}")
    // public ResponseEntity<Transaction> getTransactionById(@PathVariable UUID id) { ... }

    // @PatchMapping("/{id}/status")
    // public ResponseEntity<Transaction> updateTransactionStatus(@PathVariable UUID id, @RequestBody Map<String, String> statusUpdate) { ... }
}
