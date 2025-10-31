package pe.edu.vallegrande.vgmsusers.infrastructure.dto.request;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import pe.edu.vallegrande.vgmsusers.domain.enums.DocumentType;
import pe.edu.vallegrande.vgmsusers.domain.enums.RolesUsers;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.Set;

/**
 * DTO para crear un nuevo usuario
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CreateUserRequest {

     // Campos opcionales - se pueden obtener de RENIEC automáticamente
     @Size(min = 2, max = 50, message = "El nombre debe tener entre 2 y 50 caracteres")
     private String firstName;

     @Size(min = 2, max = 50, message = "El apellido debe tener entre 2 y 50 caracteres")
     private String lastName;

     @NotNull(message = "El tipo de documento es obligatorio")
     private DocumentType documentType;

     @NotBlank(message = "El número de documento es obligatorio")
     @Size(min = 8, max = 12, message = "El número de documento debe tener entre 8 y 12 caracteres")
     private String documentNumber;

     @Email(message = "Debe proporcionar un email válido")
     private String email; // Campo opcional - se puede generar automáticamente

     @Size(min = 9, max = 15, message = "El teléfono debe tener entre 9 y 15 caracteres")
     private String phone;

     @Size(max = 200, message = "La dirección no puede exceder los 200 caracteres")
     private String address;

     @NotBlank(message = "El ID de organización es obligatorio")
     private String organizationId;

     private String streetId;

     private String zoneId;

     // Roles opcionales - se asignan automáticamente según el endpoint
     @Size(min = 1, message = "Debe asignar al menos un rol")
     private Set<RolesUsers> roles;
}