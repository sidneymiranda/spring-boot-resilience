package com.github.sidneymiranda.resilience;

import io.github.resilience4j.bulkhead.Bulkhead;
import io.github.resilience4j.bulkhead.BulkheadRegistry;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import io.github.resilience4j.ratelimiter.RateLimiter;
import io.github.resilience4j.ratelimiter.RateLimiterRegistry;
import io.github.resilience4j.retry.Retry;
import io.github.resilience4j.retry.RetryRegistry;
import io.github.resilience4j.timelimiter.TimeLimiter;
import io.github.resilience4j.timelimiter.TimeLimiterRegistry;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;

@Controller
@RequestMapping("/dashboard")
public class DashboardController {

    private final CircuitBreakerRegistry circuitBreakerRegistry;
    private final RetryRegistry retryRegistry;
    private final RateLimiterRegistry rateLimiterRegistry;
    private final BulkheadRegistry bulkheadRegistry;
    private final TimeLimiterRegistry timeLimiterRegistry;
    private final Environment environment;

    // Resilience4j does not expose a built-in Metrics object for TimeLimiter, so successes/timeouts/errors
    // are tracked here via event listeners registered for every instance (existing and future ones).
    private final Map<String, AtomicLong> timeLimiterSuccessCounts = new ConcurrentHashMap<>();
    private final Map<String, AtomicLong> timeLimiterTimeoutCounts = new ConcurrentHashMap<>();
    private final Map<String, AtomicLong> timeLimiterErrorCounts = new ConcurrentHashMap<>();

    public DashboardController(CircuitBreakerRegistry circuitBreakerRegistry,
                                RetryRegistry retryRegistry,
                                RateLimiterRegistry rateLimiterRegistry,
                                BulkheadRegistry bulkheadRegistry,
                                TimeLimiterRegistry timeLimiterRegistry,
                                Environment environment) {
        this.circuitBreakerRegistry = circuitBreakerRegistry;
        this.retryRegistry = retryRegistry;
        this.rateLimiterRegistry = rateLimiterRegistry;
        this.bulkheadRegistry = bulkheadRegistry;
        this.timeLimiterRegistry = timeLimiterRegistry;
        this.environment = environment;

        this.timeLimiterRegistry.getAllTimeLimiters().forEach(this::registerTimeLimiterEvents);
        this.timeLimiterRegistry.getEventPublisher()
                .onEntryAdded(event -> registerTimeLimiterEvents(event.getAddedEntry()));
    }

    private void registerTimeLimiterEvents(TimeLimiter timeLimiter) {
        String name = timeLimiter.getName();
        timeLimiterSuccessCounts.putIfAbsent(name, new AtomicLong());
        timeLimiterTimeoutCounts.putIfAbsent(name, new AtomicLong());
        timeLimiterErrorCounts.putIfAbsent(name, new AtomicLong());

        timeLimiter.getEventPublisher()
                .onSuccess(event -> timeLimiterSuccessCounts.get(name).incrementAndGet())
                .onTimeout(event -> timeLimiterTimeoutCounts.get(name).incrementAndGet())
                .onError(event -> timeLimiterErrorCounts.get(name).incrementAndGet());
    }

    @GetMapping("/circuit-breakers")
    @ResponseBody
    public Map<String, Object> circuitBreakers() {
        return circuitBreakerRegistry.getAllCircuitBreakers().stream()
                .collect(
                        Collectors.toMap(
                                CircuitBreaker::getName,
                                circuitBreaker -> {
                                    Map<String, Object> details = new HashMap<>();
                                    details.put("state", circuitBreaker.getState().name());

                                    CircuitBreaker.Metrics metrics = circuitBreaker.getMetrics();
                                    details.put("failureRate", metrics.getFailureRate());
                                    details.put("slowCallRate", metrics.getSlowCallRate());
                                    details.put("numberOfBufferedCalls", metrics.getNumberOfBufferedCalls());
                                    details.put("numberOfFailedCalls", metrics.getNumberOfFailedCalls());
                                    details.put("numberOfSuccessfulCalls", metrics.getNumberOfSuccessfulCalls());
                                    details.put("numberOfSlowCalls", metrics.getNumberOfSlowCalls());
                                    details.put("numberOfNotPermittedCalls", metrics.getNumberOfNotPermittedCalls());

                                    return details;
                                }
                        )
                );
    }

    @GetMapping("/retries")
    @ResponseBody
    public Map<String, Object> retries() {
        return retryRegistry.getAllRetries().stream()
                .collect(
                        Collectors.toMap(
                                Retry::getName,
                                retry -> {
                                    Map<String, Object> details = new HashMap<>();
                                    Retry.Metrics metrics = retry.getMetrics();
                                    details.put("numberOfSuccessfulCallsWithoutRetryAttempt", metrics.getNumberOfSuccessfulCallsWithoutRetryAttempt());
                                    details.put("numberOfSuccessfulCallsWithRetryAttempt", metrics.getNumberOfSuccessfulCallsWithRetryAttempt());
                                    details.put("numberOfFailedCallsWithoutRetryAttempt", metrics.getNumberOfFailedCallsWithoutRetryAttempt());
                                    details.put("numberOfFailedCallsWithRetryAttempt", metrics.getNumberOfFailedCallsWithRetryAttempt());
                                    return details;
                                }
                        )
                );
    }

    @GetMapping("/rate-limiters")
    @ResponseBody
    public Map<String, Object> rateLimiters() {
        return rateLimiterRegistry.getAllRateLimiters().stream()
                .collect(
                        Collectors.toMap(
                                RateLimiter::getName,
                                rateLimiter -> {
                                    Map<String, Object> details = new HashMap<>();
                                    RateLimiter.Metrics metrics = rateLimiter.getMetrics();
                                    details.put("availablePermissions", metrics.getAvailablePermissions());
                                    details.put("numberOfWaitingThreads", metrics.getNumberOfWaitingThreads());
                                    return details;
                                }
                        )
                );
    }

    @GetMapping("/bulkheads")
    @ResponseBody
    public Map<String, Object> bulkheads() {
        return bulkheadRegistry.getAllBulkheads().stream()
                .collect(
                        Collectors.toMap(
                                Bulkhead::getName,
                                bulkhead -> {
                                    Map<String, Object> details = new HashMap<>();
                                    Bulkhead.Metrics metrics = bulkhead.getMetrics();
                                    details.put("availableConcurrentCalls", metrics.getAvailableConcurrentCalls());
                                    details.put("maxAllowedConcurrentCalls", metrics.getMaxAllowedConcurrentCalls());
                                    return details;
                                }
                        )
                );
    }

    @GetMapping("/time-limiters")
    @ResponseBody
    public Map<String, Object> timeLimiters() {
        return timeLimiterRegistry.getAllTimeLimiters().stream()
                .collect(
                        Collectors.toMap(
                                TimeLimiter::getName,
                                timeLimiter -> {
                                    String name = timeLimiter.getName();
                                    Map<String, Object> details = new HashMap<>();
                                    details.put("numberOfSuccessfulCalls", timeLimiterSuccessCounts.getOrDefault(name, new AtomicLong()).get());
                                    details.put("numberOfTimeouts", timeLimiterTimeoutCounts.getOrDefault(name, new AtomicLong()).get());
                                    details.put("numberOfErrors", timeLimiterErrorCounts.getOrDefault(name, new AtomicLong()).get());
                                    return details;
                                }
                        )
                );
    }

    @GetMapping("/configs")
    @ResponseBody
    public Map<String, Object> configs() {
        Map<String, Object> result = new LinkedHashMap<>();

        Map<String, Object> circuitBreaker = new LinkedHashMap<>();
        circuitBreaker.put("Failure Rate Threshold", environment.getProperty(
                "resilience4j.circuitbreaker.instances.externalServiceCircuitBreaker.failureRateThreshold") + "%");
        circuitBreaker.put("Sliding Window Size", environment.getProperty(
                "resilience4j.circuitbreaker.instances.externalServiceCircuitBreaker.slidingWindowSize"));
        circuitBreaker.put("Minimum Number Of Calls", environment.getProperty(
                "resilience4j.circuitbreaker.instances.externalServiceCircuitBreaker.minimumNumberOfCalls"));
        circuitBreaker.put("Wait Duration In Open State", environment.getProperty(
                "resilience4j.circuitbreaker.instances.externalServiceCircuitBreaker.waitDurationInOpenState") + " ms");
        circuitBreaker.put("Permitted Calls In Half-Open", environment.getProperty(
                "resilience4j.circuitbreaker.instances.externalServiceCircuitBreaker.permittedNumberOfCallsInHalfOpenState"));
        result.put("circuitBreaker", circuitBreaker);

        Map<String, Object> retry = new LinkedHashMap<>();
        retry.put("Max Attempts", environment.getProperty(
                "resilience4j.retry.instances.externalServiceRetry.maxAttempts"));
        retry.put("Wait Duration", environment.getProperty(
                "resilience4j.retry.instances.externalServiceRetry.waitDuration"));
        retry.put("Exponential Backoff", environment.getProperty(
                "resilience4j.retry.instances.externalServiceRetry.enable-exponential-backoff"));
        retry.put("Backoff Multiplier", environment.getProperty(
                "resilience4j.retry.instances.externalServiceRetry.exponential-backoff-multiplier"));
        result.put("retry", retry);

        Map<String, Object> rateLimiter = new LinkedHashMap<>();
        rateLimiter.put("Limit For Period", environment.getProperty(
                "resilience4j.rateLimiter.instances.externalServiceRateLimiter.limitForPeriod"));
        rateLimiter.put("Limit Refresh Period", environment.getProperty(
                "resilience4j.rateLimiter.instances.externalServiceRateLimiter.limitRefreshPeriod"));
        rateLimiter.put("Timeout Duration", environment.getProperty(
                "resilience4j.rateLimiter.instances.externalServiceRateLimiter.timeoutDuration") + " ms");
        result.put("rateLimiter", rateLimiter);

        Map<String, Object> timeLimiter = new LinkedHashMap<>();
        timeLimiter.put("Timeout Duration", environment.getProperty(
                "resilience4j.timelimiter.instances.externalServiceTimeLimiter.timeoutDuration"));
        timeLimiter.put("Cancel Running Future", environment.getProperty(
                "resilience4j.timelimiter.instances.externalServiceTimeLimiter.cancelRunningFuture"));
        result.put("timeLimiter", timeLimiter);

        Map<String, Object> bulkhead = new LinkedHashMap<>();
        bulkhead.put("Max Concurrent Calls", environment.getProperty(
                "resilience4j.bulkhead.instances.externalServiceBulkhead.maxConcurrentCalls"));
        bulkhead.put("Max Wait Duration", environment.getProperty(
                "resilience4j.bulkhead.instances.externalServiceBulkhead.maxWaitDuration"));
        result.put("bulkhead", bulkhead);

        return result;
    }

    @GetMapping
    public String dashboard(Model model) {
        model.addAttribute("title", "Resilience Patterns Dashboard");
        return "dashboard";
    }
}
