package pe.edu.vallegrande.vgmsusers.infrastructure.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.web.server.SecurityWebFilterChain;

/**
 * Configuración de Seguridad SIMPLIFICADA para MS-USERS
 *
 * ARQUITECTURA:
 * - Gateway valida JWT de Keycloak
 * - Gateway propaga headers: X-User-Id, X-User-Roles, X-Organization-Id
 * - MS-Users confía en headers del Gateway (red interna segura)
 * - NO se conecta directamente a Keycloak
 * - NO valida JWT (responsabilidad del Gateway)
 *
 * SEGURIDAD POR CAPAS:
 * 1. Gateway: Valida JWT + Extrae claims
 * 2. Red Interna: Solo accesible desde Gateway
 * 3. MS-Users: Lee headers propagados
 */
@Slf4j
@Configuration
@EnableWebFluxSecurity
public class SecurityConfig {

     /**
      * Configuración de seguridad simplificada
      *
      * TODAS las rutas son accesibles porque:
      * 1. El Gateway ya validó el JWT
      * 2. La red es interna y segura
      * 3. Los headers (X-User-Roles, X-User-Id, etc.) son confiables
      *
      * La autorización específica se maneja a nivel de servicio
      * leyendo los headers propagados por el Gateway.
      */
     @Bean
     public SecurityWebFilterChain securityWebFilterChain(ServerHttpSecurity http) {
          log.info("🔧 Configurando SecurityWebFilterChain SIMPLIFICADO - Confiando en headers del Gateway");

          return http
                    .csrf(csrf -> csrf.disable())
                    .authorizeExchange(exchanges -> exchanges
                              // ✅ Rutas públicas - Actuator
                              .pathMatchers("/actuator/health").permitAll()
                              .pathMatchers("/actuator/**").permitAll()

                              // ✅ Rutas públicas - Swagger/OpenAPI
                              .pathMatchers("/v3/api-docs/**").permitAll()
                              .pathMatchers("/swagger-ui/**").permitAll()
                              .pathMatchers("/swagger-ui.html").permitAll()
                              .pathMatchers("/webjars/**").permitAll()
                              .pathMatchers("/swagger-resources/**").permitAll()
                              .pathMatchers("/configuration/**").permitAll()

                              // ✅ Endpoints internos (comunicación entre microservicios)
                              .pathMatchers("/internal/**").permitAll()
                              .pathMatchers("/api/internal/**").permitAll()

                              // ✅ Endpoints para MS-Authentication
                              .pathMatchers("/api/users/email/**").permitAll()
                              .pathMatchers("/api/users/username/**").permitAll()
                              .pathMatchers("/api/common/user/username/**").permitAll()

                              // ✅ Configuración inicial
                              .pathMatchers("/api/common/setup/**").permitAll()

                              // ✅ TODO LO DEMÁS: Gateway ya validó, confiar en headers
                              .anyExchange().permitAll())
                    .build();
     }
}
