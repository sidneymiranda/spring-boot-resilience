package com.github.sidneymiranda.resilience;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.concurrent.CompletableFuture;

@RestController
@RequestMapping("/api/resilience")
public class ResilienceController {
    private static final Logger log = LoggerFactory.getLogger(ResilienceController.class);

    private final CircuitBreakerService circuitBreakerService;

    public ResilienceController(CircuitBreakerService circuitBreakerService) {
        this.circuitBreakerService = circuitBreakerService;
    }

    @RequestMapping("/circuit-breaker")
    public ResponseEntity<String> circuitBreaker() {
        log.info("Circuit Breaker demo endpoint called.");
        return circuitBreakerService.callServiceWithCircuitBreaker();
    }

    @RequestMapping("/retry")
    public ResponseEntity<String> retry() {
        log.info("Retry demo endpoint called.");
        return circuitBreakerService.callServiceWithRetry();
    }

    @RequestMapping("/bulkhead")
    public ResponseEntity<String> bulkhead() {
        log.info("Bulkhead demo endpoint called.");
        return circuitBreakerService.callServiceWithBulkhead();
    }
    @RequestMapping("/rate-limiter")
    public ResponseEntity<String> rateLimiter() {
        log.info("Rate Limiter demo endpoint called.");
        return circuitBreakerService.callServiceWithRateLimiter();
    }

    @RequestMapping("/time-limiter")
    public CompletableFuture<ResponseEntity<String>> timeLimiter() {
        log.info("Time Limiter demo endpoint called.");
        return circuitBreakerService.callServiceWithTimeLimiter();
    }

}
