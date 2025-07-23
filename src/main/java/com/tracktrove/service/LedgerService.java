package com.tracktrove.service;

import com.tracktrove.entity.LedgerEntry;
import com.tracktrove.entity.enums.LedgerType;
import com.tracktrove.repository.LedgerEntryRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
public class LedgerService {

    private final LedgerEntryRepository ledgerRepo;

    public LedgerService(LedgerEntryRepository ledgerRepo) {
        this.ledgerRepo = ledgerRepo;
    }

    public void recordEntry(UUID transactionId, Double amount, LedgerType type, String description)
    {
        LedgerEntry entry = new LedgerEntry();
        entry.setTransactionId(transactionId);
        entry.setAmount(amount);
        entry.setType(type); // or use LedgerType enum if typed
        entry.setDescription(description);
        entry.setEntryTimestamp(LocalDateTime.now());

        ledgerRepo.save(entry);

//        Ledger service test
//        LedgerService ledgerService = new LedgerService(ledgerRepo);
//        ledgerService.recordEntry(
//                UUID.fromString("430e30a1-c027-431a-a7f2-035b66c101bc"),
//                150.75,
//                LedgerType.SETTLEMENT,
//                "Manual test entry"
//        );


        System.out.println("Ledger recorded: " + transactionId + " | " + type);
    }
}
