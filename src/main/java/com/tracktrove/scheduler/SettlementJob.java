package com.tracktrove.scheduler;

import com.tracktrove.entity.Transaction;
import com.tracktrove.entity.enums.TransactionStatus;
import com.tracktrove.entity.enums.LedgerType;
import com.tracktrove.repository.TransactionRepository;
import com.tracktrove.service.TransactionService;
import com.tracktrove.service.LedgerService;
import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class SettlementJob implements Job {

    private final TransactionRepository transactionRepository;
    private final TransactionService transactionService;
    private final LedgerService ledgerService;

    public SettlementJob(TransactionRepository transactionRepository,
                         TransactionService transactionService,
                         LedgerService ledgerService) {
        this.transactionRepository = transactionRepository;
        this.transactionService = transactionService;
        this.ledgerService = ledgerService;
    }

    @Override
    public void execute(JobExecutionContext context) throws JobExecutionException {
        List<Transaction> escrows = transactionRepository.findByCurrentStatus(TransactionStatus.ESCROW);
        System.out.println("Settlement job called: " + escrows.size() + " transactions pending.");

        for (Transaction txn : escrows) {
            try {
                Transaction updated = transactionService.updateTransactionStatus(txn.getId(), TransactionStatus.SETTLED);

                ledgerService.recordEntry(
                        updated.getId(),
                        updated.getAmount().doubleValue(),
                        LedgerType.SETTLEMENT,
                        "Auto-settlement job"
                );

                System.out.println("Ledger & status updated for txn: " + updated.getId());
            } catch (Exception e) {
                System.err.println("Failed settlement for txn " + txn.getId() + ": " + e.getMessage());
            }
        }
    }
}
