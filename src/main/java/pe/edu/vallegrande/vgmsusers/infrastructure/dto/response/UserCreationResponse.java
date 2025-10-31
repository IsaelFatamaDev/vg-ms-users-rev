package pe.edu.vallegrande.vgmsusers.infrastructure.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO de respuesta para la creación de usuarios que incluye las credenciales de
 * acceso
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserCreationResponse {

     private UserResponse userInfo;
     private String username;
     private String temporaryPassword;
     private String message;
     private boolean requiresPasswordChange;

     /**
      * Crea una respuesta exitosa con credenciales
      */
     public static UserCreationResponse success(UserResponse userInfo, String username, String temporaryPassword) {
          return UserCreationResponse.builder()
                    .userInfo(userInfo)
                    .username(username)
                    .temporaryPassword(temporaryPassword)
                    .message("Usuario creado exitosamente. Credenciales generadas para primer acceso.")
                    .requiresPasswordChange(true)
                    .build();
     }

     /**
      * Crea una respuesta de error
      */
     public static UserCreationResponse error(String message) {
          return UserCreationResponse.builder()
                    .message(message)
                    .requiresPasswordChange(false)
                    .build();
     }
}