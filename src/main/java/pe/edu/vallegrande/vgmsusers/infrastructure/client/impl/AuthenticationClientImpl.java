package pe.edu.vallegrande.vgmsusers.infrastructure.client.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import pe.edu.vallegrande.vgmsusers.infrastructure.client.AuthenticationClient;
import pe.edu.vallegrande.vgmsusers.infrastructure.dto.ApiResponse;
import pe.edu.vallegrande.vgmsusers.infrastructure.dto.request.CreateAccountRequestDto;
import pe.edu.vallegrande.vgmsusers.infrastructure.dto.request.UserCredentialRequest;
import pe.edu.vallegrande.vgmsusers.infrastructure.dto.response.CreateAccountResponse;
import reactor.core.publisher.Mono;
import reactor.util.retry.Retry;

import java.time.Duration;

/**
 * Implementación del cliente para comunicación con el microservicio de
 * autenticación
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class AuthenticationClientImpl implements AuthenticationClient {

     private final WebClient.Builder webClientBuilder;

     @Value("${external.ms-authentication.url}")
     private String authenticationServiceUrl;

     @Value("${external.ms-authentication.timeout:10000}")
     private int timeout;

     @Value("${external.ms-authentication.retryAttempts:3}")
     private int retryAttempts;

     @Override
     public Mono<ApiResponse<String>> registerUserInKeycloak(UserCredentialRequest request) {
          log.info("Registrando usuario en Keycloak: {}", request.getUsername());

          var createAccountRequest = new CreateAccountRequestDto(
                    request.getUserId(),
                    request.getFirstName(),
                    request.getLastName(),
                    request.getEmail(),
                    request.getOrganizationId(), // CORREGIDO: Usar organizationId real
                    request.getTemporaryPassword(),
                    request.getRoles().stream().map(role -> role.name()).toArray(String[]::new));

          return webClientBuilder.build()
                    .post()
                    .uri(authenticationServiceUrl + "/api/auth/accounts") // CORREGIDO: URL correcta
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(createAccountRequest) // CORREGIDO: DTO correcto
                    .retrieve()
                    .bodyToMono(new ParameterizedTypeReference<ApiResponse<String>>() {
                    })
                    .timeout(Duration.ofMillis(timeout))
                    .retryWhen(Retry.backoff(retryAttempts, Duration.ofMillis(500))
                              .doAfterRetry(
                                        retrySignal -> log.warn("Reintentando registro en Keycloak: {} (intento: {})",
                                                  request.getUsername(), retrySignal.totalRetries() + 1)))
                    .onErrorResume(error -> {
                         log.error("Error registrando usuario en Keycloak: {}", error.getMessage());
                         return Mono.just(ApiResponse.<String>builder()
                                   .success(false)
                                   .message("Error al registrar en servicio de autenticación: " + error.getMessage())
                                   .build());
                    })
                    .doOnSuccess(response -> {
                         if (response.isSuccess()) {
                              log.info("Usuario registrado exitosamente en Keycloak: {}", request.getUsername());
                         } else {
                              log.warn("No se pudo registrar el usuario en Keycloak: {}", response.getMessage());
                         }
                    });
     }

     @Override
     public Mono<ApiResponse<CreateAccountResponse>> createAccountWithFullResponse(UserCredentialRequest request) {
          log.info("🔧 Creando cuenta en Keycloak con respuesta completa: {}", request.getUsername());
          log.info("🔗 URL del servicio: {}/api/auth/accounts", authenticationServiceUrl);

          // Convertir UserCredentialRequest a CreateAccountRequest (SIN username)
          var createAccountRequest = new CreateAccountRequestDto(
                    request.getUserId(),
                    request.getFirstName(),
                    request.getLastName(),
                    request.getEmail(),
                    request.getOrganizationId(),
                    request.getTemporaryPassword(), // Puede ser null para que MS-AUTHENTICATION genere una
                    request.getRoles().stream().map(role -> role.name()).toArray(String[]::new));

          log.info("📤 Enviando request a MS-AUTHENTICATION: {}", createAccountRequest);

          return webClientBuilder.build()
                    .post()
                    .uri(authenticationServiceUrl + "/api/auth/accounts")
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(createAccountRequest)
                    .retrieve()
                    .bodyToMono(new ParameterizedTypeReference<ApiResponse<CreateAccountResponse>>() {
                    })
                    .timeout(Duration.ofMillis(timeout))
                    .retryWhen(Retry.backoff(retryAttempts, Duration.ofMillis(500))
                              .doAfterRetry(
                                        retrySignal -> log.warn("Reintentando creación en Keycloak: {} (intento: {})",
                                                  request.getUsername(), retrySignal.totalRetries() + 1)))
                    .doOnError(error -> log.error("❌ Error detallado llamando a MS-AUTHENTICATION: {}",
                              error.getMessage(), error))
                    .onErrorResume(error -> {
                         log.error("❌ Error creando cuenta en Keycloak: {}", error.getMessage());
                         return Mono.just(ApiResponse.<CreateAccountResponse>builder()
                                   .success(false)
                                   .message("MS-AUTHENTICATION no disponible")
                                   .build());
                    })
                    .doOnSuccess(response -> {
                         if (response.isSuccess()) {
                              log.info("✅ Cuenta creada exitosamente en Keycloak: {} con contraseña temporal",
                                        request.getUsername());
                         } else {
                              log.warn("⚠️ No se pudo crear la cuenta en Keycloak: {}", response.getMessage());
                         }
                    });
     }

     @Override
     public Mono<Boolean> isServiceAvailable() {
          return webClientBuilder.build()
                    .get()
                    .uri(authenticationServiceUrl + "/actuator/health")
                    .retrieve()
                    .bodyToMono(String.class)
                    .map(response -> true)
                    .timeout(Duration.ofMillis(2000))
                    .onErrorReturn(false);
     }
}
