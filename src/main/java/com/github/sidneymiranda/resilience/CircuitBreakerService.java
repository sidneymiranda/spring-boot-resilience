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
    public String callServiceWithCircuitBreaker() {
        log.info("Calling external service with Circuit Breaker protection...");
        return externalService.callExternalService();
    }

    @Retry(name = RETRY_INSTANCE, fallbackMethod = "fallbackMethod")
    public String callServiceWithRetry() {
        log.info("Calling external service with Retry protection...");
        return externalService.callExternalService();
    }

    @Bulkhead(name = BULKHEAD_INSTANCE, fallbackMethod = "fallbackMethod")
    public String callServiceWithBulkhead() {
        log.info("Calling external service with Bulkhead protection...");
        return externalService.callExternalService();
    }

    @RateLimiter(name = RATE_LIMITER_INSTANCE, fallbackMethod = "fallbackMethod")
    public String callServiceWithRateLimiter() {
        log.info("Calling external service with Rate Limiter protection...");
        return externalService.callExternalService();
    }

    @TimeLimiter(name = TIME_LIMITER_INSTANCE, fallbackMethod = "timeLimiterFallbackMethod")
    public CompletableFuture<String> callServiceWithTimeLimiter() {
        log.info("Calling external service with Time Limiter protection...");
        return CompletableFuture.supplyAsync(externalService::callExternalService);
    }

    private String fallbackMethod(Exception throwable) {
        log.error("Fallback method called due to: {}", throwable.getMessage());
        return "Fallback response: External service is currently unavailable. Please try again later.";
    }

    private String fallbackMethod(CallNotPermittedException throwable) {
        log.warn("Circuit Breaker is OPEN, call not permitted: {}", throwable.getMessage());
        return "Fallback response: Circuit breaker is OPEN. Calls are temporarily blocked.";
    }

    private String fallbackMethod(RequestNotPermitted throwable) {
        log.warn("Rate limit exceeded: {}", throwable.getMessage());
        return "Fallback response: Rate limit exceeded. Too many requests in the current period.";
    }

    private String fallbackMethod(BulkheadFullException throwable) {
        log.warn("Bulkhead is full: {}", throwable.getMessage());
        return "Fallback response: Bulkhead is full. Too many concurrent calls in progress.";
    }

    private CompletableFuture<String> timeLimiterFallbackMethod(Exception throwable) {
        log.error("Time Limiter fallback method called due to: {}", throwable.getMessage());
        return CompletableFuture.supplyAsync(() -> "Fallback response: The service took too long to response. Please try again later.");
    }

    private CompletableFuture<String> timeLimiterFallbackMethod(TimeoutException exception) {
        log.error("Time Limiter fallback method called due to timeout: {}", exception.getMessage());
        return CompletableFuture.supplyAsync(() -> "Fallback response: The service request timed out. Please try again later.");
    }
}
