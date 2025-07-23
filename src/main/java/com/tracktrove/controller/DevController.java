package com.tracktrove.controller;

import com.tracktrove.scheduler.SettlementJob;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/dev")
public class DevController {

    private final SettlementJob settlementJob;

    public DevController(SettlementJob settlementJob) {
        this.settlementJob = settlementJob;
    }

    @GetMapping("/run-settlement")
    public ResponseEntity<String> runSettlementJob() {
        try {
            settlementJob.execute(null); // You can pass a dummy JobExecutionContext if needed
            return ResponseEntity.ok("✅ SettlementJob executed manually");
        } catch (Exception e) {
            return ResponseEntity.status(500).body("❌ SettlementJob failed: " + e.getMessage());
        }
    }
}

