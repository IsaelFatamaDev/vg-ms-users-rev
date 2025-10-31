package pe.edu.vallegrande.vgmsusers.infrastructure.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import pe.edu.vallegrande.vgmsusers.domain.enums.RolesUsers;
import pe.edu.vallegrande.vgmsusers.domain.enums.UserStatus;
import com.fasterxml.jackson.annotation.JsonFormat;

import java.time.LocalDateTime;
import java.util.Set;

/**
 * Respuesta completa de usuario con información de organización, zona y calle
 * Usado en endpoints internos para proporcionar información completa
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CompleteUserResponse {

     private String id;
     private String userCode;
     private String firstName;
     private String lastName;
     private String documentType;
     private String documentNumber;
     private String email;
     private String phone;
     private String address;
     private Set<RolesUsers> roles;
     private UserStatus status;

     @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss.SSS")
     private LocalDateTime createdAt;

     @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss.SSS")
     private LocalDateTime updatedAt;

     // Información completa de relaciones
     private Object organization; // Información completa de la organización
     private Object zone; // Información completa de la zona
     private Object street; // Información completa de la calle
}