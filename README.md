# Spring Boot Resilience — Demonstração de padrões de resiliência com Resilience4j

## Visão Geral

Projeto de exemplo que demonstra padrões de resiliência usando Resilience4j integrado a uma aplicação Spring Boot. O objetivo é mostrar na prática como aplicar Circuit Breaker, Retry, Bulkhead, Rate Limiter e Time Limiter em chamadas a um serviço externo simulado, além de expor métricas e um dashboard simples.

## Estrutura do Projeto

Principais arquivos e diretórios:

- `src/main/java/com/github/sidneymiranda/resilience/`
  - `SpringBootResilienceApplication.java` — classe principal do Spring Boot.
  - `ResilienceController.java` — endpoints de demonstração (rotas em `/api/resilience`).
  - `CircuitBreakerService.java` — serviços anotados com Resilience4j (CircuitBreaker, Retry, Bulkhead, RateLimiter, TimeLimiter) e métodos de fallback.
  - `ExternalService.java` — serviço simulado que responde aleatoriamente com sucesso ou erro e introduz atraso.
  - `DashboardController.java` — controller que fornece um dashboard simples para visualizar circuit breakers.
- `src/main/resources/application.yaml` — configuração das instâncias do Resilience4j e do Actuator.
- `src/main/resources/templates/dashboard.html` — frontend simples (Thymeleaf) para o dashboard de circuit breakers.

## Endpoints

Endpoints principais para demonstração:

- GET `/api/resilience/circuit-breaker`
  - Descrição: Chama o serviço externo protegido por um Circuit Breaker.
  - Exemplo curl:
    ```bash
    curl -v http://localhost:8080/api/resilience/circuit-breaker
    ```
  - Exemplo PowerShell:
    ```powershell
    Invoke-RestMethod -Uri http://localhost:8080/api/resilience/circuit-breaker -Method GET
    ```

- GET `/api/resilience/retry`
  - Descrição: Chama o serviço com política de Retry configurada.
  - Exemplo:
    ```bash
    curl http://localhost:8080/api/resilience/retry
    ```

- GET `/api/resilience/bulkhead`
  - Descrição: Chama o serviço protegido por Bulkhead (limita concorrência).
  - Exemplo:
    ```bash
    curl http://localhost:8080/api/resilience/bulkhead
    ```

- GET `/api/resilience/rate-limiter`
  - Descrição: Chama o serviço com Rate Limiter (limita taxa de requisições).
  - Exemplo:
    ```bash
    curl http://localhost:8080/api/resilience/rate-limiter
    ```

- GET `/api/resilience/time-limiter`
  - Descrição: Chama o serviço com Time Limiter (timeout) — retorna um `CompletableFuture`.
  - Exemplo:
    ```bash
    curl http://localhost:8080/api/resilience/time-limiter
    ```

Dashboard e informações de circuit breakers:

- GET `/dashboard` — página HTML do dashboard (Thymeleaf).
- GET `/dashboard/circuit-breakers` — endpoint JSON com o estado e métricas dos circuit breakers.

Actuator (monitoramento) — ver seção "Monitoramento" para endpoints expostos.

## Configuração dos padrões no `application.yaml`

Abaixo estão os trechos relevantes do `src/main/resources/application.yaml` organizados por seção, com cada propriedade em seu respectivo local para facilitar a visualização. Após cada trecho há uma breve explicação do significado de cada propriedade.

```yaml
resilience4j:
  circuitbreaker:
    instances:
      externalServiceCircuitBreaker:
        registerHealthIndicator: true
        slidingWindowType: COUNT_BASED
        slidingWindowSize: 10
        minimumNumberOfCalls: 5
        permittedNumberOfCallsInHalfOpenState: 3
        waitDurationInOpenState: 10000
        failureRateThreshold: 50
        eventConsumerBufferSize: 10
        automaticTransitionFromOpenToHalfOpenEnabled: true
```
Explicação (Circuit Breaker):
- `registerHealthIndicator`: registra um indicador de saúde para o Actuator.
- `slidingWindowType`: tipo de janela (COUNT_BASED ou TIME_BASED).
- `slidingWindowSize`: tamanho da janela (número de chamadas) para cálculo de métricas.
- `minimumNumberOfCalls`: mínimo de chamadas necessário para começar a calcular taxas.
- `permittedNumberOfCallsInHalfOpenState`: número de chamadas permitidas no estado HALF_OPEN.
- `waitDurationInOpenState`: tempo (ms) que o CB permanece aberto antes de tentar HALF_OPEN.
- `failureRateThreshold`: porcentagem de falhas que dispara a abertura do CB.
- `eventConsumerBufferSize`: tamanho do buffer para eventos (logs/metrics).
- `automaticTransitionFromOpenToHalfOpenEnabled`: permite transição automática de OPEN para HALF_OPEN após `waitDurationInOpenState`.

```yaml
resilience4j:
  retry:
    instances:
      externalServiceRetry:
        maxAttempts: 3
        waitDuration: 1000ms
        enable-exponential-backoff: true
        exponential-backoff-multiplier: 2
        retryExceptions: java.lang.RuntimeException
```
Explicação (Retry):
- `maxAttempts`: número máximo de tentativas (inclui a tentativa original).
- `waitDuration`: tempo de espera entre tentativas (ex.: `1000ms`).
- `enable-exponential-backoff` e `exponential-backoff-multiplier`: habilitam e configuram backoff exponencial.
- `retryExceptions`: lista de exceções que disparam retry.

```yaml
resilience4j:
  bulkhead:
    instances:
      externalServiceBulkhead:
        maxConcurrentCalls: 5
        maxWaitDuration: 500ms
```
Explicação (Bulkhead):
- `maxConcurrentCalls`: número máximo de chamadas concorrentes permitidas.
- `maxWaitDuration`: tempo máximo que uma chamada pode esperar para entrar no bulkhead.

```yaml
resilience4j:
  ratelimiter:
    instances:
      externalServiceRateLimiter:
        limitForPeriod: 5
        limitRefreshPeriod: 10s
        timeoutDuration: 0
```
Explicação (Rate Limiter):
- `limitForPeriod`: número máximo de chamadas permitidas por período.
- `limitRefreshPeriod`: duração do período (ex.: `10s`).
- `timeoutDuration`: tempo de espera para adquirir permissão (0 = sem espera).

```yaml
resilience4j:
  timelimiter:
    instances:
      externalServiceTimeLimiter:
        timeoutDuration: 1s
        cancelRunningFuture: true
```
Explicação (Time Limiter):
- `timeoutDuration`: tempo máximo permitido para a execução assíncrona (ex.: `1s`).
- `cancelRunningFuture`: se `true`, cancela a tarefa em execução quando ocorrer timeout.

```yaml
management:
  endpoints:
    web:
      exposure:
        include: "*"
  endpoint:
    health:
      show-details: always
  health:
    circuitbreakers:
      enabled: true
    ratelimiters:
      enabled: true
```
Explicação (Actuator / Management):
- `management.endpoints.web.exposure.include: "*"`: expõe todos os endpoints do Actuator (útil para demonstração).
- `management.endpoint.health.show-details: always`: exibe detalhes do health, inclusive status de circuit breakers.
- `management.health.circuitbreakers.enabled` e `management.health.ratelimiters.enabled`: habilitam integração de saúde para recursos de Resilience4j.

## Como Testar os endpoints

1. Inicie a aplicação (ver seção "Como executar").
2. Teste as chamadas individuais via curl ou PowerShell (exemplos acima).
3. Para observar comportamento do Circuit Breaker / Retry / Fallback:
   - Execute repetidas chamadas ao endpoint `/api/resilience/circuit-breaker` até que ocorram falhas suficientes para alcançar `failureRateThreshold`. Por exemplo, repetir em loop:

```bash
for i in {1..20}; do curl -sS http://localhost:8080/api/resilience/circuit-breaker; echo; sleep 0.2; done
```

(Em PowerShell, um exemplo simples para repetir 20 vezes):

```powershell
1..20 | ForEach-Object { Invoke-RestMethod -Uri http://localhost:8080/api/resilience/circuit-breaker; Start-Sleep -Milliseconds 200 }
```

Quando o Circuit Breaker abrir, as chamadas retornarão a resposta de fallback definida em `CircuitBreakerService#fallbackMethod`.

Para testar Time Limiter, chame `/api/resilience/time-limiter` repetidamente até que um tempo de execução acima de `timeoutDuration` ocorra — então o fallback de timeout será acionado.

Para testar Rate Limiter, faça várias requisições rápidas a `/api/resilience/rate-limiter` e observe quando as solicitações são rejeitadas.

Dica: o `ExternalService` é intencionalmente não determinístico (aleatório) para facilitar a demonstração de falhas.

## Monitoramento: Endpoints do Actuator disponíveis e Dashboard

Actuator exposto (por padrão neste projeto):

- `/actuator/health` — estado de saúde da aplicação (inclui circuit breakers quando habilitado).
- `/actuator/metrics` — métricas disponíveis.
- `/actuator/metrics/{metric.name}` — métrica específica.
- `/actuator/circuitbreakerevents`, `/actuator/retryevents`, `/actuator/ratelimiterevents` etc. — eventos do Resilience4j (se suportado por versão/depuração).

Dashboard custom:

- `/dashboard` — página HTML que carrega informações dos circuit breakers.
- `/dashboard/circuit-breakers` — endpoint JSON que fornece o estado (`OPEN`, `CLOSED`, `HALF_OPEN`) e métricas (failureRate, numberOfFailedCalls etc.) para cada circuit breaker registrado.

Observação: neste projeto o Actuator está configurado para expor todos os endpoints (`management.endpoints.web.exposure.include: "*"`), por isso você terá acesso direto aos endpoints acima sem autenticação. Em produção, proteja esses endpoints.

## Como executar a aplicação

Pré-requisitos:

- JDK 21 (conforme definido em `pom.xml`)
- Maven (ou use o wrapper incluido `mvnw.cmd`).

Executar em modo de desenvolvimento (Windows PowerShell):

```powershell
# Rodar com o Maven wrapper
mvnw.cmd spring-boot:run
```

Gerar o JAR e executar:

```powershell
mvnw.cmd clean package
java -jar target/spring-boot-resilience-0.0.1-SNAPSHOT.jar
```

A aplicação ficará disponível por padrão em `http://localhost:8080`.

## Dependências utilizadas (principais)

- Spring Boot Starter Web — servidor web e suporte REST.
- Spring Boot Starter Actuator — endpoints de monitoramento e métricas.
- Resilience4j (módulos):
  - `resilience4j-circuitbreaker`
  - `resilience4j-retry`
  - `resilience4j-bulkhead`
  - `resilience4j-ratelimiter`
  - `resilience4j-timelimiter`
  - `resilience4j-micrometer` (se métricas integradas ao Micrometer estiverem presentes)
- Thymeleaf — template engine para o dashboard HTML.

(Ver `pom.xml` para versões específicas e dependências completas.)

## Dicas rápidas / Troubleshooting

- Logs: observe o console para mensagens de log geradas por `CircuitBreakerService` quando fallbacks são acionados.
- Forçar fallback: chame repetidamente os endpoints até que o `failureRateThreshold` seja alcançado para o Circuit Breaker, ou provoque delays para acionar o TimeLimiter.
- Segurança: como o Actuator está totalmente exposto para fins de demonstração, evite usar essa mesma configuração em produção.

---
