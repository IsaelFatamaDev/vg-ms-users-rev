package pe.edu.vallegrande.vgmsusers.infrastructure.client;

import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.time.LocalDateTime;

@Component
@RequiredArgsConstructor
@Slf4j
public class InfrastructureClient {

    private final WebClient.Builder webClientBuilder;

    @Value("${external.ms-infrastructure.url}")
    private String infrastructureUrl;

    @Value("${external.ms-infrastructure.timeout:3000}")
    private long timeout;

    /**
     * Obtiene la asignación de caja de agua activa para un usuario
     */
    public Mono<WaterBoxAssignmentResponse> getActiveWaterBoxAssignmentByUserId(String userId) {
        log.info("[INFRASTRUCTURE-CLIENT] 🔍 Obteniendo asignación de caja de agua para usuario: {}", userId);

        return webClientBuilder.build()
                .get()
                .uri(infrastructureUrl + "/internal/users/{userId}/water-box-assignment", userId)
                .retrieve()
                .onStatus(HttpStatusCode::is4xxClientError, response -> {
                    log.warn("[INFRASTRUCTURE-CLIENT] ⚠️ Usuario {} no tiene caja de agua asignada", userId);
                    return Mono.empty();
                })
                .onStatus(HttpStatusCode::is5xxServerError, response -> {
                    log.error("[INFRASTRUCTURE-CLIENT] ❌ Error del servidor al obtener caja de agua para usuario {}",
                            userId);
                    return Mono.empty();
                })
                .bodyToMono(WaterBoxAssignmentResponse.class)
                .timeout(Duration.ofMillis(timeout))
                .doOnSuccess(response -> log.info("[INFRASTRUCTURE-CLIENT] ✅ Asignación de caja obtenida: BoxCode={}",
                        response != null ? response.getBoxCode() : "N/A"))
                .doOnError(error -> log.error("[INFRASTRUCTURE-CLIENT] ❌ Error obteniendo caja de agua: {}",
                        error.getMessage()))
                .onErrorResume(error -> {
                    log.warn("[INFRASTRUCTURE-CLIENT] ⚠️ No se pudo obtener la caja de agua, continuando sin ella");
                    return Mono.empty();
                });
    }

    /**
     * DTO para la respuesta de asignación de caja de agua
     */
    @Data
    public static class WaterBoxAssignmentResponse {
        private Long id;
        private Long waterBoxId;
        private String userId;
        private LocalDateTime startDate;
        private LocalDateTime endDate;
        private Double monthlyFee;
        private String status;
        private LocalDateTime createdAt;
        private Long transferId;
        private String boxCode;
        private String boxType;
    }
}
