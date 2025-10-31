package pe.edu.vallegrande.vgmsusers.infrastructure.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO para información básica de usuario (sin datos sensibles)
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserBasicInfoResponse {
     private String userCode;
     private String firstName;
     private String lastName;
     private String status;
     private String organizationId;
}