package com.tracktrove.job;

import com.tracktrove.entity.Transaction;
import com.tracktrove.entity.enums.TransactionStatus;
import com.tracktrove.service.TraceService;
import com.tracktrove.service.TransactionService;
import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

@Component
public class RetryJob implements Job {

    private final TransactionService txnService;
    private final TraceService traceService;

    public RetryJob(TransactionService txnService, TraceService traceService) {
        this.txnService = txnService;
        this.traceService = traceService;
    }

    @Override
    @Transactional // Make the job execution transactional
    public void execute(JobExecutionContext context) throws JobExecutionException {
        System.out.println("Running Retry Job at " + Instant.now());

        // Fetch transactions that are FAILED or RETRY_PENDING and have not exceeded max retries
        // We'll fetch FAILED ones and if they are retried, their status will update.
        // If you want to explicitly use RETRY_PENDING for the job to pick up,
        // then the initial failure would set it to RETRY_PENDING.
        // For simplicity, let's have the job process 'FAILED' transactions for retry.
        List<Transaction> transactionsToRetry = txnService.getByStatus(TransactionStatus.FAILED);

        if (transactionsToRetry.isEmpty()) {
            System.out.println("No transactions in FAILED status to process for retry.");
            return;
        }

        for (Transaction txn : transactionsToRetry) {
            try {
                // Check if max retries exceeded
                if (txn.getRetryCount() >= TransactionService.getMaxRetries()) {
                    txn.setCurrentStatus(TransactionStatus.PERMANENTLY_FAILED);
                    txnService.save(txn);
                    traceService.createAndSaveTrace(
                            "RETRY_LIMIT_EXCEEDED",
                            null,
                            null,
                            txn.getId(),
                            "Transaction reached max retry limit (" + TransactionService.getMaxRetries() + ").",
                            txn.getRetryCount()
                    );
                    System.out.println("Transaction " + txn.getId() + " permanently failed after " + txn.getRetryCount() + " retries.");
                    continue; // Move to the next transaction
                }

                // Increment retry count for the current attempt
                int newRetryCount = txn.getRetryCount() + 1;
                txn.setRetryCount(newRetryCount); // Update retry count before simulating

                // Simulate success/failure based on the transaction's simulatedSuccessRate
                boolean success = Math.random() < txn.getSimulatedSuccessRate();

                if (success) {
                    // If retry is successful, transition to ESCROW
                    txn.setCurrentStatus(TransactionStatus.ESCROW);
                    txnService.save(txn); // Save the updated status and retry count
                    traceService.createAndSaveTrace(
                            "RETRIED_SUCCESS",
                            null,
                            null,
                            txn.getId(),
                            "Transaction retried successfully.",
                            newRetryCount
                    );
                    System.out.println("Transaction " + txn.getId() + " retried successfully. New status: ESCROW. Retries: " + newRetryCount);
                } else {
                    // If retry fails, keep as FAILED and save updated retry count
                    // Status remains FAILED, so the job will pick it up again in the next cycle if retries remain
                    txn.setCurrentStatus(TransactionStatus.FAILED); // Explicitly set, though it might already be FAILED
                    txnService.save(txn); // Save the updated retry count
                    traceService.createAndSaveTrace(
                            "RETRIED_FAILURE",
                            null,
                            null,
                            txn.getId(),
                            "Transaction retry failed.",
                            newRetryCount
                    );
                    System.out.println("Transaction " + txn.getId() + " retry failed. Retry count: " + newRetryCount);
                }

            } catch (Exception e) {
                String errorMessage = "Exception during retry processing for transaction " + txn.getId() + ": " + e.getMessage();
                System.err.println(errorMessage);
                e.printStackTrace();

                traceService.createAndSaveTrace(
                        "RETRY_EXCEPTION",
                        null,
                        null,
                        txn.getId(),
                        e.toString(),
                        txn.getRetryCount() // Use current retry count before the failed attempt
                );
                // Optionally, if an exception occurs, you might want to mark it as FAILED or PERMANENTLY_FAILED
                // depending on the severity and whether it's a transient error.
            }
        }
    }
}
