package com.tracktrove.scheduler;

import com.tracktrove.service.TransactionService;
import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Component
public class EscrowJob implements Job {

    private final RedisTemplate<String, String> redisTemplate;
    private final TransactionService transactionService;

    public EscrowJob(RedisTemplate<String, String> redisTemplate,
                     TransactionService transactionService) {
        this.redisTemplate = redisTemplate;
        this.transactionService = transactionService;
    }

    @Override
    public void execute(JobExecutionContext context) throws JobExecutionException {
        System.out.println("Escrow job called: Checking Redis for new transactions...");

        while (true) {
            String txnId = redisTemplate.opsForList().rightPop("transaction_queue", 2, TimeUnit.SECONDS);
            if (txnId == null) break;

            try {
                UUID uuid = UUID.fromString(txnId);
                transactionService.updateTransactionStatus(uuid, com.tracktrove.entity.enums.TransactionStatus.ESCROW);
                System.out.println("Moved transaction to ESCROW_INITIATED: " + txnId);
            } catch (Exception e) {
                System.err.println("EscrowJob error for ID " + txnId + ": " + e.getMessage());
            }
        }
    }
}
