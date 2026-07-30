package com.github.sidneymiranda.resilience;

import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.stream.Collectors;

@Controller
@RequestMapping("/dashboard")
public class DashboardController {

    private final CircuitBreakerRegistry circuitBreakerRegistry;
    private final Environment environment;

    public DashboardController(CircuitBreakerRegistry circuitBreakerRegistry, Environment environment) {
        this.circuitBreakerRegistry = circuitBreakerRegistry;
        this.environment = environment;
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
