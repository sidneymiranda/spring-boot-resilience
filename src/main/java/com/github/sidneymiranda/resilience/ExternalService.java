package com.github.sidneymiranda.resilience;

import org.springframework.stereotype.Service;

import java.util.Random;

@Service
public class ExternalService {

    private final Random random = new Random();
    private final MetricsService metricsService;

    public ExternalService(MetricsService metricsService) {
        this.metricsService = metricsService;
    }

    public String callExternalService() {
        long start = System.currentTimeMillis();
        try {
            // Simula um atraso (0-2 segundos)
            try {
                Thread.sleep(random.nextInt(2000));
            } catch (InterruptedException exception) {
                Thread.currentThread().interrupt();
            }

            if (random.nextBoolean()) {
                throw new RuntimeException("External service failure!");
            }

            long duration = System.currentTimeMillis() - start;
            metricsService.recordSuccess(duration);
            return "Response success from external service";
        } catch (RuntimeException ex) {
            long duration = System.currentTimeMillis() - start;
            metricsService.recordFailure(duration);
            throw ex;
        }
    }
}
