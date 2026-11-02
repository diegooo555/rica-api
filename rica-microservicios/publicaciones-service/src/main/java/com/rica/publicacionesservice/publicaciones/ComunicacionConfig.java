package com.rica.publicacionesservice.publicaciones;

import com.rica.publicacionesservice.grpc.InvestigadorGrpcGrpc.InvestigadorGrpcBlockingStub;
import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig;
import io.github.resilience4j.timelimiter.TimeLimiterConfig;
import org.springframework.cloud.circuitbreaker.resilience4j.Resilience4JCircuitBreakerFactory;
import org.springframework.cloud.circuitbreaker.resilience4j.Resilience4JConfigBuilder;
import org.springframework.cloud.client.circuitbreaker.Customizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.grpc.client.ImportGrpcClients;

import java.time.Duration;

/**
 * Cómo habla publicaciones-service con investigadores-service:
 * - el stub gRPC apunta al canal lógico "investigadores" (spring.grpc.client.channel.investigadores.target);
 * - el Circuit Breaker se abre si falla la mitad de las últimas 10 llamadas y espera 10 s antes de probar de nuevo.
 */
@Configuration
@ImportGrpcClients(target = "investigadores", types = InvestigadorGrpcBlockingStub.class)
public class ComunicacionConfig {

    @Bean
    public Customizer<Resilience4JCircuitBreakerFactory> circuitoInvestigadores() {
        return factory -> factory.configure(builder -> builder
                .circuitBreakerConfig(CircuitBreakerConfig.custom()
                        .slidingWindowSize(10)
                        .minimumNumberOfCalls(5)
                        .failureRateThreshold(50)
                        .waitDurationInOpenState(Duration.ofSeconds(10))
                        .permittedNumberOfCallsInHalfOpenState(2)
                        .build())
                .timeLimiterConfig(TimeLimiterConfig.custom()
                        .timeoutDuration(Duration.ofSeconds(3))
                        .build()), VerificadorInvestigadorGrpc.CIRCUITO);
    }
}
