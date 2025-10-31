package pe.edu.vallegrande.vgmsusers.infrastructure.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import pe.edu.vallegrande.vgmsusers.domain.enums.DocumentType;
import pe.edu.vallegrande.vgmsusers.domain.enums.RolesUsers;
import pe.edu.vallegrande.vgmsusers.domain.enums.UserStatus;

import java.time.LocalDateTime;
import java.util.Set;

/**
 * DTO de respuesta para operaciones con usuarios
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserResponse {

     private String id;
     private String userCode;
     private String firstName;
     private String lastName;
     private DocumentType documentType;
     private String documentNumber;
     private String email;
     private String phone;
     private String address;
     private String organizationId;
     private String streetId;
     private String zoneId;
     private Set<RolesUsers> roles;
     private UserStatus status;
     private LocalDateTime createdAt;
     private LocalDateTime updatedAt;

     /**
      * Verifica si el usuario tiene un rol específico
      */
     public boolean hasRole(RolesUsers role) {
          return roles != null && roles.contains(role);
     }

     /**
      * Verifica si el usuario tiene alguno de los roles especificados
      */
     public boolean hasAnyRole(RolesUsers... rolesToCheck) {
          if (roles == null || rolesToCheck == null) {
               return false;
          }
          for (RolesUsers role : rolesToCheck) {
               if (roles.contains(role)) {
                    return true;
               }
          }
          return false;
     }

     /**
      * Verifica si el usuario es administrador
      */
     public boolean isAdmin() {
          return hasRole(RolesUsers.ADMIN);
     }

     /**
      * Verifica si el usuario es super administrador
      */
     public boolean isSuperAdmin() {
          return hasRole(RolesUsers.SUPER_ADMIN);
     }

     /**
      * Verifica si el usuario es cliente
      */
     public boolean isClient() {
          return hasRole(RolesUsers.CLIENT);
     }

     /**
      * Verifica si el usuario es operario
      */
     public boolean isOperator() {
          return hasRole(RolesUsers.OPERATOR);
     }
}