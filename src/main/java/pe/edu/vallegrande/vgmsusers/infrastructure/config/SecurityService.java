package pe.edu.vallegrande.vgmsusers.infrastructure.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.util.List;

/**
 * Servicio de seguridad SIMPLIFICADO para MS-USERS
 * - Extrae información de headers HTTP enviados por el Gateway
 * - NO maneja JWT directamente (eso lo hace el Gateway)
 */
@Slf4j
@Service
public class SecurityService {

     // Headers que el Gateway envía con información del usuario
     public static final String HEADER_USER_ID = "X-User-Id";
     public static final String HEADER_ORGANIZATION_ID = "X-Organization-Id";
     public static final String HEADER_USER_EMAIL = "X-User-Email";
     public static final String HEADER_USER_ROLE = "X-User-Role";
     public static final String HEADER_USERNAME = "X-Username";

     /**
      * Obtiene el ID del usuario actual desde headers del Gateway
      */
     public Mono<String> getCurrentUserId(ServerWebExchange exchange) {
          String userId = exchange.getRequest().getHeaders().getFirst(HEADER_USER_ID);
          return Mono.justOrEmpty(userId);
     }

     /**
      * Obtiene el ID de la organización del usuario actual
      */
     public Mono<String> getCurrentOrganizationId(ServerWebExchange exchange) {
          String organizationId = exchange.getRequest().getHeaders().getFirst(HEADER_ORGANIZATION_ID);
          return Mono.justOrEmpty(organizationId);
     }

     /**
      * Obtiene el email del usuario actual
      */
     public Mono<String> getCurrentUserEmail(ServerWebExchange exchange) {
          String email = exchange.getRequest().getHeaders().getFirst(HEADER_USER_EMAIL);
          return Mono.justOrEmpty(email);
     }

     /**
      * Obtiene el username del usuario actual
      */
     public Mono<String> getCurrentUsername(ServerWebExchange exchange) {
          String username = exchange.getRequest().getHeaders().getFirst(HEADER_USERNAME);
          return Mono.justOrEmpty(username);
     }

     /**
      * Obtiene el rol del usuario actual
      */
     public Mono<String> getCurrentUserRole(ServerWebExchange exchange) {
          String role = exchange.getRequest().getHeaders().getFirst(HEADER_USER_ROLE);
          return Mono.justOrEmpty(role);
     }

     /**
      * Verifica si el usuario actual tiene un rol específico
      */
     public Mono<Boolean> hasRole(ServerWebExchange exchange, String role) {
          return getCurrentUserRole(exchange)
                    .map(userRole -> userRole.equals(role))
                    .defaultIfEmpty(false);
     }

     /**
      * Verifica si el usuario actual es SUPER_ADMIN
      */
     public Mono<Boolean> isSuperAdmin(ServerWebExchange exchange) {
          return hasRole(exchange, "SUPER_ADMIN");
     }

     /**
      * Verifica si el usuario actual es ADMIN
      */
     public Mono<Boolean> isAdmin(ServerWebExchange exchange) {
          return hasRole(exchange, "ADMIN");
     }

     /**
      * Verifica si el usuario actual es CLIENT
      */
     public Mono<Boolean> isClient(ServerWebExchange exchange) {
          return hasRole(exchange, "CLIENT");
     }

     /**
      * Verifica si el usuario puede gestionar otro usuario
      * - SUPER_ADMIN: puede gestionar ADMIN y CLIENT
      * - ADMIN: puede gestionar CLIENT de su organización
      * - CLIENT: solo puede gestionar su propio perfil
      */
     public Mono<Boolean> canManageUser(ServerWebExchange exchange, String targetUserId, String targetUserRole,
               String targetOrganizationId) {
          return Mono.zip(
                    getCurrentUserId(exchange),
                    getCurrentUserRole(exchange),
                    getCurrentOrganizationId(exchange))
                    .map(tuple -> {
                         String currentUserId = tuple.getT1();
                         String currentRole = tuple.getT2();
                         String currentOrgId = tuple.getT3();

                         if ("SUPER_ADMIN".equals(currentRole)) {
                              return List.of("ADMIN", "CLIENT").contains(targetUserRole);
                         }

                         if ("ADMIN".equals(currentRole)) {
                              return "CLIENT".equals(targetUserRole) &&
                                        currentOrgId != null &&
                                        currentOrgId.equals(targetOrganizationId);
                         }

                         if ("CLIENT".equals(currentRole)) {
                              return currentUserId != null && currentUserId.equals(targetUserId);
                         }

                         return false;
                    })
                    .defaultIfEmpty(false);
     }

     /**
      * Verifica si el usuario pertenece a la misma organización que el recurso
      */
     public Mono<Boolean> belongsToOrganization(ServerWebExchange exchange, String organizationId) {
          return getCurrentOrganizationId(exchange)
                    .map(currentOrgId -> currentOrgId != null && currentOrgId.equals(organizationId))
                    .defaultIfEmpty(false);
     }

     /**
      * Verifica si el usuario es el propietario del recurso
      */
     public Mono<Boolean> isResourceOwner(ServerWebExchange exchange, String resourceUserId) {
          return getCurrentUserId(exchange)
                    .map(currentUserId -> currentUserId != null && currentUserId.equals(resourceUserId))
                    .defaultIfEmpty(false);
     }

     /**
      * Métodos simplificados que reciben directamente el organizationId
      * Para uso en controllers donde ya se tiene el ID de organización
      */

     /**
      * Obtiene el ID de organización actual o usa el proporcionado
      */
     public Mono<String> getOrganizationId(ServerWebExchange exchange, String providedOrgId) {
          if (providedOrgId != null && !providedOrgId.trim().isEmpty()) {
               return Mono.just(providedOrgId);
          }
          return getCurrentOrganizationId(exchange);
     }
}