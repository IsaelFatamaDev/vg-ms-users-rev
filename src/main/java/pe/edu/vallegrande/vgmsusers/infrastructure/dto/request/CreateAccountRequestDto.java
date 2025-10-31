package pe.edu.vallegrande.vgmsusers.infrastructure.dto.request;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO para enviar a MS-AUTHENTICATION endpoint /api/auth/accounts
 * NOTA: El username NO se envía porque MS-AUTHENTICATION lo genera
 * automáticamente
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class CreateAccountRequestDto {
     private String userId;
     // ELIMINADO: username se genera automáticamente en MS-AUTHENTICATION
     private String firstName;
     private String lastName;
     private String email;
     private String organizationId;
     private String temporaryPassword;
     private String[] roles;
}