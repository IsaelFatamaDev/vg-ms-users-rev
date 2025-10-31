package pe.edu.vallegrande.vgmsusers.infrastructure.rest.migration;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;
import pe.edu.vallegrande.vgmsusers.infrastructure.dto.ApiResponse;
import pe.edu.vallegrande.vgmsusers.infrastructure.migration.RoleMigrationService;
import reactor.core.publisher.Mono;

/**
 * Controlador REST para ejecutar migraciones de datos
 */
@RestController
@RequestMapping("/api/migration")
@RequiredArgsConstructor
@Slf4j
public class MigrationRest {

     private final RoleMigrationService roleMigrationService;

     /**
      * Ejecutar migración de role a roles
      */
     @PostMapping("/migrate-roles")
     public Mono<ApiResponse<String>> migrateRoles() {
          log.info("[MIGRATION] Iniciando migración de roles");

          return roleMigrationService.migrateRoleToRoles()
                    .map(count -> ApiResponse.<String>builder()
                              .success(true)
                              .message("Migración completada exitosamente")
                              .data("Documentos migrados: " + count)
                              .build())
                    .onErrorReturn(ApiResponse.<String>builder()
                              .success(false)
                              .message("Error durante la migración")
                              .data(null)
                              .build());
     }

     /**
      * Verificar cuántos documentos necesitan migración
      */
     @GetMapping("/check-migration")
     public Mono<ApiResponse<String>> checkMigration() {
          log.info("[MIGRATION] Verificando documentos que necesitan migración");

          return roleMigrationService.countDocumentsNeedingMigration()
                    .map(count -> ApiResponse.<String>builder()
                              .success(true)
                              .message("Verificación completada")
                              .data("Documentos que necesitan migración: " + count)
                              .build())
                    .onErrorReturn(ApiResponse.<String>builder()
                              .success(false)
                              .message("Error durante la verificación")
                              .data(null)
                              .build());
     }
}