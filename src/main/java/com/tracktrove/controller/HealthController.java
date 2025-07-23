package com.tracktrove.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class HealthController {
    @GetMapping("/health")
    public String status() {
        return "TrackTrove backend is up and healthy 🔥";
    }
}
