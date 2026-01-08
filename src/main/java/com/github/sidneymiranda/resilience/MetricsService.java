package com.github.sidneymiranda.resilience;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Timer;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;

@Service
public class MetricsService {

    private final Counter successCounter;
    private final Counter failureCounter;
    private final Timer latencyTimer;

    public MetricsService(MeterRegistry registry) {
        this.successCounter = Counter.builder("external_service.success")
                .description("Número de chamadas externas bem-sucedidas")
                .register(registry);
        this.failureCounter = Counter.builder("external_service.failure")
                .description("Número de chamadas externas que falharam")
                .register(registry);
        this.latencyTimer = Timer.builder("external_service.latency")
                .description("Latência das chamadas ao serviço externo")
                .publishPercentiles(0.5, 0.95)
                .register(registry);
    }

    public void recordSuccess(long durationMillis) {
        successCounter.increment();
        latencyTimer.record(durationMillis, TimeUnit.MILLISECONDS);
    }

    public void recordFailure(long durationMillis) {
        failureCounter.increment();
        latencyTimer.record(durationMillis, TimeUnit.MILLISECONDS);
    }
}

