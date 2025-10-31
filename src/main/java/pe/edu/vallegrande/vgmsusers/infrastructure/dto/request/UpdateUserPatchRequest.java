package pe.edu.vallegrande.vgmsusers.infrastructure.dto.request;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Size;

/**
 * DTO para actualización parcial de usuario (PATCH)
 * Solo permite actualizar campos específicos
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UpdateUserPatchRequest {

     @Email(message = "El formato del email no es válido")
     private String email;

     @Size(min = 9, max = 15, message = "El teléfono debe tener entre 9 y 15 caracteres")
     private String phone;

     @Size(max = 200, message = "La dirección no puede exceder los 200 caracteres")
     private String streetAddress;

     @Size(max = 200, message = "La dirección no puede exceder los 200 caracteres")
     private String address;

     private String streetId;

     private String zoneId;
}