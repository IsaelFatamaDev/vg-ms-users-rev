package pe.edu.vallegrande.vgmsusers.infrastructure.rest.client;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import pe.edu.vallegrande.vgmsusers.application.service.UserService;
import pe.edu.vallegrande.vgmsusers.infrastructure.dto.request.UpdateUserRequest;
import pe.edu.vallegrande.vgmsusers.infrastructure.dto.response.UserResponse;
import pe.edu.vallegrande.vgmsusers.infrastructure.dto.ApiResponse;
import pe.edu.vallegrande.vgmsusers.infrastructure.exception.ForbiddenException;
import pe.edu.vallegrande.vgmsusers.infrastructure.exception.NotFoundException;
import pe.edu.vallegrande.vgmsusers.infrastructure.exception.ValidationException;
import pe.edu.vallegrande.vgmsusers.infrastructure.util.HeaderExtractorUtil;
import reactor.core.publisher.Mono;

import jakarta.validation.Valid;

/**
 * REST Controller para funciones de CLIENT
 * Solo accesible para usuarios con rol CLIENT
 * Permite gestionar su propio perfil de usuario
 */
@RestController
@RequestMapping("/api/client")
@RequiredArgsConstructor
@Slf4j
@PreAuthorize("hasRole('CLIENT')")
public class ClientRest {

     private final UserService userService;
     private final HeaderExtractorUtil headerExtractor;

     /**
      * Obtener mi perfil de usuario
      * GET /api/client/profile
      */
     @GetMapping("/profile")
     public Mono<ApiResponse<UserResponse>> getMyProfile(ServerHttpRequest httpRequest) {

          String clientUserId = headerExtractor.getKeycloakSub(httpRequest);

          log.info("[CLIENT] Obteniendo perfil del usuario: {}", clientUserId);

          return userService.getUserById(clientUserId)
                    .flatMap(response -> {
                         if (response.isSuccess()) {
                              return Mono.just(ApiResponse.success("Perfil obtenido exitosamente", response.getData()));
                         } else {
                              return Mono.error(new NotFoundException("Usuario no encontrado"));
                         }
                    });
     }

     /**
      * Obtener mi perfil por código de usuario
      * GET /api/client/profile/code/{userCode}
      */
     @GetMapping("/profile/code/{userCode}")
     public Mono<ApiResponse<UserResponse>> getMyProfileByCode(
               @PathVariable String userCode,
               ServerHttpRequest httpRequest) {

          log.info("[CLIENT] Obteniendo perfil por código: {}", userCode);

          return userService.getUserByCode(userCode)
                    .flatMap(response -> {
                         if (response.isSuccess()) {
                              return Mono.just(ApiResponse.success("Perfil obtenido exitosamente", response.getData()));
                         } else {
                              return Mono.error(new NotFoundException("Usuario no encontrado"));
                         }
                    });
     }

     /**
      * Actualizar mi perfil (solo datos permitidos)
      * PUT /api/client/profile
      */
     @PutMapping("/profile")
     public Mono<ApiResponse<UserResponse>> updateMyProfile(
               @Valid @RequestBody UpdateUserRequest request,
               ServerHttpRequest httpRequest) {

          String clientUserId = headerExtractor.getKeycloakSub(httpRequest);

          log.info("[CLIENT] Actualizando perfil del usuario: {}", clientUserId);

          if (request.getRoles() != null) {
               throw new ForbiddenException("Los clientes no pueden modificar sus roles");
          }

          return userService.updateUser(clientUserId, request)
                    .flatMap(response -> {
                         if (response.isSuccess()) {
                              return Mono.just(
                                        ApiResponse.success("Perfil actualizado exitosamente", response.getData()));
                         } else {
                              return Mono.error(
                                        new ValidationException("Error actualizando perfil: " + response.getMessage()));
                         }
                    });
     }

     /**
      * Cambiar mi estado (limitado a ciertos estados)
      * PATCH /api/client/profile/status
      */
     @PatchMapping("/profile/status")
     public Mono<ApiResponse<String>> requestStatusChange(
               @RequestParam String statusChangeReason,
               ServerHttpRequest httpRequest) {

          String clientUserId = headerExtractor.getKeycloakSub(httpRequest);

          log.info("[CLIENT] Solicitando cambio de estado del usuario: {} - Razón: {}", clientUserId,
                    statusChangeReason);

          // Los clientes no pueden cambiar directamente su estado, solo solicitar cambios
          // TODO: Implementar sistema de solicitudes de cambio de estado

          return Mono.just(ApiResponse.success(
                    "Solicitud de cambio de estado enviada. Será procesada por un administrador.",
                    "Solicitud registrada con razón: " + statusChangeReason));
     }

     /**
      * Ver mi código de usuario
      * GET /api/client/user-code
      */
     @GetMapping("/user-code")
     public Mono<ApiResponse<String>> getMyUserCode(ServerHttpRequest httpRequest) {

          String clientUserId = headerExtractor.getKeycloakSub(httpRequest);

          return userService.getUserById(clientUserId)
                    .flatMap(response -> {
                         if (response.isSuccess()) {
                              return Mono.just(ApiResponse.success("Código de usuario obtenido exitosamente",
                                        response.getData().getUserCode()));
                         } else {
                              return Mono.error(new NotFoundException("Usuario no encontrado"));
                         }
                    });
     }
}
