package com.tracktrove.controller;

import com.tracktrove.entity.Transaction;
import com.tracktrove.entity.enums.TransactionStatus;
import com.tracktrove.repository.TransactionRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Set;

@RestController
@RequestMapping("/admin/patch")
public class AdminPatchController {

    @Autowired
    private TransactionRepository transactionRepository;

    @Autowired
    private RedisTemplate<String, String> redisTemplate;

    @PostMapping("/initiated-to-queue")
    public ResponseEntity<String> patchInitiatedTxns() {
        List<Transaction> txns = transactionRepository.findByCurrentStatus(TransactionStatus.INITIATED);

        txns.forEach(txn -> {
            redisTemplate.opsForList().rightPush("transaction_queue", txn.getId().toString());
            System.out.println("📦 Pushed to Redis queue: " + txn.getId());
        });

        return ResponseEntity.ok("✅ Pushed " + txns.size() + " INITIATED txns to queue.");
    }

    @GetMapping("/debug/ttl")
    public void printTTLs() {
        Set<String> keys = redisTemplate.keys("txn:*");
        for (String key : keys) {
            Long ttl = redisTemplate.getExpire(key);
            System.out.println("Key: " + key + " has TTL: " + ttl);
        }
    }

}
