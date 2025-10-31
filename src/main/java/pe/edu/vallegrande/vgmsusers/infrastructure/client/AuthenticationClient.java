package pe.edu.vallegrande.vgmsusers.infrastructure.client;

import pe.edu.vallegrande.vgmsusers.infrastructure.dto.ApiResponse;
import pe.edu.vallegrande.vgmsusers.infrastructure.dto.request.UserCredentialRequest;
import pe.edu.vallegrande.vgmsusers.infrastructure.dto.response.CreateAccountResponse;
import reactor.core.publisher.Mono;

/**
 * Cliente para comunicación con el microservicio de autenticación
 */
public interface AuthenticationClient {

     /**
      * Registra un nuevo usuario en el sistema de autenticación (Keycloak)
      *
      * @param request Datos del usuario para crear en Keycloak
      * @return ApiResponse con el resultado de la operación
      */
     Mono<ApiResponse<String>> registerUserInKeycloak(UserCredentialRequest request);

     /**
      * NUEVO: Crea cuenta en Keycloak y devuelve respuesta completa con
      * contraseña temporal
      *
      * @param request Datos del usuario para crear en Keycloak
      * @return ApiResponse con CreateAccountResponse que incluye la contraseña
      *         temporal
      */
     Mono<ApiResponse<CreateAccountResponse>> createAccountWithFullResponse(UserCredentialRequest request);

     /**
      * Verifica si el servicio de autenticación está disponible
      *
      * @return true si está disponible, false en caso contrario
      */
     Mono<Boolean> isServiceAvailable();
}