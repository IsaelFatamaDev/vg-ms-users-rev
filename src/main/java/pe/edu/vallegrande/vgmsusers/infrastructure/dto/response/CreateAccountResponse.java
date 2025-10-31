package pe.edu.vallegrande.vgmsusers.infrastructure.dto.response;

import lombok.Builder;

/**
 * Respuesta de creación de cuenta desde MS-AUTHENTICATION
 */
@Builder
public record CreateAccountResponse(
          String userId,
          String username,
          String temporaryPassword,
          boolean accountEnabled,
          String message) {
}