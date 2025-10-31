package pe.edu.vallegrande.vgmsusers.application.service;

import pe.edu.vallegrande.vgmsusers.domain.model.User;
import pe.edu.vallegrande.vgmsusers.infrastructure.dto.ApiResponse;
import pe.edu.vallegrande.vgmsusers.infrastructure.dto.response.CreateAccountResponse;
import reactor.core.publisher.Mono;

/**
 * Servicio para la integración con el microservicio de autenticación
 */
public interface UserAuthIntegrationService {

     /**
      * Registra un usuario en el sistema de autenticación (Keycloak)
      *
      * @param user              Usuario creado en el microservicio de usuarios
      * @param temporaryPassword Contraseña temporal generada
      * @return ApiResponse con el resultado de la operación
      */
     Mono<ApiResponse<String>> registerUserInAuthService(User user, String temporaryPassword);

     /**
      * NUEVO: Registra un usuario en el sistema de autenticación sin contraseña
      * previa
      * MS-AUTHENTICATION generará la contraseña temporal automáticamente
      *
      * @param user Usuario creado en el microservicio de usuarios
      * @return CreateAccountResponse con username y contraseña temporal generados
      */
     Mono<CreateAccountResponse> registerUserWithAutoPassword(User user);

     /**
      * Genera un nombre de usuario para el usuario basado en su información personal
      *
      * @param firstName Nombre del usuario
      * @param lastName  Apellido del usuario
      * @return Nombre de usuario generado
      */
     String generateUsername(String firstName, String lastName);
}