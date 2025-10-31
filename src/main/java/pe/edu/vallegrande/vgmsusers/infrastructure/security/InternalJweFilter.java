package pe.edu.vallegrande.vgmsusers.infrastructure.security;

/*
// JWE FILTER COMMENTED OUT - NO LONGER NEEDED FOR INTERNAL APIS

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.ReactiveSecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;

import java.util.Collections;

/**
 * Filtro para validar tokens JWE en endpoints /internal/
 * Solo permite acceso con token JWE válido para comunicación entre
 * microservicios
 *
 * COMENTADO: Ya no se usa JWE para APIs internas
 */
/*
@Component
@RequiredArgsConstructor
@Slf4j
public class InternalJweFilter implements WebFilter {

     private final InternalJweService jweService;

     @Override
     public @NonNull Mono<Void> filter(@NonNull ServerWebExchange exchange, @NonNull WebFilterChain chain) {
          String path = exchange.getRequest().getPath().value();

          // Solo aplicar filtro a endpoints /internal/
          if (!path.startsWith("/internal/")) {
               return chain.filter(exchange);
          }

          log.debug("🔐 Validando acceso JWE para endpoint interno: {}", path);

          // Obtener token del header
          String authHeader = exchange.getRequest().getHeaders().getFirst("Authorization");

          if (authHeader == null || !authHeader.startsWith("Bearer ")) {
               log.warn("❌ Token JWE faltante en endpoint interno: {}", path);
               return unauthorized(exchange, "Token JWE requerido para endpoints internos");
          }

          String token = authHeader.substring(7); // Remover "Bearer "

          try {
               // Validar token JWE
               jweService.validateInternalToken(token);
               log.debug("✅ Token JWE válido para endpoint: {}", path);

               // Crear autenticación para el contexto de seguridad
               // Esto evita que el filtro JWT intente procesar el token
               var authentication = new UsernamePasswordAuthenticationToken(
                         "internal-service",
                         null,
                         Collections.singletonList(new SimpleGrantedAuthority("ROLE_INTERNAL_SERVICE")));

               // Establecer autenticación en el contexto de seguridad y continuar
               return chain.filter(exchange)
                         .contextWrite(ReactiveSecurityContextHolder.withAuthentication(authentication));

          } catch (Exception e) {
               log.error("❌ Token JWE inválido para endpoint {}: {}", path, e.getMessage());
               return unauthorized(exchange, "Token JWE inválido: " + e.getMessage());
          }
     }

     /**
      * Respuesta de no autorizado
      */
/*
     private Mono<Void> unauthorized(ServerWebExchange exchange, String message) {
          exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
          exchange.getResponse().getHeaders().add("Content-Type", "application/json");

          String body = String.format(
                    "{\"success\":false,\"message\":\"%s\",\"data\":null}",
                    message);

          var buffer = exchange.getResponse().bufferFactory().wrap(body.getBytes());
          return exchange.getResponse().writeWith(Mono.just(buffer));
     }
}
*/

// Empty class to avoid compilation errors during transition
public class InternalJweFilter {
     // This filter has been disabled - JWE no longer needed for internal APIs
}