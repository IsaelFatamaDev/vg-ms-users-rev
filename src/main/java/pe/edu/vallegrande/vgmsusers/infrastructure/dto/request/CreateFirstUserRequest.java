package pe.edu.vallegrande.vgmsusers.infrastructure.dto.request;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import pe.edu.vallegrande.vgmsusers.domain.enums.DocumentType;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * DTO para crear el primer usuario del sistema (SUPER_ADMIN)
 * Este endpoint es público para configuración inicial
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CreateFirstUserRequest {

     @NotBlank(message = "El nombre es obligatorio")
     @Size(min = 2, max = 50, message = "El nombre debe tener entre 2 y 50 caracteres")
     private String firstName;

     @NotBlank(message = "El apellido es obligatorio")
     @Size(min = 2, max = 50, message = "El apellido debe tener entre 2 y 50 caracteres")
     private String lastName;

     @NotNull(message = "El tipo de documento es obligatorio")
     private DocumentType documentType;

     @NotBlank(message = "El número de documento es obligatorio")
     @Size(min = 8, max = 12, message = "El número de documento debe tener entre 8 y 12 caracteres")
     private String documentNumber;

     @Email(message = "Debe proporcionar un email válido")
     @NotBlank(message = "El email es obligatorio")
     private String email;

     @Size(min = 9, max = 15, message = "El teléfono debe tener entre 9 y 15 caracteres")
     private String phone;

     @Size(max = 200, message = "La dirección no puede exceder los 200 caracteres")
     private String address;

     @NotBlank(message = "El ID de organización es obligatorio")
     private String organizationId;
}