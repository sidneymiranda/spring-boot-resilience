package com.github.sidneymiranda.resilience;

import org.springframework.stereotype.Service;

import java.util.Random;

@Service
public class ExternalService {

    private final Random random = new Random();

    public String callExternalService() {
        // Simula um atraso (0-2 segundos)
        try {
            Thread.sleep(random.nextInt(2000));
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
        }

        if (random.nextBoolean()) {
            throw new RuntimeException("External service failure!");
        } else {
            return "Response success from external service";
        }
    }
}
