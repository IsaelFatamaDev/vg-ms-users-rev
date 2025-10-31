package pe.edu.vallegrande.vgmsusers.infrastructure.util;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * Utilidad para extraer información de headers propagados por el Gateway
 *
 * ARQUITECTURA:
 * - Gateway valida JWT y extrae claims
 * - Gateway propaga información en headers:
 * * X-Keycloak-Sub: ID de usuario en Keycloak
 * * X-User-Roles: Roles del usuario (separados por comas)
 * * X-User-Email: Email del usuario
 * * X-Username: Username del usuario
 * * X-Organization-Id: ID de la organización
 * * X-Authenticated: Flag de autenticación
 *
 * Este microservicio confía en estos headers (red interna segura).
 */
@Slf4j
@Component
public class HeaderExtractorUtil {

     // Nombres de headers estándar del Gateway
     public static final String HEADER_KEYCLOAK_SUB = "X-Keycloak-Sub";
     public static final String HEADER_USER_ROLES = "X-User-Roles";
     public static final String HEADER_USER_EMAIL = "X-User-Email";
     public static final String HEADER_USERNAME = "X-Username";
     public static final String HEADER_ORGANIZATION_ID = "X-Organization-Id";
     public static final String HEADER_AUTHENTICATED = "X-Authenticated";

     // Constantes de roles
     public static final String ROLE_SUPER_ADMIN = "SUPER_ADMIN";
     public static final String ROLE_ADMIN = "ADMIN";
     public static final String ROLE_CLIENT = "CLIENT";
     public static final String ROLE_UNKNOWN = "UNKNOWN";

     /**
      * Extrae el Keycloak User ID (sub) del header
      */
     public String getKeycloakSub(ServerHttpRequest request) {
          return getHeaderValue(request, HEADER_KEYCLOAK_SUB);
     }

     /**
      * Extrae los roles del usuario del header
      *
      * @return Lista de roles (ej: ["ADMIN", "CLIENT"])
      */
     public List<String> getUserRoles(ServerHttpRequest request) {
          String rolesHeader = getHeaderValue(request, HEADER_USER_ROLES);

          if (rolesHeader == null || rolesHeader.isEmpty()) {
               log.warn("⚠️ Header {} no encontrado o vacío", HEADER_USER_ROLES);
               return Collections.emptyList();
          }

          return Arrays.asList(rolesHeader.split(","));
     }

     /**
      * Extrae el rol principal del usuario
      * Prioridad: SUPER_ADMIN > ADMIN > CLIENT
      */
     public String getPrimaryRole(ServerHttpRequest request) {
          List<String> roles = getUserRoles(request);

          if (roles.contains(ROLE_SUPER_ADMIN)) {
               return ROLE_SUPER_ADMIN;
          } else if (roles.contains(ROLE_ADMIN)) {
               return ROLE_ADMIN;
          } else if (roles.contains(ROLE_CLIENT)) {
               return ROLE_CLIENT;
          }

          log.warn("⚠️ No se encontró rol válido en headers. Roles: {}", roles);
          return ROLE_UNKNOWN;
     }

     /**
      * Extrae el email del usuario
      */
     public String getUserEmail(ServerHttpRequest request) {
          return getHeaderValue(request, HEADER_USER_EMAIL);
     }

     /**
      * Extrae el username del usuario
      */
     public String getUsername(ServerHttpRequest request) {
          return getHeaderValue(request, HEADER_USERNAME);
     }

     /**
      * Extrae el ID de la organización
      */
     public String getOrganizationId(ServerHttpRequest request) {
          return getHeaderValue(request, HEADER_ORGANIZATION_ID);
     }

     /**
      * Verifica si el usuario está autenticado
      */
     public boolean isAuthenticated(ServerHttpRequest request) {
          String authHeader = getHeaderValue(request, HEADER_AUTHENTICATED);
          return "true".equalsIgnoreCase(authHeader);
     }

     /**
      * Verifica si el usuario tiene un rol específico
      */
     public boolean hasRole(ServerHttpRequest request, String role) {
          List<String> userRoles = getUserRoles(request);
          boolean hasRole = userRoles.contains(role);

          log.debug("Verificando rol '{}' para usuario. Resultado: {}", role, hasRole);
          return hasRole;
     }

     /**
      * Verifica si el usuario es SUPER_ADMIN
      */
     public boolean isSuperAdmin(ServerHttpRequest request) {
          return hasRole(request, ROLE_SUPER_ADMIN);
     }

     /**
      * Verifica si el usuario es ADMIN
      */
     public boolean isAdmin(ServerHttpRequest request) {
          return hasRole(request, ROLE_ADMIN);
     }

     /**
      * Verifica si el usuario es CLIENT
      */
     public boolean isClient(ServerHttpRequest request) {
          return hasRole(request, ROLE_CLIENT);
     }

     /**
      * Método privado para extraer valor de un header
      */
     private String getHeaderValue(ServerHttpRequest request, String headerName) {
          List<String> headerValues = request.getHeaders().get(headerName);

          if (headerValues == null || headerValues.isEmpty()) {
               log.trace("Header '{}' no encontrado en la petición", headerName);
               return null;
          }

          String value = headerValues.get(0);
          log.trace("Header '{}' extraído: {}", headerName, value);
          return value;
     }

     /**
      * Log de todos los headers recibidos (útil para debugging)
      */
     public void logAllHeaders(ServerHttpRequest request) {
          log.debug("📨 Headers recibidos:");
          log.debug("  - Keycloak Sub: {}", getKeycloakSub(request));
          log.debug("  - User Roles: {}", getUserRoles(request));
          log.debug("  - User Email: {}", getUserEmail(request));
          log.debug("  - Username: {}", getUsername(request));
          log.debug("  - Organization ID: {}", getOrganizationId(request));
          log.debug("  - Authenticated: {}", isAuthenticated(request));
     }
}
