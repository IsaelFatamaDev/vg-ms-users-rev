package pe.edu.vallegrande.vgmsusers.infrastructure.dto.request;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import pe.edu.vallegrande.vgmsusers.domain.enums.RolesUsers;

import java.util.Set;

/**
 * DTO para actualizar un usuario existente
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UpdateUserRequest {

     private String firstName;
     private String lastName;
     private String email;
     private String phone;
     private String address;
     private Set<RolesUsers> roles;
}