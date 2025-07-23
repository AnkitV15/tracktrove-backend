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

        if (!expiredKey.startsWith("txn:")) return;

        String txnIdStr = expiredKey.substring(4);
        try {
            UUID txnId = UUID.fromString(txnIdStr);

            // Update status to ESCROW
            var updatedTxn = transactionService.updateTransactionStatus(txnId, TransactionStatus.ESCROW);

            // Record trace
            traceService.captureTrace("AUTO_ESCROW", null, null, txnId, null, 0);

            // Record ledger
            ledgerService.recordEntry(
                txnId,
                updatedTxn.getAmount().doubleValue(),
                LedgerType.ESCROW,
                "Auto-hold in escrow after Redis TTL"
            );

            System.out.println("🧬 TTL Escrow triggered for txn: " + txnId);
        } catch (IllegalArgumentException e) {
            System.out.println("❌ Invalid UUID: " + txnIdStr);
        }
    }
}
