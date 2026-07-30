package com.github.sidneymiranda.resilience;

import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

@Controller
@RequestMapping("/dashboard")
public class DashboardController {

    private final CircuitBreakerRegistry circuitBreakerRegistry;

    public DashboardController(CircuitBreakerRegistry circuitBreakerRegistry) {
        this.circuitBreakerRegistry = circuitBreakerRegistry;
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

    @GetMapping
    public String dashboard(Model model) {
        model.addAttribute("title", "Circuit Breaker Dashboard");
        return "dashboard";
    }
}
