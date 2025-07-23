package com.tracktrove.controller;

import com.tracktrove.entity.Trace;
import com.tracktrove.repository.TraceRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/trace")
@RequiredArgsConstructor
public class TraceController {

    private final TraceRepository traceRepository;

    @GetMapping("/{transactionId}")
    public ResponseEntity<List<Trace>> getTraces(@PathVariable UUID transactionId) {
        List<Trace> traces = traceRepository.findByTransactionId(transactionId);

        if (traces.isEmpty()) {
            return ResponseEntity.noContent().build();
        }

        return ResponseEntity.ok(traces);
    }
}
