package com.github.sidneymiranda.resilience;

import io.github.resilience4j.bulkhead.BulkheadFullException;
import io.github.resilience4j.bulkhead.annotation.Bulkhead;
import io.github.resilience4j.circuitbreaker.CallNotPermittedException;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.ratelimiter.RequestNotPermitted;
import io.github.resilience4j.ratelimiter.annotation.RateLimiter;
import io.github.resilience4j.retry.annotation.Retry;
import io.github.resilience4j.timelimiter.annotation.TimeLimiter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeoutException;

@Service
public class CircuitBreakerService {

    private static final String CIRCUIT_BREAKER_INSTANCE = "externalServiceCircuitBreaker";
    private static final String RETRY_INSTANCE = "externalServiceRetry";
    private static final String BULKHEAD_INSTANCE = "externalServiceBulkhead";
    private static final String RATE_LIMITER_INSTANCE = "externalServiceRateLimiter";
    private static final String TIME_LIMITER_INSTANCE = "externalServiceTimeLimiter";

    Logger log = LoggerFactory.getLogger(CircuitBreakerService.class);

    private final ExternalService externalService;

    public CircuitBreakerService(ExternalService externalService) {
        this.externalService = externalService;
    }

    @CircuitBreaker(name = CIRCUIT_BREAKER_INSTANCE, fallbackMethod = "fallbackMethod")
    public ResponseEntity<String> callServiceWithCircuitBreaker() {
        log.info("Calling external service with Circuit Breaker protection...");
        return ResponseEntity.ok(externalService.callExternalService());
    }

    @Retry(name = RETRY_INSTANCE, fallbackMethod = "fallbackMethod")
    public ResponseEntity<String> callServiceWithRetry() {
        log.info("Calling external service with Retry protection...");
        return ResponseEntity.ok(externalService.callExternalService());
    }

    @Bulkhead(name = BULKHEAD_INSTANCE, fallbackMethod = "fallbackMethod")
    public ResponseEntity<String> callServiceWithBulkhead() {
        log.info("Calling external service with Bulkhead protection...");
        return ResponseEntity.ok(externalService.callExternalService());
    }

    @RateLimiter(name = RATE_LIMITER_INSTANCE, fallbackMethod = "fallbackMethod")
    public ResponseEntity<String> callServiceWithRateLimiter() {
        log.info("Calling external service with Rate Limiter protection...");
        return ResponseEntity.ok(externalService.callExternalService());
    }

    @TimeLimiter(name = TIME_LIMITER_INSTANCE, fallbackMethod = "timeLimiterFallbackMethod")
    public CompletableFuture<ResponseEntity<String>> callServiceWithTimeLimiter() {
        log.info("Calling external service with Time Limiter protection...");
        return CompletableFuture.supplyAsync(() -> ResponseEntity.ok(externalService.callExternalService()));
    }

    private ResponseEntity<String> fallbackMethod(Exception throwable) {
        log.error("Fallback method called due to: {}", throwable.getMessage());
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                .body("Fallback response: External service is currently unavailable. Please try again later.");
    }

    private ResponseEntity<String> fallbackMethod(CallNotPermittedException throwable) {
        log.warn("Circuit Breaker is OPEN, call not permitted: {}", throwable.getMessage());
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                .body("Fallback response: Circuit breaker is OPEN. Calls are temporarily blocked.");
    }

    private ResponseEntity<String> fallbackMethod(RequestNotPermitted throwable) {
        log.warn("Rate limit exceeded: {}", throwable.getMessage());
        return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS)
                .body("Fallback response: Rate limit exceeded. Too many requests in the current period.");
    }

    private ResponseEntity<String> fallbackMethod(BulkheadFullException throwable) {
        log.warn("Bulkhead is full: {}", throwable.getMessage());
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                .body("Fallback response: Bulkhead is full. Too many concurrent calls in progress.");
    }

    private CompletableFuture<ResponseEntity<String>> timeLimiterFallbackMethod(Exception throwable) {
        log.error("Time Limiter fallback method called due to: {}", throwable.getMessage());
        return CompletableFuture.completedFuture(ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                .body("Fallback response: The service took too long to response. Please try again later."));
    }

    private CompletableFuture<ResponseEntity<String>> timeLimiterFallbackMethod(TimeoutException exception) {
        log.error("Time Limiter fallback method called due to timeout: {}", exception.getMessage());
        return CompletableFuture.completedFuture(ResponseEntity.status(HttpStatus.GATEWAY_TIMEOUT)
                .body("Fallback response: The service request timed out. Please try again later."));
    }
}
