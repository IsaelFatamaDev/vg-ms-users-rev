package pe.edu.vallegrande.vgmsusers.infrastructure.dto.request;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import pe.edu.vallegrande.vgmsusers.domain.enums.RolesUsers;

import java.util.Set;

/**
 * DTO para enviar datos de credenciales al servicio de autenticación
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserCredentialRequest {

     private String username;
     private String email;
     private String firstName;
     private String lastName;
     private String temporaryPassword;
     private String organizationId; // AGREGADO
     private Set<RolesUsers> roles;
     private String userCode;
     private String userId;
}