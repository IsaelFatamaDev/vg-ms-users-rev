package pe.edu.vallegrande.vgmsusers.infrastructure.rest.internal;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;
import pe.edu.vallegrande.vgmsusers.application.service.UserService;
import pe.edu.vallegrande.vgmsusers.infrastructure.client.OrganizationClient;
import pe.edu.vallegrande.vgmsusers.infrastructure.client.InfrastructureClient;
import pe.edu.vallegrande.vgmsusers.infrastructure.dto.request.CreateUserRequest;
import pe.edu.vallegrande.vgmsusers.infrastructure.dto.response.CompleteUserResponse;
import pe.edu.vallegrande.vgmsusers.infrastructure.dto.response.UserCreationResponse;
import pe.edu.vallegrande.vgmsusers.infrastructure.dto.ApiResponse;
import pe.edu.vallegrande.vgmsusers.infrastructure.dto.response.UserResponse;
import pe.edu.vallegrande.vgmsusers.infrastructure.dto.response.UserWithLocationResponse;
import pe.edu.vallegrande.vgmsusers.infrastructure.exception.ForbiddenException;
import pe.edu.vallegrande.vgmsusers.infrastructure.exception.NotFoundException;
import pe.edu.vallegrande.vgmsusers.infrastructure.exception.ValidationException;
import pe.edu.vallegrande.vgmsusers.infrastructure.client.ReniecClient;
import pe.edu.vallegrande.vgmsusers.domain.enums.RolesUsers;
import reactor.core.publisher.Mono;

import jakarta.validation.Valid;
import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * REST Controller INTERNO para comunicación entre microservicios
 * - SIN autenticación JWT (solo para uso interno)
 * - Endpoints simples para otros microservicios del ecosistema JASS
 */
@RestController
@RequestMapping("/internal")
@RequiredArgsConstructor
@Slf4j
public class InternalRest {

     private final UserService userService;
     private final ReniecClient reniecClient;
     private final OrganizationClient organizationClient;
     private final InfrastructureClient infrastructureClient;

     /**
      * Obtener todos los usuarios de una organización
      * GET /internal/organizations/{organizationId}/users
      */
     @GetMapping("/organizations/{organizationId}/users")
     public Mono<ApiResponse<List<CompleteUserResponse>>> getUsersByOrganization(@PathVariable String organizationId) {
          log.info("[INTERNAL] 🔍 Obteniendo usuarios completos de organización: {}", organizationId);

          return userService.getCompleteUsersByOrganization(organizationId)
                    .flatMap(response -> {
                         if (response.isSuccess()) {
                              log.info("[INTERNAL] ✅ {} usuarios obtenidos con información completa",
                                        response.getData().size());
                              return Mono.just(
                                        ApiResponse.success("Usuarios obtenidos exitosamente con información completa",
                                                  response.getData()));
                         } else {
                              log.warn("[INTERNAL] ⚠️  No se pudieron obtener usuarios: {}", response.getMessage());
                              return Mono.just(ApiResponse.<List<CompleteUserResponse>>error(
                                        "Error obteniendo usuarios: " + response.getMessage()));
                         }
                    });
     }

     /**
      * Obtener solo clientes de una organización
      * GET /internal/organizations/{organizationId}/clients
      */
     @GetMapping("/organizations/{organizationId}/clients")
     public Mono<ApiResponse<List<CompleteUserResponse>>> getClientsByOrganization(
               @PathVariable String organizationId) {
          log.info("[INTERNAL] 🔍 Obteniendo clientes completos de organización: {}", organizationId);

          return userService.getCompleteUsersByRole(organizationId, RolesUsers.CLIENT)
                    .flatMap(response -> {
                         if (response.isSuccess()) {
                              log.info("[INTERNAL] ✅ {} clientes obtenidos con información completa",
                                        response.getData().size());
                              return Mono.just(
                                        ApiResponse.success("Clientes obtenidos exitosamente con información completa",
                                                  response.getData()));
                         } else {
                              log.warn("[INTERNAL] ⚠️  No se pudieron obtener clientes: {}", response.getMessage());
                              return Mono.just(ApiResponse.<List<CompleteUserResponse>>error(
                                        "Error obteniendo clientes: " + response.getMessage()));
                         }
                    });
     }

     /**
      * Obtener solo administradores de una organización
      * GET /internal/organizations/{organizationId}/admins
      */
     @GetMapping("/organizations/{organizationId}/admins")
     public Mono<ApiResponse<List<CompleteUserResponse>>> getAdminsByOrganization(@PathVariable String organizationId) {
          log.info("[INTERNAL] 🔍 Obteniendo administradores completos de organización: {}", organizationId);

          return userService.getCompleteUsersByRole(organizationId, RolesUsers.ADMIN)
                    .flatMap(response -> {
                         if (response.isSuccess()) {
                              log.info("[INTERNAL] ✅ {} administradores obtenidos con información completa",
                                        response.getData().size());
                              return Mono.just(ApiResponse.success(
                                        "Administradores obtenidos exitosamente con información completa",
                                        response.getData()));
                         } else {
                              log.warn("[INTERNAL] ⚠️  No se pudieron obtener administradores: {}",
                                        response.getMessage());
                              return Mono.just(ApiResponse.<List<CompleteUserResponse>>error(
                                        "Error obteniendo administradores: " + response.getMessage()));
                         }
                    });
     }

     /**
      * Obtener información de un usuario específico
      * GET /internal/users/{userId}
      */
     @GetMapping("/users/{userId}")
     public Mono<ApiResponse<CompleteUserResponse>> getUserById(@PathVariable String userId) {
          log.info("[INTERNAL] 🔍 Obteniendo información completa del usuario (incluyendo eliminados): {}", userId);

          return userService.getCompleteUserByIdIncludingDeleted(userId)
                    .flatMap(response -> {
                         if (response.isSuccess()) {
                              log.info("[INTERNAL] ✅ Usuario completo obtenido: {}", response.getData().getUserCode());
                              return Mono
                                        .just(ApiResponse.success(
                                                  "Usuario obtenido exitosamente con información completa",
                                                  response.getData()));
                         } else {
                              log.warn("[INTERNAL] ⚠️  Usuario no encontrado: {}", response.getMessage());
                              // Devolver respuesta de error sin lanzar excepción para endpoints internos
                              return Mono.just(ApiResponse.<CompleteUserResponse>error(
                                        "Usuario no encontrado: " + response.getMessage()));
                         }
                    })
                    .onErrorResume(error -> {
                         log.error("[INTERNAL] ❌ Error interno obteniendo usuario {}: {}", userId, error.getMessage());
                         return Mono.just(ApiResponse.<CompleteUserResponse>error(
                                   "Error interno: " + error.getMessage()));
                    });
     }

     /*
      * OBTENER UN USUARIO CON DATOS DE SU ORGANIZACION Y CAJA DE AGUA (SUMINISTRO)
      */
     @GetMapping("/clients/{id}")
     public Mono<ApiResponse<UserWithLocationResponse>> getClient(@PathVariable String id) {

          log.info("[INTERNAL] Obteniendo cliente con información completa: {}", id);

          return userService.getUserById(id)
                    .flatMap(response -> {
                         if (!response.isSuccess()) {
                              return Mono.error(new NotFoundException("Cliente no encontrado"));
                         }

                         UserResponse user = response.getData();
                         if (!user.isClient()) {
                              return Mono.error(new ForbiddenException("El usuario no es un cliente"));
                         }

                         log.info("[INTERNAL] Cliente encontrado: {} - Organización: {}", user.getUserCode(),
                                   user.getOrganizationId());

                         // Llamar a ambos servicios en paralelo
                         Mono<OrganizationClient.OrganizationResponse> orgMono = organizationClient
                                   .getOrganizationById(user.getOrganizationId());
                         Mono<InfrastructureClient.WaterBoxAssignmentResponse> waterBoxMono = infrastructureClient
                                   .getActiveWaterBoxAssignmentByUserId(id);

                         return Mono.zip(orgMono, waterBoxMono.map(Optional::of).defaultIfEmpty(Optional.empty()))
                                   .map(tuple -> {
                                        OrganizationClient.OrganizationResponse orgResponse = tuple.getT1();
                                        InfrastructureClient.WaterBoxAssignmentResponse waterBoxAssignment = tuple
                                                  .getT2().orElse(null);

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

                                        // Construir información de caja de agua si existe
                                        UserWithLocationResponse.WaterBoxAssignmentInfo waterBoxInfo = null;
                                        if (waterBoxAssignment != null) {
                                             waterBoxInfo = UserWithLocationResponse.WaterBoxAssignmentInfo.builder()
                                                       .id(waterBoxAssignment.getId())
                                                       .waterBoxId(waterBoxAssignment.getWaterBoxId())
                                                       .userId(waterBoxAssignment.getUserId())
                                                       .startDate(waterBoxAssignment.getStartDate())
                                                       .endDate(waterBoxAssignment.getEndDate())
                                                       .monthlyFee(waterBoxAssignment.getMonthlyFee())
                                                       .status(waterBoxAssignment.getStatus())
                                                       .createdAt(waterBoxAssignment.getCreatedAt())
                                                       .transferId(waterBoxAssignment.getTransferId())
                                                       .boxCode(waterBoxAssignment.getBoxCode())
                                                       .boxType(waterBoxAssignment.getBoxType())
                                                       .build();

                                             log.info("[INTERNAL] Caja de agua encontrada: {} - Tipo: {}",
                                                       waterBoxAssignment.getBoxCode(),
                                                       waterBoxAssignment.getBoxType());
                                        } else {
                                             log.info("[INTERNAL] Cliente sin caja de agua asignada");
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
                                                  .waterBoxAssignment(waterBoxInfo)
                                                  .build();

                                        log.info("[INTERNAL] Cliente enriquecido - Organización: {}, Zona: {}, Calle: {}, Caja: {}",
                                                  orgData.getOrganizationName(),
                                                  zone != null ? zone.getZoneName() : "N/A",
                                                  street != null ? street.getStreetName() : "N/A",
                                                  waterBoxInfo != null ? waterBoxInfo.getBoxCode() : "N/A");

                                        return ApiResponse.success(
                                                  "Cliente obtenido exitosamente con información completa",
                                                  enrichedUser);
                                   });
                    });
     }

     /**
      * Crear un administrador desde otro microservicio
      * POST /internal/organizations/{organizationId}/create-admin
      */
     @PostMapping("/organizations/{organizationId}/create-admin")
     public Mono<ApiResponse<UserCreationResponse>> createAdmin(
               @PathVariable String organizationId,
               @Valid @RequestBody CreateUserRequest request) {

          log.info("[INTERNAL] Creando administrador para organización: {}", organizationId);

          // Validar que la organización coincida
          if (!organizationId.equals(request.getOrganizationId())) {
               return Mono.error(new ValidationException("OrganizationId en URL no coincide con el del body"));
          }

          // Configurar rol ADMIN
          request.setRoles(Set.of(RolesUsers.ADMIN));

          // Validar DNI con RENIEC
          return reniecClient.getPersonalDataByDni(request.getDocumentNumber())
                    .doOnNext(reniecData -> {
                         log.info("[INTERNAL] Datos RENIEC obtenidos: {}", reniecData);

                         // Actualizar con datos reales de RENIEC
                         request.setFirstName(reniecData.getFirstName());
                         request.setLastName(reniecData.getFirstLastName() +
                                   (reniecData.getSecondLastName() != null
                                             && !reniecData.getSecondLastName().trim().isEmpty()
                                                       ? " " + reniecData.getSecondLastName()
                                                       : ""));

                         // Email opcional - usar username generado si no se proporciona
                         if (request.getEmail() == null || request.getEmail().trim().isEmpty()) {
                              String generatedUsername = generateSimpleUsername(
                                        reniecData.getFirstName(),
                                        reniecData.getFirstLastName(),
                                        reniecData.getSecondLastName());
                              request.setEmail(generatedUsername);
                         }
                    })
                    .onErrorMap(error -> new ValidationException("DNI no válido según RENIEC: " + error.getMessage()))
                    .flatMap(reniecData -> userService.createUserWithCredentials(request))
                    .flatMap(response -> {
                         if (response.isSuccess()) {
                              UserCreationResponse data = response.getData();
                              log.info("[INTERNAL] Administrador creado exitosamente: {} - Username: {}",
                                        data.getUserInfo().getUserCode(), data.getUsername());
                              return Mono.just(
                                        ApiResponse.success("Administrador creado exitosamente", response.getData()));
                         } else {
                              return Mono.error(new ValidationException(
                                        "Error creando administrador: " + response.getMessage()));
                         }
                    });
     }

     /**
      * Crear un super administrador desde otro microservicio
      * POST /internal/create-super-admin
      */
     @PostMapping("/create-super-admin")
     public Mono<ApiResponse<UserCreationResponse>> createSuperAdmin(
               @Valid @RequestBody CreateUserRequest request) {

          log.info("[INTERNAL] 🚀 Creando SUPER_ADMIN con DNI: {}", request.getDocumentNumber());

          // Configurar rol SUPER_ADMIN
          request.setRoles(Set.of(RolesUsers.SUPER_ADMIN));

          // Validar DNI con RENIEC y obtener datos reales
          log.info("[INTERNAL] Validando DNI {} con RENIEC para SUPER_ADMIN", request.getDocumentNumber());

          return reniecClient.getPersonalDataByDni(request.getDocumentNumber())
                    .doOnNext(reniecData -> {
                         log.info("[INTERNAL] 📄 Datos RENIEC recibidos para SUPER_ADMIN: {}", reniecData);

                         // Actualizar request con datos reales de RENIEC
                         request.setFirstName(reniecData.getFirstName());
                         request.setLastName(
                                   reniecData.getFirstLastName() +
                                             (reniecData.getSecondLastName() != null
                                                       && !reniecData.getSecondLastName().trim().isEmpty()
                                                                 ? " " + reniecData.getSecondLastName()
                                                                 : ""));

                         // Generar username inteligente basado en datos RENIEC
                         String generatedUsername = generateIntelligentUsername(
                                   reniecData.getFirstName(),
                                   reniecData.getFirstLastName(),
                                   reniecData.getSecondLastName());

                         // Email es obligatorio para Keycloak - usar username generado si no se
                         // proporciona
                         if (request.getEmail() == null || request.getEmail().trim().isEmpty()) {
                              request.setEmail(generatedUsername);
                         }

                         log.info("[INTERNAL] Username generado para SUPER_ADMIN: {}", generatedUsername);
                    })
                    .onErrorMap(error -> {
                         log.error("[INTERNAL] Error validando DNI con RENIEC: {}", error.getMessage());
                         return new ValidationException("DNI no válido según RENIEC: " + error.getMessage());
                    })
                    .flatMap(reniecData -> {
                         log.info("[INTERNAL] Creando SUPER_ADMIN con datos validados de RENIEC");
                         return userService.createUserWithCredentials(request);
                    })
                    .flatMap(response -> {
                         if (response.isSuccess()) {
                              UserCreationResponse data = response.getData();
                              log.info("[INTERNAL] ✅ SUPER_ADMIN creado exitosamente: {} - Username: {}",
                                        data.getUserInfo().getUserCode(), data.getUsername());
                              return Mono.just(
                                        ApiResponse.success("SUPER_ADMIN creado exitosamente", response.getData()));
                         } else {
                              log.error("[INTERNAL] ❌ Error creando SUPER_ADMIN: {}", response.getMessage());
                              return Mono.error(new ValidationException(
                                        "Error creando SUPER_ADMIN: " + response.getMessage()));
                         }
                    });
     }

     /**
      * Genera username inteligente basado en datos RENIEC
      * Maneja casos especiales como palabras de 2 letras (DE, LA, etc.)
      *
      * Ejemplos:
      * - VICTORIA ROSALINA + DE LA CRUZ + LAURA = victoria.cruz.l@jass.gob.pe
      * - JUAN CARLOS + PÉREZ + LÓPEZ = juan.perez.l@jass.gob.pe
      * - MARÍA + GONZÁLEZ + null = maria.gonzalez@jass.gob.pe
      */
     private String generateIntelligentUsername(String firstName, String firstLastName, String secondLastName) {
          log.debug("[INTERNAL] Generando username para: {} | {} | {}", firstName, firstLastName, secondLastName);

          // Limpiar y normalizar nombres
          String cleanFirstName = cleanAndNormalize(firstName);
          String cleanFirstLastName = cleanAndNormalize(firstLastName);
          String cleanSecondLastName = cleanAndNormalize(secondLastName);

          // Obtener primer nombre (primera palabra)
          String firstNamePart = getFirstWord(cleanFirstName).toLowerCase();

          // Procesar primer apellido de manera inteligente
          String lastNamePart = processFirstLastName(cleanFirstLastName).toLowerCase();

          // Inicializar username
          StringBuilder username = new StringBuilder();
          username.append(firstNamePart).append(".").append(lastNamePart);

          // Agregar inicial del segundo apellido si existe
          if (cleanSecondLastName != null && !cleanSecondLastName.trim().isEmpty()) {
               String secondInitial = String.valueOf(cleanSecondLastName.trim().charAt(0)).toLowerCase();
               username.append(".").append(secondInitial);
          }

          // Agregar dominio
          username.append("@jass.gob.pe");

          String result = username.toString();
          log.info("[INTERNAL] Username generado: {} para persona: {} {} {}",
                    result, firstName, firstLastName, secondLastName);

          return result;
     }

     /**
      * Procesa el primer apellido de manera inteligente
      * Si tiene palabras de 2 letras o menos, busca la siguiente palabra válida
      */
     private String processFirstLastName(String firstLastName) {
          if (firstLastName == null || firstLastName.trim().isEmpty()) {
               return "usuario";
          }

          String[] words = firstLastName.trim().split("\\s+");

          // Buscar la primera palabra que tenga más de 2 letras
          for (String word : words) {
               if (word.length() > 2) {
                    log.debug("[INTERNAL] Palabra válida encontrada en primer apellido: {}", word);
                    return word;
               }
          }

          // Si todas las palabras son de 2 letras o menos, usar la última
          String lastWord = words[words.length - 1];
          log.debug("[INTERNAL] Usando última palabra del primer apellido: {}", lastWord);
          return lastWord;
     }

     /**
      * Obtiene la primera palabra de un string
      */
     private String getFirstWord(String text) {
          if (text == null || text.trim().isEmpty()) {
               return "usuario";
          }

          String[] words = text.trim().split("\\s+");
          return words[0];
     }

     /**
      * Limpia y normaliza texto removiendo tildes y caracteres especiales
      */
     private String cleanAndNormalize(String text) {
          if (text == null) {
               return null;
          }

          return text.trim()
                    .replace("Á", "A").replace("É", "E").replace("Í", "I")
                    .replace("Ó", "O").replace("Ú", "U").replace("Ñ", "N")
                    .replace("á", "a").replace("é", "e").replace("í", "i")
                    .replace("ó", "o").replace("ú", "u").replace("ñ", "n")
                    .replaceAll("[^A-Za-z0-9\\s]", "");
     }

     /**
      * Genera username simple: nombre.apellido@jass.gob.pe
      */
     private String generateSimpleUsername(String firstName, String firstLastName, String secondLastName) {
          String name = firstName != null ? firstName.trim().split("\\s+")[0].toLowerCase() : "usuario";
          String lastname = firstLastName != null ? firstLastName.trim().split("\\s+")[0].toLowerCase() : "temporal";

          return name + "." + lastname + "@jass.gob.pe";
     }
}
