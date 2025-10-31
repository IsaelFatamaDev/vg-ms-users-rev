package pe.edu.vallegrande.vgmsusers.infrastructure.rest.admin;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ServerWebExchange;
import pe.edu.vallegrande.vgmsusers.application.service.UserService;
import pe.edu.vallegrande.vgmsusers.application.service.UserCodeService;
import pe.edu.vallegrande.vgmsusers.infrastructure.config.SecurityService;
import pe.edu.vallegrande.vgmsusers.infrastructure.client.OrganizationClient;
import pe.edu.vallegrande.vgmsusers.infrastructure.client.ReniecClient;
import pe.edu.vallegrande.vgmsusers.infrastructure.dto.request.CreateUserRequest;
import pe.edu.vallegrande.vgmsusers.infrastructure.dto.request.UpdateUserPatchRequest;
import pe.edu.vallegrande.vgmsusers.infrastructure.dto.response.UserResponse;
import pe.edu.vallegrande.vgmsusers.infrastructure.dto.response.UserWithLocationResponse;
import pe.edu.vallegrande.vgmsusers.infrastructure.dto.response.UserCreationResponse;
import pe.edu.vallegrande.vgmsusers.infrastructure.dto.ApiResponse;
import pe.edu.vallegrande.vgmsusers.infrastructure.exception.ForbiddenException;
import pe.edu.vallegrande.vgmsusers.infrastructure.exception.NotFoundException;
import pe.edu.vallegrande.vgmsusers.infrastructure.exception.ValidationException;
import pe.edu.vallegrande.vgmsusers.domain.enums.RolesUsers;
import pe.edu.vallegrande.vgmsusers.domain.enums.UserStatus;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import jakarta.validation.Valid;
import java.util.List;
import java.util.Set;

/**
 * REST Controller para funciones de ADMIN
 * Solo accesible para usuarios con rol ADMIN
 * Permite gestionar usuarios CLIENT de su propia organización
 */
@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
@Slf4j
public class AdminRest {

     private final UserService userService;
     private final UserCodeService userCodeService;
     private final SecurityService securityService;
     private final OrganizationClient organizationClient;
     private final ReniecClient reniecClient;

     /**
      * Crear clientes (ADMIN solo puede crear CLIENT)
      * POST /api/admin/clients
      * Siempre asigna rol CLIENT por defecto
      * Usa datos de RENIEC para nombres y genera username inteligente
      * TOLERANTE: Si RENIEC falla, usa datos manuales del request
      */
     @PostMapping("/clients")
     public Mono<ApiResponse<UserCreationResponse>> createClient(@Valid @RequestBody CreateUserRequest request) {

          log.info("[ADMIN] Creando cliente para organización: {}", request.getOrganizationId());

          request.setRoles(Set.of(RolesUsers.CLIENT));
          log.info("[ADMIN] Asignando rol CLIENT por defecto");

          if (request.getRoles() == null || !request.getRoles().contains(RolesUsers.CLIENT) ||
                    request.getRoles().size() != 1) {
               throw new ValidationException("Los administradores solo pueden crear usuarios con rol CLIENT");
          }

          // Intentar obtener datos de RENIEC, pero si falla, usar datos manuales
          return reniecClient.getPersonalDataByDni(request.getDocumentNumber())
                    .doOnNext(reniecData -> {
                         log.info("[ADMIN] ✅ Datos RENIEC obtenidos: {}", reniecData);

                         // Sobrescribir con datos de RENIEC
                         request.setFirstName(reniecData.getFirstName());
                         request.setLastName(reniecData.getFirstLastName() +
                                   (reniecData.getSecondLastName() != null
                                             && !reniecData.getSecondLastName().trim().isEmpty()
                                                       ? " " + reniecData.getSecondLastName()
                                                       : ""));

                         // Generar username inteligente
                         String generatedUsername = generateIntelligentUsername(
                                   reniecData.getFirstName(),
                                   reniecData.getFirstLastName(),
                                   reniecData.getSecondLastName());
                         log.info("[ADMIN] 📧 Username generado desde RENIEC: {}", generatedUsername);

                         // Email es obligatorio para Keycloak - usar username generado si no se
                         // proporciona
                         if (request.getEmail() == null || request.getEmail().trim().isEmpty()) {
                              request.setEmail(generatedUsername);
                              log.info("[ADMIN] 📧 Email no proporcionado, usando username como email: {}",
                                        generatedUsername);
                         }
                    })
                    .onErrorResume(error -> {
                         log.warn("[ADMIN] ⚠️ Error validando DNI en RENIEC (continuando con datos manuales): {}",
                                   error.getMessage());

                         // Validar que firstName y lastName estén presentes si RENIEC falla
                         if (request.getFirstName() == null || request.getFirstName().trim().isEmpty() ||
                                   request.getLastName() == null || request.getLastName().trim().isEmpty()) {
                              return Mono.error(new ValidationException(
                                        "RENIEC no disponible. Debe proporcionar firstName y lastName manualmente."));
                         }

                         log.info("[ADMIN] 📝 Usando datos manuales: {} {}",
                                   request.getFirstName(), request.getLastName());

                         // Generar username con datos manuales
                         String generatedUsername = generateIntelligentUsername(
                                   request.getFirstName(),
                                   request.getLastName(),
                                   null);
                         log.info("[ADMIN] 📧 Username generado manualmente: {}", generatedUsername);

                         // Email es obligatorio para Keycloak
                         if (request.getEmail() == null || request.getEmail().trim().isEmpty()) {
                              request.setEmail(generatedUsername);
                              log.info("[ADMIN] 📧 Email no proporcionado, usando username como email: {}",
                                        generatedUsername);
                         }

                         return Mono.empty(); // Continuar sin datos de RENIEC
                    })
                    .then(Mono.defer(() -> {
                         // 4. VALIDAR ORGANIZACIÓN
                         return organizationClient.getOrganizationById(request.getOrganizationId());
                    }))
                    .flatMap(orgResponse -> {
                         if (!orgResponse.isStatus()) {
                              return Mono.error(new NotFoundException("Organización no encontrada"));
                         }

                         OrganizationClient.OrganizationData orgData = orgResponse.getData();
                         log.info("[ADMIN] Validando organización: {} - {}",
                                   orgData.getOrganizationCode(), orgData.getOrganizationName());

                         if (request.getStreetId() != null) {
                              boolean streetValid = orgData.getZones().stream()
                                        .anyMatch(zone -> zone.getZoneId().equals(request.getZoneId()) &&
                                                  zone.getStreets().stream()
                                                            .anyMatch(street -> street.getStreetId()
                                                                      .equals(request.getStreetId())));

                              if (!streetValid) {
                                   return Mono.error(new ValidationException(
                                             "La calle especificada no pertenece a la organización o zona"));
                              }
                         }

                         return userService.createUserWithCredentials(request);
                    })
                    .flatMap(response -> {
                         if (response.isSuccess()) {
                              UserCreationResponse data = response.getData();
                              log.info("[ADMIN] Cliente creado exitosamente: {} - Username: {} - Password: {}",
                                        data.getUserInfo().getUserCode(), data.getUsername(),
                                        data.getTemporaryPassword());
                              return Mono.just(ApiResponse.success("Cliente creado exitosamente", response.getData()));
                         } else {
                              log.error("[ADMIN] Error creando cliente: {}", response.getMessage());
                              return Mono.error(
                                        new ValidationException("Error creando cliente: " + response.getMessage()));
                         }
                    })
                    .doOnError(ex -> log.error("[ADMIN] Error en creación de cliente: {}", ex.getMessage()));
     }

     /**
      * Listar clientes de la organización del admin
      * GET /api/v1/admin/clients
      */
     @GetMapping("/clients")
     public Mono<ApiResponse<Page<UserWithLocationResponse>>> getMyOrganizationClients(
               @RequestParam(defaultValue = "0") int page,
               @RequestParam(defaultValue = "10") int size,
               ServerWebExchange exchange) {

          log.info("[ADMIN] Listando clientes de la organización del admin");

          Pageable pageable = PageRequest.of(page, size);

          return securityService.getCurrentOrganizationId(exchange)
                    .flatMap(adminOrganizationId -> {
                         log.info("[ADMIN] Listando clientes de organización: {}", adminOrganizationId);
                         return userService.getUsersByRole(adminOrganizationId, RolesUsers.CLIENT);
                    })
                    .flatMap(response -> {
                         if (response.isSuccess()) {
                              // Filtrar solo usuarios con rol CLIENT
                              List<UserResponse> clientUsers = response.getData().stream()
                                        .filter(user -> user.hasRole(RolesUsers.CLIENT))
                                        .filter(user -> !user.hasRole(RolesUsers.ADMIN)
                                                  && !user.hasRole(RolesUsers.SUPER_ADMIN))
                                        .toList();

                              log.info("[ADMIN] Usuarios CLIENT filtrados: {}", clientUsers.size());

                              return enrichUsersWithLocationInfo(clientUsers)
                                        .map(enrichedUsers -> {
                                             Page<UserWithLocationResponse> pageResponse = new org.springframework.data.domain.PageImpl<>(
                                                       enrichedUsers, pageable, enrichedUsers.size());
                                             return ApiResponse.success("Clientes obtenidos exitosamente",
                                                       pageResponse);
                                        });
                         } else {
                              return Mono.error(
                                        new ValidationException("Error obteniendo clientes: " + response.getMessage()));
                         }
                    });
     }

     /**
      * Obtener cliente específico (solo de su organización)
      * GET /api/v1/admin/clients/{id}
      */
     @GetMapping("/clients/{id}")
     public Mono<ApiResponse<UserWithLocationResponse>> getClient(@PathVariable String id) {

          log.info("[ADMIN] Obteniendo cliente con información completa: {}", id);

          return userService.getUserById(id)
                    .flatMap(response -> {
                         if (!response.isSuccess()) {
                              return Mono.error(new NotFoundException("Cliente no encontrado"));
                         }

                         UserResponse user = response.getData();
                         if (!user.isClient()) {
                              return Mono.error(new ForbiddenException("El usuario no es un cliente"));
                         }

                         log.info("[ADMIN] Cliente encontrado: {} - Organización: {}", user.getUserCode(),
                                   user.getOrganizationId());

                         return organizationClient.getOrganizationById(user.getOrganizationId())
                                   .map(orgResponse -> {
                                        if (!orgResponse.isStatus()) {
                                             throw new NotFoundException("Organización no encontrada");
                                        }

                                        OrganizationClient.OrganizationData orgData = orgResponse.getData();

                                        OrganizationClient.Zone zone = null;
                                        OrganizationClient.Street street = null;

                                        if (user.getZoneId() != null && orgData.getZones() != null) {
                                             zone = orgData.getZones().stream()
                                                       .filter(z -> z.getZoneId().equals(user.getZoneId()))
                                                       .findFirst()
                                                       .orElse(null);

                                             if (zone != null && user.getStreetId() != null
                                                       && zone.getStreets() != null) {
                                                  street = zone.getStreets().stream()
                                                            .filter(s -> s.getStreetId().equals(user.getStreetId()))
                                                            .findFirst()
                                                            .orElse(null);
                                             }
                                        }

                                        UserWithLocationResponse.OrganizationInfo orgInfo = UserWithLocationResponse.OrganizationInfo
                                                  .builder()
                                                  .organizationId(orgData.getOrganizationId())
                                                  .organizationCode(orgData.getOrganizationCode())
                                                  .organizationName(orgData.getOrganizationName())
                                                  .legalRepresentative(orgData.getLegalRepresentative())
                                                  .address(orgData.getAddress())
                                                  .phone(orgData.getPhone())
                                                  .status(orgData.getStatus())
                                                  .build();

                                        UserWithLocationResponse.ZoneInfo zoneInfo = null;
                                        if (zone != null) {
                                             zoneInfo = UserWithLocationResponse.ZoneInfo.builder()
                                                       .zoneId(zone.getZoneId())
                                                       .zoneCode(zone.getZoneCode())
                                                       .zoneName(zone.getZoneName())
                                                       .description(zone.getDescription())
                                                       .status(zone.getStatus())
                                                       .build();
                                        }

                                        UserWithLocationResponse.StreetInfo streetInfo = null;
                                        if (street != null) {
                                             streetInfo = UserWithLocationResponse.StreetInfo.builder()
                                                       .streetId(street.getStreetId())
                                                       .streetCode(street.getStreetCode())
                                                       .streetName(street.getStreetName())
                                                       .streetType(street.getStreetType())
                                                       .status(street.getStatus())
                                                       .build();
                                        }

                                        UserWithLocationResponse enrichedUser = UserWithLocationResponse.builder()
                                                  .id(user.getId())
                                                  .userCode(user.getUserCode())
                                                  .firstName(user.getFirstName())
                                                  .lastName(user.getLastName())
                                                  .documentType(user.getDocumentType())
                                                  .documentNumber(user.getDocumentNumber())
                                                  .email(user.getEmail())
                                                  .phone(user.getPhone())
                                                  .address(user.getAddress())
                                                  .roles(user.getRoles())
                                                  .status(user.getStatus())
                                                  .createdAt(user.getCreatedAt())
                                                  .updatedAt(user.getUpdatedAt())
                                                  .organization(orgInfo)
                                                  .zone(zoneInfo)
                                                  .street(streetInfo)
                                                  .build();

                                        log.info("[ADMIN] Cliente enriquecido - Organización: {}, Zona: {}, Calle: {}",
                                                  orgData.getOrganizationName(),
                                                  zone != null ? zone.getZoneName() : "N/A",
                                                  street != null ? street.getStreetName() : "N/A");

                                        return ApiResponse.success(
                                                  "Cliente obtenido exitosamente con información completa",
                                                  enrichedUser);
                                   });
                    });
     }

     /**
      * Actualización parcial de cliente (solo campos permitidos)
      * PATCH /api/v1/admin/clients/{id}
      */
     @PatchMapping("/clients/{id}")
     public Mono<ApiResponse<UserWithLocationResponse>> patchClient(
               @PathVariable String id,
               @Valid @RequestBody UpdateUserPatchRequest request) {

          log.info("[ADMIN] Actualizando parcialmente cliente: {}", id);

          return userService.getUserById(id)
                    .flatMap(userResponse -> {
                         if (!userResponse.isSuccess()) {
                              return Mono.error(new NotFoundException("Cliente no encontrado"));
                         }

                         UserResponse user = userResponse.getData();

                         if (!user.isClient()) {
                              return Mono.error(new ForbiddenException("El usuario no es un cliente"));
                         }

                         if (request.getStreetId() != null && request.getZoneId() != null) {
                              return organizationClient.getOrganizationById(user.getOrganizationId())
                                        .flatMap(orgResponse -> {
                                             if (!orgResponse.isStatus()) {
                                                  return Mono
                                                            .error(new NotFoundException("Organización no encontrada"));
                                             }

                                             OrganizationClient.OrganizationData orgData = orgResponse.getData();
                                             boolean streetValid = orgData.getZones().stream()
                                                       .anyMatch(zone -> zone.getZoneId().equals(request.getZoneId()) &&
                                                                 zone.getStreets().stream()
                                                                           .anyMatch(street -> street.getStreetId()
                                                                                     .equals(request.getStreetId())));

                                             if (!streetValid) {
                                                  return Mono.error(new ValidationException(
                                                            "La calle especificada no pertenece a la organización o zona"));
                                             }

                                             return userService.patchUser(id, request);
                                        });
                         } else {
                              return userService.patchUser(id, request);
                         }
                    })
                    .flatMap(response -> {
                         if (response.isSuccess()) {
                              log.info("[ADMIN] Usuario actualizado correctamente, iniciando enriquecimiento...");

                              // NUEVO: Intentar enriquecimiento, pero con fallback directo si falla
                              return enrichUsersWithLocationInfo(List.of(response.getData()))
                                        .onErrorResume(enrichmentError -> {
                                             log.warn("[ADMIN] Error en enriquecimiento, usando datos actualizados directamente: {}",
                                                       enrichmentError.getMessage());

                                             // Crear respuesta directamente con los datos actualizados
                                             UserWithLocationResponse directResponse = createUserWithLocationResponseWithoutOrg(
                                                       response.getData());
                                             return Mono.just(List.of(directResponse));
                                        })
                                        .map(enrichedUsers -> {
                                             if (!enrichedUsers.isEmpty()) {
                                                  log.info("[ADMIN] Enriquecimiento completado para usuario: {}",
                                                            enrichedUsers.get(0).getUserCode());
                                                  return ApiResponse.success("Cliente actualizado exitosamente",
                                                            enrichedUsers.get(0));
                                             } else {
                                                  log.info("[ADMIN] Usando respuesta sin enriquecimiento para usuario: {}",
                                                            response.getData().getUserCode());
                                                  return ApiResponse.success("Cliente actualizado exitosamente",
                                                            createUserWithLocationResponseWithoutOrg(
                                                                      response.getData()));
                                             }
                                        });
                         } else {
                              return Mono.error(new ValidationException(
                                        "Error actualizando cliente: " + response.getMessage()));
                         }
                    });
     }

     /**
      * Cambiar estado de cliente (solo de su organización)
      * PATCH /api/v1/admin/clients/{id}/status
      */
     @PatchMapping("/clients/{id}/status")
     public Mono<ApiResponse<UserWithLocationResponse>> changeClientStatus(
               @PathVariable String id,
               @RequestParam UserStatus status,
               ServerWebExchange exchange) {

          log.info("[ADMIN] Cambiando estado del cliente {} a {}", id, status);

          return securityService.getCurrentOrganizationId(exchange)
                    .flatMap(adminOrganizationId -> {
                         return userService.getUserById(id)
                                   .flatMap(userResponse -> {
                                        if (!userResponse.isSuccess()) {
                                             return Mono.error(new NotFoundException("Cliente no encontrado"));
                                        }

                                        UserResponse user = userResponse.getData();

                                        // Verificar que es CLIENT
                                        if (!user.isClient()) {
                                             return Mono.error(new ForbiddenException("El usuario no es un cliente"));
                                        }

                                        if (!user.getOrganizationId().equals(adminOrganizationId)) {
                                             return Mono.error(new ForbiddenException(
                                                       "No tienes permiso para cambiar el estado de este cliente"));
                                        }

                                        return userService.changeUserStatus(id, status);
                                   });
                    })
                    .flatMap(response -> {
                         if (response.isSuccess()) {
                              // Enriquecer usuario con información de ubicación
                              return enrichUsersWithLocationInfo(List.of(response.getData()))
                                        .map(enrichedUsers -> {
                                             if (!enrichedUsers.isEmpty()) {
                                                  return ApiResponse.success("Estado del cliente cambiado exitosamente",
                                                            enrichedUsers.get(0));
                                             } else {
                                                  return ApiResponse.success("Estado del cliente cambiado exitosamente",
                                                            createUserWithLocationResponseWithoutOrg(
                                                                      response.getData()));
                                             }
                                        });
                         } else {
                              return Mono.error(new ValidationException(
                                        "Error cambiando estado del cliente: " + response.getMessage()));
                         }
                    });
     }

     /**
      * Eliminar cliente (solo de su organización)
      * DELETE /api/v1/admin/clients/{id}
      */
     @DeleteMapping("/clients/{id}")
     public Mono<ApiResponse<Void>> deleteClient(
               @PathVariable String id) {

          log.warn("[ADMIN] Eliminando cliente: {}", id);

          return userService.deleteUser(id);
     }

     /**
      * Generar código de usuario para su organización
      * POST /api/v1/admin/user-codes/generate
      */
     @PostMapping("/user-codes/generate")
     public Mono<ApiResponse<String>> generateUserCodeForMyOrg(ServerWebExchange exchange) {

          return securityService.getCurrentOrganizationId(exchange)
                    .flatMap(adminOrganizationId -> {
                         log.info("[ADMIN] Generando código de usuario para organización: {}", adminOrganizationId);
                         return userCodeService.generateUserCode(adminOrganizationId);
                    })
                    .map(userCode -> ApiResponse.success("Código de usuario generado exitosamente", userCode))
                    .onErrorMap(error -> {
                         log.error("Error generando código: {}", error.getMessage());
                         return new ValidationException("Error generando código: " + error.getMessage());
                    });
     }

     /**
      * Listar clientes activos de la organización específica
      * GET /api/v1/admin/clients/active
      * OPTIMIZADO: Sin validación preliminar de org (se valida en enrichment)
      */
     @GetMapping("/clients/active")
     public Mono<ApiResponse<List<UserWithLocationResponse>>> getActiveClients(@RequestParam String organizationId) {
          log.info("[ADMIN] Listando clientes activos de organización: {}", organizationId);

          return userService.getActiveUsersByOrganization(organizationId)
                    .flatMap(response -> {
                         if (response.isSuccess()) {
                              // Filtrar solo usuarios con rol CLIENT
                              List<UserResponse> clientUsers = response.getData().stream()
                                        .filter(user -> user.hasRole(RolesUsers.CLIENT))
                                        .filter(user -> !user.hasRole(RolesUsers.ADMIN)
                                                  && !user.hasRole(RolesUsers.SUPER_ADMIN))
                                        .toList();

                              log.info("[ADMIN] Clientes activos CLIENT filtrados: {}", clientUsers.size());

                              return enrichUsersWithLocationInfo(clientUsers)
                                        .map(enrichedUsers -> ApiResponse.success(
                                                  "Clientes activos obtenidos exitosamente",
                                                  enrichedUsers));
                         } else {
                              return Mono.error(new ValidationException(
                                        "Error obteniendo clientes activos: " + response.getMessage()));
                         }
                    });
     }

     /**
      * Listar clientes inactivos de la organización específica
      * GET /api/v1/admin/clients/inactive
      * OPTIMIZADO: Sin validación preliminar de org (se valida en enrichment)
      */
     @GetMapping("/clients/inactive")
     public Mono<ApiResponse<List<UserWithLocationResponse>>> getInactiveClients(@RequestParam String organizationId) {
          log.info("[ADMIN] Listando clientes inactivos de organización: {}", organizationId);

          return userService.getInactiveUsersByOrganization(organizationId)
                    .flatMap(response -> {
                         if (response.isSuccess()) {
                              // Filtrar solo usuarios con rol CLIENT
                              List<UserResponse> clientUsers = response.getData().stream()
                                        .filter(user -> user.hasRole(RolesUsers.CLIENT))
                                        .filter(user -> !user.hasRole(RolesUsers.ADMIN)
                                                  && !user.hasRole(RolesUsers.SUPER_ADMIN))
                                        .toList();

                              log.info("[ADMIN] Clientes inactivos CLIENT filtrados: {}", clientUsers.size());

                              return enrichUsersWithLocationInfo(clientUsers)
                                        .map(enrichedUsers -> ApiResponse.success(
                                                  "Clientes inactivos obtenidos exitosamente",
                                                  enrichedUsers));
                         } else {
                              return Mono.error(new ValidationException(
                                        "Error obteniendo clientes inactivos: " + response.getMessage()));
                         }
                    });
     }

     /**
      * Listar todos los clientes de la organización específica (activos e inactivos)
      * GET /api/v1/admin/clients/all
      * OPTIMIZADO: Sin validación preliminar de org (se valida en enrichment)
      */
     @GetMapping("/clients/all")
     public Mono<ApiResponse<List<UserWithLocationResponse>>> getAllClients(@RequestParam String organizationId) {
          log.info("[ADMIN] Listando todos los clientes de organización: {}", organizationId);

          return userService.getAllUsersByOrganization(organizationId)
                    .flatMap(response -> {
                         if (response.isSuccess()) {
                              // Filtrar solo usuarios con rol CLIENT
                              List<UserResponse> clientUsers = response.getData().stream()
                                        .filter(user -> user.hasRole(RolesUsers.CLIENT))
                                        .filter(user -> !user.hasRole(RolesUsers.ADMIN)
                                                  && !user.hasRole(RolesUsers.SUPER_ADMIN))
                                        .toList();

                              log.info("[ADMIN] Todos los clientes CLIENT filtrados: {}", clientUsers.size());

                              return enrichUsersWithLocationInfo(clientUsers)
                                        .map(enrichedUsers -> ApiResponse.success(
                                                  "Todos los clientes obtenidos exitosamente",
                                                  enrichedUsers));
                         } else {
                              return Mono.error(new ValidationException(
                                        "Error obteniendo todos los clientes: " + response.getMessage()));
                         }
                    });
     }

     /**
      * Restaurar cliente eliminado (solo de su organización)
      * PUT /api/v1/admin/clients/{id}/restore
      */
     @PutMapping("/clients/{id}/restore")
     public Mono<ApiResponse<UserWithLocationResponse>> restoreClient(
               @PathVariable String id) {
          log.info("[ADMIN] Restaurando cliente: {}", id);
          return userService.restoreUser(id)
                    .flatMap(response -> {
                         if (response.isSuccess()) {
                              // Enriquecer usuario restaurado con información de ubicación
                              return enrichUsersWithLocationInfo(List.of(response.getData()))
                                        .map(enrichedUsers -> {
                                             if (!enrichedUsers.isEmpty()) {
                                                  return ApiResponse.success("Cliente restaurado exitosamente",
                                                            enrichedUsers.get(0));
                                             } else {
                                                  return ApiResponse.success("Cliente restaurado exitosamente",
                                                            createUserWithLocationResponseWithoutOrg(
                                                                      response.getData()));
                                             }
                                        });
                         } else {
                              return Mono.error(new ValidationException(
                                        "Error restaurando cliente: " + response.getMessage()));
                         }
                    });
     }

     /**
      * Ver próximo código para su organización
      * GET /api/v1/admin/user-codes/next
      */
     @GetMapping("/user-codes/next")
     public Mono<ApiResponse<String>> getNextUserCodeForMyOrg(ServerWebExchange exchange) {
          return securityService.getCurrentOrganizationId(exchange)
                    .flatMap(adminOrganizationId -> {
                         log.info("[ADMIN] Obteniendo próximo código para organización: {}", adminOrganizationId);
                         return userCodeService.getNextUserCode(adminOrganizationId);
                    })
                    .map(userCode -> ApiResponse.success("Próximo código obtenido exitosamente", userCode));
     }

     /**
      * Generar username inteligente usando datos de RENIEC
      * Maneja casos especiales como palabras de 2 letras ("DE", "LA", etc.)
      *
      * Ejemplos:
      * - VICTORIA ROSALINA, DE LA CRUZ, LAURA -> victoria.cruz.l@jass.gob.pe
      * - JUAN CARLOS, RODRIGUEZ, PEREZ -> juan.rodriguez.p@jass.gob.pe
      * - MARIA, DE LOS SANTOS, null -> maria.santos@jass.gob.pe
      */
     private String generateIntelligentUsername(String firstName, String firstLastName, String secondLastName) {
          log.debug("[ADMIN] 🔧 Generando username con: firstName='{}', firstLastName='{}', secondLastName='{}'",
                    firstName, firstLastName, secondLastName);

          // Procesar primer nombre (usar solo el primero si hay varios)
          String primaryFirstName = cleanAndNormalize(firstName.trim().split("\\s+")[0]).toLowerCase();
          log.debug("[ADMIN] 📝 Primer nombre procesado: '{}'", primaryFirstName);

          // Procesar primer apellido - manejar palabras de 2 letras
          String processedFirstLastName = processLastNameIntelligently(firstLastName);
          log.debug("[ADMIN] 📝 Primer apellido procesado: '{}'", processedFirstLastName);

          // Procesar segundo apellido - solo primera letra si existe
          String secondLastNameInitial = "";
          if (secondLastName != null && !secondLastName.trim().isEmpty()) {
               // Si hay múltiples palabras, tomar la primera letra de la primera palabra
               // significativa
               String[] secondWords = secondLastName.trim().split("\\s+");
               for (String word : secondWords) {
                    if (!isShortWord(word)) {
                         secondLastNameInitial = "." + cleanAndNormalize(word).toLowerCase().charAt(0);
                         break;
                    }
               }
               // Si todas son palabras cortas, tomar la primera de la última palabra
               if (secondLastNameInitial.isEmpty() && secondWords.length > 0) {
                    secondLastNameInitial = "."
                              + cleanAndNormalize(secondWords[secondWords.length - 1]).toLowerCase().charAt(0);
               }
          }
          log.debug("[ADMIN] 📝 Inicial segundo apellido: '{}'", secondLastNameInitial);

          String generatedUsername = primaryFirstName + "." + processedFirstLastName + secondLastNameInitial
                    + "@jass.gob.pe";
          log.info("[ADMIN] ✅ Username generado: '{}'", generatedUsername);

          return generatedUsername;
     }

     /**
      * Procesar apellido de forma inteligente
      * Si encuentra palabras de 2 letras (DE, LA, etc.), busca la siguiente palabra
      * significativa
      */
     private String processLastNameIntelligently(String lastName) {
          if (lastName == null || lastName.trim().isEmpty()) {
               return "apellido";
          }

          String[] words = lastName.trim().split("\\s+");
          log.debug("[ADMIN] 🔍 Procesando apellido con palabras: {}", java.util.Arrays.toString(words));

          // Buscar la primera palabra significativa (más de 2 letras)
          for (String word : words) {
               if (!isShortWord(word)) {
                    log.debug("[ADMIN] ✅ Palabra significativa encontrada: '{}'", word);
                    return cleanAndNormalize(word).toLowerCase();
               }
          }

          // Si todas son palabras cortas, usar la última
          if (words.length > 0) {
               String lastWord = cleanAndNormalize(words[words.length - 1]).toLowerCase();
               log.debug("[ADMIN] 🔄 Usando última palabra: '{}'", lastWord);
               return lastWord;
          }

          return "apellido";
     }

     /**
      * Verificar si una palabra es "corta" (artículos, preposiciones, etc.)
      */
     private boolean isShortWord(String word) {
          if (word == null || word.length() <= 2) {
               return true;
          }

          // Palabras comunes de 3 letras que también consideramos "cortas"
          String upperWord = word.toUpperCase();
          return java.util.Set.of("DE", "LA", "EL", "DEL", "LAS", "LOS", "VON", "VAN", "MAC", "DI").contains(upperWord);
     }

     /**
      * Limpia y normaliza texto removiendo tildes y caracteres especiales
      */
     private String cleanAndNormalize(String text) {
          if (text == null) {
               return null;
          }

          return text.trim()
                    .replaceAll("Á", "A").replaceAll("É", "E").replaceAll("Í", "I")
                    .replaceAll("Ó", "O").replaceAll("Ú", "U").replaceAll("Ñ", "N")
                    .replaceAll("á", "a").replaceAll("é", "e").replaceAll("í", "i")
                    .replaceAll("ó", "o").replaceAll("ú", "u").replaceAll("ñ", "n")
                    .replaceAll("[^A-Za-z0-9\\s]", ""); // Remover caracteres especiales
     }

     /**
      * Enriquece una lista de usuarios con información de organización, zona y calle
      * OPTIMIZADO:
      * 1. Cachea organizaciones (evita múltiples llamadas a la misma org)
      * 2. Llamadas paralelas reactivas
      */
     private Mono<List<UserWithLocationResponse>> enrichUsersWithLocationInfo(List<UserResponse> users) {
          if (users == null || users.isEmpty()) {
               return Mono.just(List.of());
          }

          log.info("[ADMIN] Enriqueciendo {} usuarios con información de ubicación (optimizado)", users.size());

          // Obtener IDs únicos de organizaciones
          Set<String> uniqueOrgIds = users.stream()
                    .map(UserResponse::getOrganizationId)
                    .filter(id -> id != null && !id.isEmpty())
                    .collect(java.util.stream.Collectors.toSet());

          log.info("[ADMIN] Cargando {} organizaciones únicas", uniqueOrgIds.size());

          // Cargar todas las organizaciones en paralelo y cachearlas
          return Flux.fromIterable(uniqueOrgIds)
                    .flatMap(orgId -> organizationClient.getOrganizationById(orgId)
                              .map(orgResponse -> java.util.Map.entry(orgId, orgResponse))
                              .onErrorResume(error -> {
                                   log.warn("[ADMIN] Error cargando organización {}: {}", orgId, error.getMessage());
                                   // Crear respuesta vacía en lugar de null
                                   return Mono.just(java.util.Map.entry(orgId,
                                             new OrganizationClient.OrganizationResponse(false, null)));
                              }))
                    .collectMap(java.util.Map.Entry::getKey, java.util.Map.Entry::getValue)
                    .flatMap(orgCache -> {
                         // Ahora enriquecer usuarios usando el cache
                         return Flux.fromIterable(users)
                                   .map(user -> enrichSingleUserWithCache(user, orgCache))
                                   .collectList();
                    });
     }

     /**
      * Enriquece un usuario usando cache de organizaciones (ultra rápido)
      */
     private UserWithLocationResponse enrichSingleUserWithCache(
               UserResponse user,
               java.util.Map<String, OrganizationClient.OrganizationResponse> orgCache) {

          OrganizationClient.OrganizationResponse orgResponse = orgCache.get(user.getOrganizationId());

          if (orgResponse == null || !orgResponse.isStatus()) {
               return createUserWithLocationResponseWithoutOrg(user);
          }

          OrganizationClient.OrganizationData orgData = orgResponse.getData();
          OrganizationClient.Zone zone = null;
          OrganizationClient.Street street = null;

          // Buscar zona y calle
          if (user.getZoneId() != null && orgData.getZones() != null) {
               zone = orgData.getZones().stream()
                         .filter(z -> z.getZoneId().equals(user.getZoneId()))
                         .findFirst()
                         .orElse(null);

               if (zone != null && user.getStreetId() != null && zone.getStreets() != null) {
                    street = zone.getStreets().stream()
                              .filter(s -> s.getStreetId().equals(user.getStreetId()))
                              .findFirst()
                              .orElse(null);
               }
          }

          // Crear objetos de información
          UserWithLocationResponse.OrganizationInfo orgInfo = UserWithLocationResponse.OrganizationInfo
                    .builder()
                    .organizationId(orgData.getOrganizationId())
                    .organizationCode(orgData.getOrganizationCode())
                    .organizationName(orgData.getOrganizationName())
                    .legalRepresentative(orgData.getLegalRepresentative())
                    .address(orgData.getAddress())
                    .phone(orgData.getPhone())
                    .status(orgData.getStatus())
                    .logo(orgData.getLogo())
                    .build();

          UserWithLocationResponse.ZoneInfo zoneInfo = null;
          if (zone != null) {
               zoneInfo = UserWithLocationResponse.ZoneInfo.builder()
                         .zoneId(zone.getZoneId())
                         .zoneCode(zone.getZoneCode())
                         .zoneName(zone.getZoneName())
                         .description(zone.getDescription())
                         .status(zone.getStatus())
                         .build();
          }

          UserWithLocationResponse.StreetInfo streetInfo = null;
          if (street != null) {
               streetInfo = UserWithLocationResponse.StreetInfo.builder()
                         .streetId(street.getStreetId())
                         .streetCode(street.getStreetCode())
                         .streetName(street.getStreetName())
                         .streetType(street.getStreetType())
                         .status(street.getStatus())
                         .build();
          }

          return UserWithLocationResponse.builder()
                    .id(user.getId())
                    .userCode(user.getUserCode())
                    .firstName(user.getFirstName())
                    .lastName(user.getLastName())
                    .documentType(user.getDocumentType())
                    .documentNumber(user.getDocumentNumber())
                    .email(user.getEmail())
                    .phone(user.getPhone())
                    .address(user.getAddress())
                    .roles(user.getRoles())
                    .status(user.getStatus())
                    .createdAt(user.getCreatedAt())
                    .updatedAt(user.getUpdatedAt())
                    .organization(orgInfo)
                    .zone(zoneInfo)
                    .street(streetInfo)
                    .build();
     }

     /**
      * Crea UserWithLocationResponse sin información de organización (fallback)
      */
     private UserWithLocationResponse createUserWithLocationResponseWithoutOrg(UserResponse user) {
          return UserWithLocationResponse.builder()
                    .id(user.getId())
                    .userCode(user.getUserCode())
                    .firstName(user.getFirstName())
                    .lastName(user.getLastName())
                    .documentType(user.getDocumentType())
                    .documentNumber(user.getDocumentNumber())
                    .email(user.getEmail())
                    .phone(user.getPhone())
                    .address(user.getAddress())
                    .roles(user.getRoles())
                    .status(user.getStatus())
                    .createdAt(user.getCreatedAt())
                    .updatedAt(user.getUpdatedAt())
                    .organization(null)
                    .zone(null)
                    .street(null)
                    .build();
     }

     // ==========================================
     // ENDPOINTS PARA GESTIÓN DE OPERARIOS
     // ==========================================

     /**
      * Crear operario (ADMIN puede crear OPERATOR)
      * POST /api/admin/operators
      * Siempre asigna rol OPERATOR por defecto
      * Usa datos de RENIEC para nombres y genera username inteligente
      */
     @PostMapping("/operators")
     public Mono<ApiResponse<UserCreationResponse>> createOperator(@Valid @RequestBody CreateUserRequest request) {

          log.info("[ADMIN] Creando operario para organización: {}", request.getOrganizationId());

          // Asignar rol OPERATOR por defecto y único permitido
          request.setRoles(java.util.Set.of(RolesUsers.OPERATOR));
          log.info("[ADMIN] Asignando rol OPERATOR por defecto");

          // Validación adicional para asegurar que solo se crea OPERATOR
          if (request.getRoles() == null || !request.getRoles().contains(RolesUsers.OPERATOR) ||
                    request.getRoles().size() != 1) {
               throw new ValidationException("Los administradores solo pueden crear usuarios con rol OPERATOR");
          }

          // 1. VALIDAR Y OBTENER DATOS DE RENIEC
          return reniecClient.getPersonalDataByDni(request.getDocumentNumber())
                    .onErrorResume(error -> {
                         log.error("[ADMIN] Error validando DNI en RENIEC: {}", error.getMessage());
                         return Mono
                                   .error(new ValidationException("DNI no válido según RENIEC: " + error.getMessage()));
                    })
                    .flatMap(reniecData -> {
                         log.info("[ADMIN] ✅ Datos RENIEC obtenidos: {}", reniecData);

                         // 2. GENERAR USERNAME INTELIGENTE USANDO DATOS DE RENIEC
                         String generatedUsername = generateIntelligentUsername(
                                   reniecData.getFirstName(),
                                   reniecData.getFirstLastName(),
                                   reniecData.getSecondLastName());

                         log.info("[ADMIN] 📧 Username generado: {}", generatedUsername);

                         // 3. CONFIGURAR REQUEST CON DATOS DE RENIEC
                         request.setFirstName(reniecData.getFirstName());
                         request.setLastName(reniecData.getFirstLastName() +
                                   (reniecData.getSecondLastName() != null
                                             && !reniecData.getSecondLastName().trim().isEmpty()
                                                       ? " " + reniecData.getSecondLastName()
                                                       : ""));

                         // Email es obligatorio para Keycloak - usar username generado si no se
                         // proporciona
                         if (request.getEmail() == null || request.getEmail().trim().isEmpty()) {
                              request.setEmail(generatedUsername); // Usar username como email para Keycloak
                              log.info("[ADMIN] 📧 Email no proporcionado, usando username como email: {}",
                                        generatedUsername);
                         }

                         // 4. VALIDAR ORGANIZACIÓN
                         return organizationClient.getOrganizationById(request.getOrganizationId());
                    })
                    .flatMap(orgResponse -> {
                         if (!orgResponse.isStatus()) {
                              return Mono.error(new NotFoundException("Organización no encontrada"));
                         }

                         OrganizationClient.OrganizationData orgData = orgResponse.getData();
                         log.info("[ADMIN] Validando organización: {} - {}",
                                   orgData.getOrganizationCode(), orgData.getOrganizationName());

                         if (request.getStreetId() != null) {
                              boolean streetValid = orgData.getZones().stream()
                                        .anyMatch(zone -> zone.getZoneId().equals(request.getZoneId()) &&
                                                  zone.getStreets().stream()
                                                            .anyMatch(street -> street.getStreetId()
                                                                      .equals(request.getStreetId())));

                              if (!streetValid) {
                                   return Mono.error(new ValidationException(
                                             "La calle especificada no pertenece a la organización o zona"));
                              }
                         }

                         return userService.createUserWithCredentials(request);
                    })
                    .flatMap(response -> {
                         if (response.isSuccess()) {
                              UserCreationResponse data = response.getData();
                              log.info("[ADMIN] Operario creado exitosamente: {} - Username: {} - Password: {}",
                                        data.getUserInfo().getUserCode(), data.getUsername(),
                                        data.getTemporaryPassword());
                              return Mono.just(ApiResponse.success("Operario creado exitosamente", response.getData()));
                         } else {
                              log.error("[ADMIN] Error creando operario: {}", response.getMessage());
                              return Mono.error(
                                        new ValidationException("Error creando operario: " + response.getMessage()));
                         }
                    })
                    .doOnError(ex -> log.error("[ADMIN] Error en creación de operario: {}", ex.getMessage()));
     }

     /**
      * Listar todos los operarios de la organización específica (activos e
      * inactivos)
      * GET /api/admin/operators
      */
     @GetMapping("/operators")
     public Mono<ApiResponse<List<UserWithLocationResponse>>> getAllOperators(@RequestParam String organizationId) {
          log.info("[ADMIN] Listando todos los operarios de organización: {}", organizationId);

          // Validar que la organización existe y mostrar información completa
          return organizationClient.getOrganizationById(organizationId)
                    .flatMap(orgResponse -> {
                         if (!orgResponse.isStatus()) {
                              return Mono.error(new NotFoundException("Organización no encontrada: " + organizationId));
                         }

                         OrganizationClient.OrganizationData orgData = orgResponse.getData();
                         log.info("[ADMIN] Organización: {} - {} ({})",
                                   orgData.getOrganizationCode(),
                                   orgData.getOrganizationName(),
                                   orgData.getStatus());

                         if (orgData.getZones() != null && !orgData.getZones().isEmpty()) {
                              log.info("[ADMIN] Zonas de la organización:");
                              orgData.getZones().forEach(zone -> {
                                   log.info("  - Zona: {} - {} ({})",
                                             zone.getZoneCode(),
                                             zone.getZoneName(),
                                             zone.getStatus());
                                   if (zone.getStreets() != null) {
                                        zone.getStreets().forEach(street -> {
                                             log.info("    - Calle: {} - {} ({})",
                                                       street.getStreetCode(),
                                                       street.getStreetName(),
                                                       street.getStatus());
                                        });
                                   }
                              });
                         }

                         return userService.getAllUsersByOrganization(organizationId);
                    })
                    .flatMap(response -> {
                         if (response.isSuccess()) {
                              // Filtrar solo usuarios con rol OPERATOR
                              List<UserResponse> operatorUsers = response.getData().stream()
                                        .filter(user -> user.hasRole(RolesUsers.OPERATOR))
                                        .filter(user -> !user.hasRole(RolesUsers.ADMIN)
                                                  && !user.hasRole(RolesUsers.SUPER_ADMIN))
                                        .toList();

                              log.info("[ADMIN] Todos los operarios OPERATOR filtrados: {}", operatorUsers.size());

                              return enrichUsersWithLocationInfo(operatorUsers)
                                        .map(enrichedUsers -> ApiResponse.success(
                                                  "Todos los operarios obtenidos exitosamente",
                                                  enrichedUsers));
                         } else {
                              return Mono.error(new ValidationException(
                                        "Error obteniendo todos los operarios: " + response.getMessage()));
                         }
                    });
     }

     /**
      * Obtener operario específico (solo de su organización)
      * GET /api/admin/operators/{id}
      */
     @GetMapping("/operators/{id}")
     public Mono<ApiResponse<UserWithLocationResponse>> getOperator(@PathVariable String id) {

          log.info("[ADMIN] Obteniendo operario con información completa: {}", id);

          return userService.getUserById(id)
                    .flatMap(response -> {
                         if (!response.isSuccess()) {
                              return Mono.error(new NotFoundException("Operario no encontrado"));
                         }

                         UserResponse user = response.getData();
                         if (!user.hasRole(RolesUsers.OPERATOR)) {
                              return Mono.error(new ForbiddenException("El usuario no es un operario"));
                         }

                         log.info("[ADMIN] Operario encontrado: {} - Organización: {}", user.getUserCode(),
                                   user.getOrganizationId());

                         return organizationClient.getOrganizationById(user.getOrganizationId())
                                   .map(orgResponse -> {
                                        if (!orgResponse.isStatus()) {
                                             throw new NotFoundException("Organización no encontrada");
                                        }

                                        OrganizationClient.OrganizationData orgData = orgResponse.getData();

                                        OrganizationClient.Zone zone = null;
                                        OrganizationClient.Street street = null;

                                        if (user.getZoneId() != null && orgData.getZones() != null) {
                                             zone = orgData.getZones().stream()
                                                       .filter(z -> z.getZoneId().equals(user.getZoneId()))
                                                       .findFirst()
                                                       .orElse(null);

                                             if (zone != null && user.getStreetId() != null
                                                       && zone.getStreets() != null) {
                                                  street = zone.getStreets().stream()
                                                            .filter(s -> s.getStreetId().equals(user.getStreetId()))
                                                            .findFirst()
                                                            .orElse(null);
                                             }
                                        }

                                        UserWithLocationResponse.OrganizationInfo orgInfo = UserWithLocationResponse.OrganizationInfo
                                                  .builder()
                                                  .organizationId(orgData.getOrganizationId())
                                                  .organizationCode(orgData.getOrganizationCode())
                                                  .organizationName(orgData.getOrganizationName())
                                                  .legalRepresentative(orgData.getLegalRepresentative())
                                                  .address(orgData.getAddress())
                                                  .phone(orgData.getPhone())
                                                  .status(orgData.getStatus())
                                                  .build();

                                        UserWithLocationResponse.ZoneInfo zoneInfo = null;
                                        if (zone != null) {
                                             zoneInfo = UserWithLocationResponse.ZoneInfo.builder()
                                                       .zoneId(zone.getZoneId())
                                                       .zoneCode(zone.getZoneCode())
                                                       .zoneName(zone.getZoneName())
                                                       .description(zone.getDescription())
                                                       .status(zone.getStatus())
                                                       .build();
                                        }

                                        UserWithLocationResponse.StreetInfo streetInfo = null;
                                        if (street != null) {
                                             streetInfo = UserWithLocationResponse.StreetInfo.builder()
                                                       .streetId(street.getStreetId())
                                                       .streetCode(street.getStreetCode())
                                                       .streetName(street.getStreetName())
                                                       .streetType(street.getStreetType())
                                                       .status(street.getStatus())
                                                       .build();
                                        }

                                        UserWithLocationResponse enrichedUser = UserWithLocationResponse.builder()
                                                  .id(user.getId())
                                                  .userCode(user.getUserCode())
                                                  .firstName(user.getFirstName())
                                                  .lastName(user.getLastName())
                                                  .documentType(user.getDocumentType())
                                                  .documentNumber(user.getDocumentNumber())
                                                  .email(user.getEmail())
                                                  .phone(user.getPhone())
                                                  .address(user.getAddress())
                                                  .roles(user.getRoles())
                                                  .status(user.getStatus())
                                                  .createdAt(user.getCreatedAt())
                                                  .updatedAt(user.getUpdatedAt())
                                                  .organization(orgInfo)
                                                  .zone(zoneInfo)
                                                  .street(streetInfo)
                                                  .build();

                                        log.info("[ADMIN] Operario enriquecido - Organización: {}, Zona: {}, Calle: {}",
                                                  orgData.getOrganizationName(),
                                                  zone != null ? zone.getZoneName() : "N/A",
                                                  street != null ? street.getStreetName() : "N/A");

                                        return ApiResponse.success(
                                                  "Operario obtenido exitosamente con información completa",
                                                  enrichedUser);
                                   });
                    });
     }

     /**
      * Actualización parcial de operario (solo campos permitidos)
      * PATCH /api/admin/operators/{id}
      */
     @PatchMapping("/operators/{id}")
     public Mono<ApiResponse<UserWithLocationResponse>> patchOperator(
               @PathVariable String id,
               @Valid @RequestBody UpdateUserPatchRequest request) {

          log.info("[ADMIN] Actualizando parcialmente operario: {}", id);

          return userService.getUserById(id)
                    .flatMap(userResponse -> {
                         if (!userResponse.isSuccess()) {
                              return Mono.error(new NotFoundException("Operario no encontrado"));
                         }

                         UserResponse user = userResponse.getData();

                         if (!user.hasRole(RolesUsers.OPERATOR)) {
                              return Mono.error(new ForbiddenException("El usuario no es un operario"));
                         }

                         if (request.getStreetId() != null && request.getZoneId() != null) {
                              return organizationClient.getOrganizationById(user.getOrganizationId())
                                        .flatMap(orgResponse -> {
                                             if (!orgResponse.isStatus()) {
                                                  return Mono
                                                            .error(new NotFoundException("Organización no encontrada"));
                                             }

                                             OrganizationClient.OrganizationData orgData = orgResponse.getData();
                                             boolean streetValid = orgData.getZones().stream()
                                                       .anyMatch(zone -> zone.getZoneId().equals(request.getZoneId()) &&
                                                                 zone.getStreets().stream()
                                                                           .anyMatch(street -> street.getStreetId()
                                                                                     .equals(request.getStreetId())));

                                             if (!streetValid) {
                                                  return Mono.error(new ValidationException(
                                                            "La calle especificada no pertenece a la organización o zona"));
                                             }

                                             return userService.patchUser(id, request);
                                        });
                         } else {
                              return userService.patchUser(id, request);
                         }
                    })
                    .flatMap(response -> {
                         if (response.isSuccess()) {
                              log.info("[ADMIN] Usuario actualizado correctamente, iniciando enriquecimiento...");

                              // NUEVO: Intentar enriquecimiento, pero con fallback directo si falla
                              return enrichUsersWithLocationInfo(List.of(response.getData()))
                                        .onErrorResume(enrichmentError -> {
                                             log.warn("[ADMIN] Error en enriquecimiento, usando datos actualizados directamente: {}, {}",
                                                       response.getData().getUserCode(), enrichmentError.getMessage());

                                             // Crear respuesta directamente con los datos actualizados
                                             UserWithLocationResponse directResponse = createUserWithLocationResponseWithoutOrg(
                                                       response.getData());
                                             return Mono.just(List.of(directResponse));
                                        })
                                        .map(enrichedUsers -> {
                                             if (!enrichedUsers.isEmpty()) {
                                                  log.info("[ADMIN] Enriquecimiento completado para usuario: {}",
                                                            enrichedUsers.get(0).getUserCode());
                                                  return ApiResponse.success("Operario actualizado exitosamente",
                                                            enrichedUsers.get(0));
                                             } else {
                                                  log.info("[ADMIN] Usando respuesta sin enriquecimiento para usuario: {}",
                                                            response.getData().getUserCode());
                                                  return ApiResponse.success("Operario actualizado exitosamente",
                                                            createUserWithLocationResponseWithoutOrg(
                                                                      response.getData()));
                                             }
                                        });
                         } else {
                              return Mono.error(new ValidationException(
                                        "Error actualizando operario: " + response.getMessage()));
                         }
                    });
     }

     /**
      * Cambiar estado de operario (solo de su organización)
      * PATCH /api/admin/operators/{id}/status
      */
     @PatchMapping("/operators/{id}/status")
     public Mono<ApiResponse<UserWithLocationResponse>> changeOperatorStatus(
               @PathVariable String id,
               @RequestParam UserStatus status,
               ServerWebExchange exchange) {

          log.info("[ADMIN] Cambiando estado del operario {} a {}", id, status);

          return securityService.getCurrentOrganizationId(exchange)
                    .flatMap(adminOrganizationId -> {
                         return userService.getUserById(id)
                                   .flatMap(userResponse -> {
                                        if (!userResponse.isSuccess()) {
                                             return Mono.error(new NotFoundException("Operario no encontrado"));
                                        }

                                        UserResponse user = userResponse.getData();

                                        // Verificar que es OPERATOR
                                        if (!user.hasRole(RolesUsers.OPERATOR)) {
                                             return Mono.error(new ForbiddenException("El usuario no es un operario"));
                                        }

                                        if (!user.getOrganizationId().equals(adminOrganizationId)) {
                                             return Mono.error(new ForbiddenException(
                                                       "No tienes permiso para cambiar el estado de este operario"));
                                        }

                                        return userService.changeUserStatus(id, status);
                                   });
                    })
                    .flatMap(response -> {
                         if (response.isSuccess()) {
                              // Enriquecer usuario con información de ubicación
                              return enrichUsersWithLocationInfo(List.of(response.getData()))
                                        .map(enrichedUsers -> {
                                             if (!enrichedUsers.isEmpty()) {
                                                  return ApiResponse.success(
                                                            "Estado del operario cambiado exitosamente",
                                                            enrichedUsers.get(0));
                                             } else {
                                                  return ApiResponse.success(
                                                            "Estado del operario cambiado exitosamente",
                                                            createUserWithLocationResponseWithoutOrg(
                                                                      response.getData()));
                                             }
                                        });
                         } else {
                              return Mono.error(new ValidationException(
                                        "Error cambiando estado del operario: " + response.getMessage()));
                         }
                    });
     }

     /**
      * Eliminar operario (solo de su organización)
      * DELETE /api/admin/operators/{id}
      */
     @DeleteMapping("/operators/{id}")
     public Mono<ApiResponse<Void>> deleteOperator(
               @PathVariable String id) {

          log.warn("[ADMIN] Eliminando operario: {}", id);

          return userService.deleteUser(id);
     }

     /**
      * Restaurar operario eliminado (solo de su organización)
      * PUT /api/admin/operators/{id}/restore
      */
     @PutMapping("/operators/{id}/restore")
     public Mono<ApiResponse<UserWithLocationResponse>> restoreOperator(
               @PathVariable String id) {
          log.info("[ADMIN] Restaurando operario: {}", id);
          return userService.restoreUser(id)
                    .flatMap(response -> {
                         if (response.isSuccess()) {
                              // Enriquecer usuario restaurado con información de ubicación
                              return enrichUsersWithLocationInfo(List.of(response.getData()))
                                        .map(enrichedUsers -> {
                                             if (!enrichedUsers.isEmpty()) {
                                                  return ApiResponse.success("Operario restaurado exitosamente",
                                                            enrichedUsers.get(0));
                                             } else {
                                                  return ApiResponse.success("Operario restaurado exitosamente",
                                                            createUserWithLocationResponseWithoutOrg(
                                                                      response.getData()));
                                             }
                                        });
                         } else {
                              return Mono.error(new ValidationException(
                                        "Error restaurando operario: " + response.getMessage()));
                         }
                    });
     }

     /**
      * Listar operarios activos de la organización específica
      * GET /api/admin/operators/active
      */
     /**
      * Listar operarios activos de la organización específica
      * GET /api/admin/operators/active
      * OPTIMIZADO: Sin validación preliminar de org (se valida en enrichment)
      */
     @GetMapping("/operators/active")
     public Mono<ApiResponse<List<UserWithLocationResponse>>> getActiveOperators(@RequestParam String organizationId) {
          log.info("[ADMIN] Listando operarios activos de organización: {}", organizationId);

          return userService.getActiveUsersByOrganization(organizationId)
                    .flatMap(response -> {
                         if (response.isSuccess()) {
                              // Filtrar solo usuarios con rol OPERATOR
                              List<UserResponse> operatorUsers = response.getData().stream()
                                        .filter(user -> user.hasRole(RolesUsers.OPERATOR))
                                        .filter(user -> !user.hasRole(RolesUsers.ADMIN)
                                                  && !user.hasRole(RolesUsers.SUPER_ADMIN))
                                        .toList();

                              log.info("[ADMIN] Operarios activos OPERATOR filtrados: {}", operatorUsers.size());

                              return enrichUsersWithLocationInfo(operatorUsers)
                                        .map(enrichedUsers -> ApiResponse.success(
                                                  "Operarios activos obtenidos exitosamente",
                                                  enrichedUsers));
                         } else {
                              return Mono.error(new ValidationException(
                                        "Error obteniendo operarios activos: " + response.getMessage()));
                         }
                    });
     }

     /**
      * Listar operarios inactivos de la organización específica
      * GET /api/admin/operators/inactive
      * OPTIMIZADO: Sin validación preliminar de org (se valida en enrichment)
      */
     @GetMapping("/operators/inactive")
     public Mono<ApiResponse<List<UserWithLocationResponse>>> getInactiveOperators(
               @RequestParam String organizationId) {
          log.info("[ADMIN] Listando operarios inactivos de organización: {}", organizationId);

          return userService.getInactiveUsersByOrganization(organizationId)
                    .flatMap(response -> {
                         if (response.isSuccess()) {
                              // Filtrar solo usuarios con rol OPERATOR
                              List<UserResponse> operatorUsers = response.getData().stream()
                                        .filter(user -> user.hasRole(RolesUsers.OPERATOR))
                                        .filter(user -> !user.hasRole(RolesUsers.ADMIN)
                                                  && !user.hasRole(RolesUsers.SUPER_ADMIN))
                                        .toList();

                              log.info("[ADMIN] Operarios inactivos OPERATOR filtrados: {}", operatorUsers.size());

                              return enrichUsersWithLocationInfo(operatorUsers)
                                        .map(enrichedUsers -> ApiResponse.success(
                                                  "Operarios inactivos obtenidos exitosamente",
                                                  enrichedUsers));
                         } else {
                              return Mono.error(new ValidationException(
                                        "Error obteniendo operarios inactivos: " + response.getMessage()));
                         }
                    });
     }

}
