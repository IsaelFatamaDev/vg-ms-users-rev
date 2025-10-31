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
 * DTO de respuesta para usuario con información completa de ubicación
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserWithLocationResponse {

     // Información básica del usuario
     private String id;
     private String userCode;
     private String firstName;
     private String lastName;
     private DocumentType documentType;
     private String documentNumber;
     private String email;
     private String phone;
     private String address;
     private Set<RolesUsers> roles;
     private UserStatus status;
     private LocalDateTime createdAt;
     private LocalDateTime updatedAt;

     // Información completa de la organización
     private OrganizationInfo organization;
     private ZoneInfo zone;
     private StreetInfo street;

     // Información de la caja de agua asignada
     private WaterBoxAssignmentInfo waterBoxAssignment;

     @Data
     @Builder
     @NoArgsConstructor
     @AllArgsConstructor
     public static class OrganizationInfo {
          private String organizationId;
          private String organizationCode;
          private String organizationName;
          private String legalRepresentative;
          private String address;
          private String phone;
          private String status;
          private String logo;
     }

     @Data
     @Builder
     @NoArgsConstructor
     @AllArgsConstructor
     public static class ZoneInfo {
          private String zoneId;
          private String zoneCode;
          private String zoneName;
          private String description;
          private String status;
     }

     @Data
     @Builder
     @NoArgsConstructor
     @AllArgsConstructor
     public static class StreetInfo {
          private String streetId;
          private String streetCode;
          private String streetName;
          private String streetType;
          private String status;
     }

     @Data
     @Builder
     @NoArgsConstructor
     @AllArgsConstructor
     public static class WaterBoxAssignmentInfo {
          private Long id;
          private Long waterBoxId;
          private String userId;
          private LocalDateTime startDate;
          private LocalDateTime endDate;
          private Double monthlyFee;
          private String status;
          private LocalDateTime createdAt;
          private Long transferId;
          private String boxCode;
          private String boxType;
     }
}
