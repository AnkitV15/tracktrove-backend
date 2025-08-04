package com.tracktrove.scheduler;

import com.tracktrove.entity.Transaction;
import com.tracktrove.entity.enums.TransactionStatus;
import com.tracktrove.entity.enums.LedgerType;
import com.tracktrove.service.TransactionService;
import com.tracktrove.service.TraceService;
import com.tracktrove.service.LedgerService;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.UUID;

@Component
public class EscrowSweep {

    private final TransactionService transactionService;
    private final TraceService traceService;
    private final LedgerService ledgerService;

    public EscrowSweep(TransactionService transactionService,
                       TraceService traceService,
                       LedgerService ledgerService) {
        this.transactionService = transactionService;
        this.traceService = traceService;
        this.ledgerService = ledgerService;
    }

    @Scheduled(fixedRate = 60000) // every 60 seconds
    public void cycleInitiatedToEscrow() {
        List<Transaction> stuckTransactions = transactionService.getByStatus(TransactionStatus.INITIATED);

        for (Transaction txn : stuckTransactions) {
            UUID txnId = txn.getId();

            transactionService.updateTransactionStatus(txnId, TransactionStatus.ESCROW);

            ledgerService.recordEntry(
                txnId,
                txn.getAmount().doubleValue(),
                LedgerType.ESCROW,
                "Scheduled fallback to ESCROW"
            );

            traceService.createAndSaveTrace(
                "SCHEDULED_ESCROW",
                null,
                null,
                txnId,
                "Auto-scheduled recovery: INITIATED → ESCROW",
                0
            );
        }

        System.out.println("[Scheduler] Escrowed " + stuckTransactions.size() + " stuck INITIATED txns.");
    }
}
