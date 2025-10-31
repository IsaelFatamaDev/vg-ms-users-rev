package pe.edu.vallegrande.vgmsusers.infrastructure.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * DTO para información de roles disponibles
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RoleInfoResponse {
     private String name;
     private String description;
     private List<String> permissions;
}