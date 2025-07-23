package com.tracktrove.controller;

import com.tracktrove.entity.LedgerEntry;
import com.tracktrove.repository.LedgerEntryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/ledger")
public class LedgerController {
    private final LedgerEntryRepository ledgerRepo;

    public LedgerController(LedgerEntryRepository ledgerRepo) {
        this.ledgerRepo = ledgerRepo;
    }

    @GetMapping("/{transactionId}")
    public ResponseEntity<List<LedgerEntry>> getLedger(@PathVariable UUID transactionId) {
        List<LedgerEntry> entries = ledgerRepo.findByTransactionId(transactionId);
        return entries.isEmpty() ? ResponseEntity.noContent().build() : ResponseEntity.ok(entries);
    }
}

