package com.tracktrove.redis;

import com.tracktrove.service.TransactionService;
import com.tracktrove.service.TraceService;
import com.tracktrove.service.LedgerService;
import com.tracktrove.entity.enums.TransactionStatus;
import com.tracktrove.entity.enums.LedgerType;
import org.springframework.data.redis.connection.Message;
import org.springframework.data.redis.connection.MessageListener;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
public class KeyExpiredListener implements MessageListener {

    private final TransactionService transactionService;
    private final TraceService traceService;
    private final LedgerService ledgerService;

    public KeyExpiredListener(TransactionService transactionService,
                              TraceService traceService,
                              LedgerService ledgerService) {
        this.transactionService = transactionService;
        this.traceService = traceService;
        this.ledgerService = ledgerService;
    }

    @Override
    public void onMessage(Message message, byte[] pattern) {
        String expiredKey = message.toString();
        System.out.println("[Redis] Key expired: " + expiredKey);

        if (expiredKey.startsWith("txn:")) {
            String id = expiredKey.replace("txn:", "");
            try {
                UUID txnId = UUID.fromString(id);

                // ⛳ Escrow trigger
                transactionService.updateTransactionStatus(txnId, TransactionStatus.ESCROW);
                System.out.println("[Redis] Moved txn to ESCROW: " + txnId);

                // 🧾 Ledger entry
                ledgerService.recordEntry(
                        txnId,
                        transactionService.getById(txnId).getAmount().doubleValue(),
                        LedgerType.ESCROW,
                        "Auto transition via Redis key expiration"
                );

                // 📜 Trace log
                traceService.createAndSaveTrace(
                        "AUTO_ESCROW",
                        null,
                        null,
                        txnId,
                        "Redis key expired; auto-moved to ESCROW.",
                        0
                );
            } catch (Exception e) {
                System.err.println("[Redis] Failed to process expired txn key: " + id);
                e.printStackTrace();
            }
        }
    }

}
