package pe.edu.vallegrande.vgmsusers.application.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import pe.edu.vallegrande.vgmsusers.application.service.UserAuthIntegrationService;
import pe.edu.vallegrande.vgmsusers.application.service.UserCodeService;
import pe.edu.vallegrande.vgmsusers.application.service.UserService;
import pe.edu.vallegrande.vgmsusers.domain.enums.RolesUsers;
import pe.edu.vallegrande.vgmsusers.domain.enums.UserStatus;
import pe.edu.vallegrande.vgmsusers.domain.model.AddressUsers;
import pe.edu.vallegrande.vgmsusers.domain.model.Contact;
import pe.edu.vallegrande.vgmsusers.domain.model.PersonalInfo;
import pe.edu.vallegrande.vgmsusers.domain.model.User;
import pe.edu.vallegrande.vgmsusers.infrastructure.dto.ApiResponse;
import pe.edu.vallegrande.vgmsusers.infrastructure.dto.request.CreateUserRequest;
import pe.edu.vallegrande.vgmsusers.infrastructure.dto.request.UpdateUserPatchRequest;
import pe.edu.vallegrande.vgmsusers.infrastructure.dto.request.UpdateUserRequest;
import pe.edu.vallegrande.vgmsusers.infrastructure.dto.response.UserCreationResponse;
import pe.edu.vallegrande.vgmsusers.infrastructure.dto.response.UserResponse;
import pe.edu.vallegrande.vgmsusers.infrastructure.dto.response.CompleteUserResponse;
import pe.edu.vallegrande.vgmsusers.infrastructure.repository.UserRepository;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Implementación del servicio de gestión de usuarios
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

     private final UserRepository userRepository;
     private final UserCodeService userCodeService;
     private final UserAuthIntegrationService userAuthIntegrationService;

     @Override
     public Mono<ApiResponse<UserResponse>> createUser(CreateUserRequest request) {
          log.info("Creando usuario para organización: {}", request.getOrganizationId());

          // MS-AUTHENTICATION se encargará de generar la contraseña temporal
          log.debug("Usuario será registrado con contraseña automática de MS-AUTHENTICATION");

          return validateCreateRequest(request)
                    .then(userCodeService.generateUserCode(request.getOrganizationId()))
                    .flatMap(userCode -> {
                         PersonalInfo personalInfo = PersonalInfo.builder()
                                   .firstName(request.getFirstName())
                                   .lastName(request.getLastName())
                                   .documentType(request.getDocumentType())
                                   .documentNumber(request.getDocumentNumber())
                                   .build();

                         Contact contact = Contact.builder()
                                   .email(request.getEmail())
                                   .phone(request.getPhone())
                                   .address(AddressUsers.builder()
                                             .fullAddress(request.getAddress())
                                             .streetId(request.getStreetId())
                                             .zoneId(request.getZoneId())
                                             .build())
                                   .build();

                         // NO generar username aquí - MS-AUTHENTICATION lo generará
                         // Crear usuario sin username (se actualizará después)
                         User user = User.builder()
                                   .userCode(userCode)
                                   .username("") // Temporal - se actualizará con la respuesta de MS-AUTHENTICATION
                                   .organizationId(request.getOrganizationId())
                                   .personalInfo(personalInfo)
                                   .contact(contact)
                                   .roles(request.getRoles()) // Asignar todos los roles
                                   .status(UserStatus.ACTIVE)
                                   .registrationDate(LocalDateTime.now())
                                   .createdAt(LocalDateTime.now())
                                   .updatedAt(LocalDateTime.now())
                                   .build();

                         return userRepository.save(user)
                                   .flatMap(savedUser -> {
                                        // Registrar usuario en MS-AUTHENTICATION (con contraseña automática)
                                        return userAuthIntegrationService
                                                  .registerUserWithAutoPassword(savedUser)
                                                  .flatMap(authResponse -> {
                                                       log.info("Respuesta del servicio de autenticación: {}",
                                                                 authResponse.message());

                                                       // ACTUALIZAR el username en la BD con el generado por
                                                       // MS-AUTHENTICATION
                                                       savedUser.setUsername(authResponse.username());

                                                       return userRepository.save(savedUser)
                                                                 .map(userUpdated -> userUpdated);
                                                  })
                                                  .onErrorResume(error -> {
                                                       log.error("Error registrando usuario en servicio de autenticación: {}",
                                                                 error.getMessage());
                                                       // Mantener usuario sin username si falla MS-AUTHENTICATION
                                                       return Mono.just(savedUser);
                                                  });
                                   });
                    })
                    .map(this::mapToUserResponse)
                    .map(userResponse -> ApiResponse.<UserResponse>builder()
                              .data(userResponse)
                              .message("Usuario creado exitosamente")
                              .success(true)
                              .build())
                    .doOnSuccess(response -> log.info("Usuario creado: {}", response.getData().getUserCode()))
                    .onErrorResume(error -> {
                         log.error("Error creando usuario: {}", error.getMessage());
                         return Mono.just(ApiResponse.<UserResponse>builder()
                                   .success(false)
                                   .message("Error creando usuario: " + error.getMessage())
                                   .build());
                    });
     }

     @Override
     public Mono<ApiResponse<UserCreationResponse>> createUserWithCredentials(CreateUserRequest request) {
          log.info("Creando usuario con credenciales para organización: {}", request.getOrganizationId());

          // CAMBIO: NO generar contraseña temporal aquí, dejar que MS-AUTHENTICATION
          // la genere
          // String temporaryPassword = passwordService.generateTemporaryPassword();
          // log.debug("Contraseña temporal generada para usuario");
          log.info("🔧 MS-AUTHENTICATION generará la contraseña temporal automáticamente");

          return validateCreateRequest(request)
                    .then(userCodeService.generateUserCode(request.getOrganizationId()))
                    .flatMap(userCode -> {
                         PersonalInfo personalInfo = PersonalInfo.builder()
                                   .firstName(request.getFirstName())
                                   .lastName(request.getLastName())
                                   .documentType(request.getDocumentType())
                                   .documentNumber(request.getDocumentNumber())
                                   .build();

                         Contact contact = Contact.builder()
                                   .email(request.getEmail())
                                   .phone(request.getPhone())
                                   .address(AddressUsers.builder()
                                             .fullAddress(request.getAddress())
                                             .streetId(request.getStreetId())
                                             .zoneId(request.getZoneId())
                                             .build())
                                   .build();

                         // NO generar username aquí - MS-AUTHENTICATION lo generará
                         // Crear usuario sin username (se actualizará después)
                         User user = User.builder()
                                   .userCode(userCode)
                                   .username("") // Temporal - se actualizará con la respuesta de MS-AUTHENTICATION
                                   .organizationId(request.getOrganizationId())
                                   .personalInfo(personalInfo)
                                   .contact(contact)
                                   .roles(request.getRoles())
                                   .status(UserStatus.ACTIVE)
                                   .registrationDate(LocalDateTime.now())
                                   .createdAt(LocalDateTime.now())
                                   .updatedAt(LocalDateTime.now())
                                   .build();

                         return userRepository.save(user)
                                   .flatMap(savedUser -> {
                                        // Registrar usuario en MS-AUTHENTICATION (con contraseña automática)
                                        return userAuthIntegrationService
                                                  .registerUserWithAutoPassword(savedUser)
                                                  .flatMap(authResponse -> {
                                                       log.info("Respuesta del servicio de autenticación: {}",
                                                                 authResponse.message());

                                                       // ACTUALIZAR el username en la BD con el generado por
                                                       // MS-AUTHENTICATION
                                                       savedUser.setUsername(authResponse.username());

                                                       return userRepository.save(savedUser)
                                                                 .map(userUpdated -> {
                                                                      // Crear respuesta con credenciales usando la
                                                                      // contraseña de MS-AUTHENTICATION
                                                                      UserResponse userResponse = mapToUserResponse(
                                                                                userUpdated);
                                                                      return UserCreationResponse.success(
                                                                                userResponse,
                                                                                authResponse.username(),
                                                                                authResponse.temporaryPassword());
                                                                 });
                                                  })
                                                  .onErrorResume(error -> {
                                                       log.error("Error registrando usuario en servicio de autenticación: {}",
                                                                 error.getMessage());

                                                       // Aunque falle MS-AUTHENTICATION, el usuario se creó en MongoDB
                                                       UserResponse userResponse = mapToUserResponse(savedUser);
                                                       UserCreationResponse creationResponse = UserCreationResponse
                                                                 .builder()
                                                                 .userInfo(userResponse)
                                                                 .username(savedUser.getUsername())
                                                                 .temporaryPassword("ERROR_MS_AUTH")
                                                                 .message("Usuario creado en BD local, pero falló registro en MS-AUTHENTICATION: "
                                                                           + error.getMessage())
                                                                 .requiresPasswordChange(true)
                                                                 .build();

                                                       return Mono.just(creationResponse);
                                                  });
                                   });
                    })
                    .map(creationResponse -> ApiResponse.<UserCreationResponse>builder()
                              .data(creationResponse)
                              .message("Usuario creado exitosamente con credenciales")
                              .success(true)
                              .build())
                    .doOnSuccess(response -> {
                         UserCreationResponse data = response.getData();
                         log.info("Usuario creado con credenciales: {} - Username: {} - Password: {}",
                                   data.getUserInfo().getUserCode(),
                                   data.getUsername(),
                                   data.getTemporaryPassword());
                    })
                    .onErrorResume(error -> {
                         log.error("Error creando usuario: {}", error.getMessage());
                         return Mono.just(ApiResponse.<UserCreationResponse>builder()
                                   .success(false)
                                   .message("Error creando usuario: " + error.getMessage())
                                   .data(UserCreationResponse.error("Error creando usuario: " + error.getMessage()))
                                   .build());
                    });
     }

     @Override
     public Mono<ApiResponse<UserResponse>> getUserById(String id) {
          return userRepository.findById(id)
                    .map(this::mapToUserResponse)
                    .map(userResponse -> ApiResponse.<UserResponse>builder()
                              .data(userResponse)
                              .message("Usuario encontrado")
                              .success(true)
                              .build())
                    .switchIfEmpty(Mono.just(ApiResponse.<UserResponse>builder()
                              .success(false)
                              .message("Usuario no encontrado")
                              .build()));
     }

     @Override
     public Mono<ApiResponse<UserResponse>> getUserByCode(String userCode) {
          return userRepository.findByUserCodeAndDeletedAtIsNull(userCode)
                    .map(this::mapToUserResponse)
                    .map(userResponse -> ApiResponse.<UserResponse>builder()
                              .data(userResponse)
                              .message("Usuario encontrado")
                              .success(true)
                              .build())
                    .switchIfEmpty(Mono.just(ApiResponse.<UserResponse>builder()
                              .success(false)
                              .message("Usuario no encontrado")
                              .build()));
     }

     @Override
     public Mono<ApiResponse<UserResponse>> getUserByUsername(String username) {
          log.info("🔍 Buscando usuario por username: {}", username);

          return userRepository.findByUsernameAndDeletedAtIsNull(username)
                    .map(this::mapToUserResponse)
                    .map(userResponse -> {
                         log.info(" Usuario encontrado por username: {} -> {}", username, userResponse.getUserCode());
                         return ApiResponse.<UserResponse>builder()
                                   .data(userResponse)
                                   .message("Usuario encontrado por username")
                                   .success(true)
                                   .build();
                    })
                    .switchIfEmpty(Mono.fromCallable(() -> {
                         log.warn(" Usuario NO encontrado por username: {}", username);
                         return ApiResponse.<UserResponse>builder()
                                   .success(false)
                                   .message("Usuario no encontrado por username: " + username)
                                   .build();
                    }));
     }

     @Override
     public Mono<ApiResponse<Page<UserResponse>>> getUsersByOrganization(String organizationId, Pageable pageable) {
          return userRepository.findByOrganizationIdAndDeletedAtIsNull(organizationId, pageable)
                    .collectList()
                    .zipWith(userRepository.countByOrganizationIdAndDeletedAtIsNull(organizationId))
                    .map(tuple -> {
                         List<UserResponse> users = tuple.getT1().stream()
                                   .map(this::mapToUserResponse)
                                   .toList();

                         Page<UserResponse> page = new PageImpl<>(users, pageable, tuple.getT2());

                         return ApiResponse.<Page<UserResponse>>builder()
                                   .data(page)
                                   .message("Usuarios obtenidos exitosamente")
                                   .success(true)
                                   .build();
                    });
     }

     @Override
     public Mono<ApiResponse<List<UserResponse>>> getUsersByRole(String organizationId, RolesUsers role) {
          return userRepository.findByOrganizationIdAndRoleAndDeletedAtIsNull(organizationId, role)
                    .map(this::mapToUserResponse)
                    .collectList()
                    .map(users -> ApiResponse.<List<UserResponse>>builder()
                              .data(users)
                              .message("Usuarios obtenidos exitosamente")
                              .success(true)
                              .build());
     }

     @Override
     public Mono<ApiResponse<UserResponse>> updateUser(String id, UpdateUserRequest request) {
          return userRepository.findByIdAndDeletedAtIsNull(id)
                    .flatMap(user -> {
                         updateUserFields(user, request);
                         return userRepository.save(user);
                    })
                    .map(this::mapToUserResponse)
                    .map(userResponse -> ApiResponse.<UserResponse>builder()
                              .data(userResponse)
                              .message("Usuario actualizado exitosamente")
                              .success(true)
                              .build())
                    .switchIfEmpty(Mono.just(ApiResponse.<UserResponse>builder()
                              .success(false)
                              .message("Usuario no encontrado")
                              .build()));
     }

     @Override
     public Mono<ApiResponse<UserResponse>> patchUser(String id, UpdateUserPatchRequest request) {
          return userRepository.findByIdAndDeletedAtIsNull(id)
                    .flatMap(user -> {
                         updateUserPatchFields(user, request);
                         return userRepository.save(user);
                    })
                    .map(this::mapToUserResponse)
                    .map(userResponse -> ApiResponse.<UserResponse>builder()
                              .data(userResponse)
                              .message("Usuario actualizado parcialmente exitosamente")
                              .success(true)
                              .build())
                    .switchIfEmpty(Mono.just(ApiResponse.<UserResponse>builder()
                              .success(false)
                              .message("Usuario no encontrado")
                              .build()));
     }

     @Override
     public Mono<ApiResponse<Void>> deleteUser(String id) {
          return userRepository.findByIdAndDeletedAtIsNull(id)
                    .flatMap(user -> {
                         user.setDeletedAt(LocalDateTime.now());
                         user.setUpdatedAt(LocalDateTime.now());
                         user.setStatus(UserStatus.INACTIVE);
                         return userRepository.save(user);
                    })
                    .then(Mono.just(ApiResponse.<Void>builder()
                              .message("Usuario eliminado exitosamente")
                              .success(true)
                              .build()))
                    .switchIfEmpty(Mono.just(ApiResponse.<Void>builder()
                              .success(false)
                              .message("Usuario no encontrado")
                              .build()));
     }

     @Override
     public Mono<ApiResponse<UserResponse>> changeUserStatus(String id, UserStatus status) {
          return userRepository.findByIdAndDeletedAtIsNull(id)
                    .flatMap(user -> {
                         user.setStatus(status);
                         user.setUpdatedAt(LocalDateTime.now());

                         if (status == UserStatus.INACTIVE) {
                              user.setDeletedAt(LocalDateTime.now());
                         } else if (status == UserStatus.ACTIVE) {
                              user.setDeletedAt(null);
                         }

                         return userRepository.save(user);
                    })
                    .map(this::mapToUserResponse)
                    .map(userResponse -> ApiResponse.<UserResponse>builder()
                              .data(userResponse)
                              .message("Estado de usuario actualizado exitosamente")
                              .success(true)
                              .build())
                    .switchIfEmpty(Mono.just(ApiResponse.<UserResponse>builder()
                              .success(false)
                              .message("Usuario no encontrado")
                              .build()));
     }

     @Override
     public Mono<ApiResponse<Void>> deleteUserPermanently(String id) {
          log.info("Eliminando usuario permanentemente (hard delete): {}", id);

          return userRepository.findById(id)
                    .switchIfEmpty(Mono.error(new RuntimeException("Usuario no encontrado")))
                    .flatMap(user -> userRepository.deleteById(id))
                    .then(Mono.just(ApiResponse.<Void>builder()
                              .success(true)
                              .message("Usuario eliminado permanentemente")
                              .build()))
                    .onErrorReturn(ApiResponse.<Void>builder()
                              .success(false)
                              .message("Error al eliminar usuario permanentemente")
                              .build());
     }

     @Override
     public Mono<ApiResponse<UserResponse>> restoreUser(String id) {
          log.info("Restaurando usuario: {}", id);

          return userRepository.findById(id)
                    .switchIfEmpty(Mono.error(new RuntimeException("Usuario no encontrado")))
                    .flatMap(user -> {
                         user.setStatus(UserStatus.ACTIVE);
                         user.setUpdatedAt(LocalDateTime.now());
                         user.setDeletedAt(null);

                         return userRepository.save(user);
                    })
                    .map(this::mapToUserResponse)
                    .map(userResponse -> ApiResponse.<UserResponse>builder()
                              .success(true)
                              .message("Usuario restaurado correctamente")
                              .data(userResponse)
                              .build())
                    .onErrorReturn(ApiResponse.<UserResponse>builder()
                              .success(false)
                              .message("Error al restaurar usuario")
                              .build());
     }

     @Override
     public Mono<ApiResponse<List<UserResponse>>> getActiveUsersByOrganization(String organizationId) {
          log.info("Obteniendo usuarios activos de la organización: {}", organizationId);

          return userRepository.findByOrganizationIdAndStatusAndDeletedAtIsNull(organizationId, UserStatus.ACTIVE)
                    .map(this::mapToUserResponse)
                    .collectList()
                    .map(users -> ApiResponse.<List<UserResponse>>builder()
                              .success(true)
                              .message("Usuarios activos obtenidos correctamente")
                              .data(users)
                              .build())
                    .onErrorReturn(ApiResponse.<List<UserResponse>>builder()
                              .success(false)
                              .message("Error al obtener usuarios activos")
                              .build());
     }

     @Override
     public Mono<ApiResponse<List<UserResponse>>> getInactiveUsersByOrganization(String organizationId) {
          log.info("Obteniendo usuarios inactivos de la organización: {}", organizationId);

          return userRepository.findByOrganizationIdAndStatusAndDeletedAtIsNull(organizationId, UserStatus.INACTIVE)
                    .map(this::mapToUserResponse)
                    .collectList()
                    .map(users -> ApiResponse.<List<UserResponse>>builder()
                              .success(true)
                              .message("Usuarios inactivos obtenidos correctamente")
                              .data(users)
                              .build())
                    .onErrorReturn(ApiResponse.<List<UserResponse>>builder()
                              .success(false)
                              .message("Error al obtener usuarios inactivos")
                              .build());
     }

     @Override
     public Mono<ApiResponse<List<UserResponse>>> getAllUsersByOrganization(String organizationId) {
          log.info("Obteniendo TODOS los usuarios de la organización (activos e inactivos): {}", organizationId);

          // Cambiar para obtener TODOS los usuarios sin importar deletedAt
          return userRepository.findByOrganizationId(organizationId)
                    .map(this::mapToUserResponse)
                    .collectList()
                    .map(users -> {
                         log.info("Se encontraron {} usuarios en total para la organización {}", users.size(),
                                   organizationId);
                         return ApiResponse.<List<UserResponse>>builder()
                                   .success(true)
                                   .message("Todos los usuarios (activos e inactivos) obtenidos correctamente")
                                   .data(users)
                                   .build();
                    })
                    .onErrorResume(error -> {
                         log.error("ERROR OBTENIENDO USUARIOS: ", error);
                         return Mono.just(ApiResponse.<List<UserResponse>>builder()
                                   .success(false)
                                   .message("Error al obtener usuarios: " + error.getMessage())
                                   .build());
                    });
     }

     @Override
     public Mono<ApiResponse<Boolean>> getUserByEmail(String email) {
          return userRepository.existsByContactEmailAndDeletedAtIsNull(email)
                    .map(exists -> ApiResponse.<Boolean>builder()
                              .data(exists) // Retornamos true si existe
                              .message("Verificación de email completada")
                              .success(true)
                              .build())
                    .onErrorReturn(ApiResponse.<Boolean>builder()
                              .data(false)
                              .message("Error al verificar email")
                              .success(false)
                              .build());
     }

     @Override
     public Mono<ApiResponse<Boolean>> getUserByDocumentNumber(String documentNumber) {
          return userRepository.existsByPersonalInfoDocumentNumberAndDeletedAtIsNull(documentNumber)
                    .map(exists -> ApiResponse.<Boolean>builder()
                              .data(exists) // Retornamos true si existe
                              .message("Verificación de DNI completada")
                              .success(true)
                              .build())
                    .onErrorReturn(ApiResponse.<Boolean>builder()
                              .data(false)
                              .message("Error al verificar DNI")
                              .success(false)
                              .build());
     }

     @Override
     public Mono<ApiResponse<Boolean>> getUserByPhone(String phone) {
          return userRepository.existsByContactPhoneAndDeletedAtIsNull(phone)
                    .map(exists -> ApiResponse.<Boolean>builder()
                              .data(exists) // Retornamos true si existe
                              .message("Verificación de teléfono completada")
                              .success(true)
                              .build())
                    .onErrorReturn(ApiResponse.<Boolean>builder()
                              .data(false)
                              .message("Error al verificar teléfono")
                              .success(false)
                              .build());
     }

     // Métodos privados auxiliares

     private Mono<Void> validateCreateRequest(CreateUserRequest request) {
          return userRepository.existsByPersonalInfoDocumentNumberAndDeletedAtIsNull(request.getDocumentNumber())
                    .flatMap(exists -> {
                         if (exists) {
                              return Mono.error(new IllegalArgumentException(
                                        "Ya existe un usuario con este número de documento"));
                         }
                         return Mono.empty();
                    })
                    .then(userRepository.existsByContactEmailAndDeletedAtIsNull(request.getEmail()))
                    .flatMap(exists -> {
                         if (exists) {
                              return Mono.error(new IllegalArgumentException("Ya existe un usuario con este email"));
                         }
                         return Mono.empty();
                    });
     }

     private void updateUserFields(User user, UpdateUserRequest request) {
          if (request.getFirstName() != null) {
               user.getPersonalInfo().setFirstName(request.getFirstName());
          }
          if (request.getLastName() != null) {
               user.getPersonalInfo().setLastName(request.getLastName());
          }
          if (request.getEmail() != null) {
               user.getContact().setEmail(request.getEmail());
          }
          if (request.getPhone() != null) {
               user.getContact().setPhone(request.getPhone());
          }
          if (request.getAddress() != null) {
               if (user.getContact().getAddress() == null) {
                    user.getContact().setAddress(AddressUsers.builder().build());
               }
               user.getContact().getAddress().setFullAddress(request.getAddress());
          }
          if (request.getRoles() != null && !request.getRoles().isEmpty()) {
               user.setRoles(request.getRoles());
          }
          user.setUpdatedAt(LocalDateTime.now());
     }

     private void updateUserPatchFields(User user, UpdateUserPatchRequest request) {
          if (request.getEmail() != null) {
               user.getContact().setEmail(request.getEmail());
          }
          if (request.getPhone() != null) {
               user.getContact().setPhone(request.getPhone());
          }

          // Manejar campo address (prioridad sobre streetAddress para consistencia con
          // frontend)
          String addressToUpdate = null;
          if (request.getAddress() != null) {
               addressToUpdate = request.getAddress();
          } else if (request.getStreetAddress() != null) {
               addressToUpdate = request.getStreetAddress();
          }

          if (addressToUpdate != null) {
               if (user.getContact().getAddress() == null) {
                    user.getContact().setAddress(AddressUsers.builder().build());
               }
               user.getContact().getAddress().setFullAddress(addressToUpdate);
               log.info("[SERVICE] Actualizando dirección del usuario {} a: {}", user.getUserCode(), addressToUpdate);
          }

          if (request.getStreetId() != null) {
               if (user.getContact().getAddress() == null) {
                    user.getContact().setAddress(AddressUsers.builder().build());
               }
               user.getContact().getAddress().setStreetId(request.getStreetId());
          }
          if (request.getZoneId() != null) {
               if (user.getContact().getAddress() == null) {
                    user.getContact().setAddress(AddressUsers.builder().build());
               }
               user.getContact().getAddress().setZoneId(request.getZoneId());
          }
          user.setUpdatedAt(LocalDateTime.now());
     }

     private UserResponse mapToUserResponse(User user) {
          return UserResponse.builder()
                    .id(user.getId())
                    .userCode(user.getUserCode())
                    .firstName(user.getPersonalInfo() != null ? user.getPersonalInfo().getFirstName() : null)
                    .lastName(user.getPersonalInfo() != null ? user.getPersonalInfo().getLastName() : null)
                    .documentType(user.getPersonalInfo() != null ? user.getPersonalInfo().getDocumentType() : null)
                    .documentNumber(user.getPersonalInfo() != null ? user.getPersonalInfo().getDocumentNumber() : null)
                    .email(user.getContact() != null ? user.getContact().getEmail() : null)
                    .phone(user.getContact() != null ? user.getContact().getPhone() : null)
                    .address(user.getContact() != null && user.getContact().getAddress() != null
                              ? user.getContact().getAddress().getFullAddress()
                              : null)
                    .organizationId(user.getOrganizationId())
                    .streetId(user.getContact() != null && user.getContact().getAddress() != null
                              ? user.getContact().getAddress().getStreetId()
                              : null)
                    .zoneId(user.getContact() != null && user.getContact().getAddress() != null
                              ? user.getContact().getAddress().getZoneId()
                              : null)
                    .roles(user.getRoles() != null ? user.getRoles() : Set.of())
                    .status(user.getStatus())
                    .createdAt(user.getCreatedAt())
                    .updatedAt(user.getUpdatedAt())
                    .build();
     }

     @Override
     public Mono<ApiResponse<Boolean>> getEmailAvailability(String email) {
          return userRepository.existsByContactEmailAndDeletedAtIsNull(email)
                    .map(exists -> ApiResponse.<Boolean>builder()
                              .data(!exists) // Retornamos true si no existe (disponible)
                              .message("Verificación de disponibilidad de email completada")
                              .success(true)
                              .build())
                    .onErrorReturn(ApiResponse.<Boolean>builder()
                              .data(false)
                              .message("Error al verificar disponibilidad del email")
                              .success(false)
                              .build());
     }

     @Override
     public Mono<Long> countSuperAdmins() {
          return userRepository.countByRolesContainingAndDeletedAtIsNull(RolesUsers.SUPER_ADMIN);
     }

     // ============================================================================
     // MÉTODOS PARA INFORMACIÓN COMPLETA (APIS INTERNAS CON JWE)
     // ============================================================================

     @Override
     public Mono<ApiResponse<List<CompleteUserResponse>>> getCompleteUsersByOrganization(String organizationId) {
          log.info("🔍 Obteniendo usuarios completos para organización: {}", organizationId);

          return userRepository.findByOrganizationIdAndDeletedAtIsNull(organizationId)
                    .collectList()
                    .flatMap(users -> {
                         if (users.isEmpty()) {
                              log.warn("⚠️ No se encontraron usuarios para la organización: {}", organizationId);
                              return Mono.just(ApiResponse
                                        .<List<CompleteUserResponse>>success("No se encontraron usuarios", List.of()));
                         }

                         return Mono.fromCallable(() -> users.stream()
                                   .map(this::mapToCompleteUserResponse)
                                   .toList())
                                   .map(completeUsers -> {
                                        log.info("✅ {} usuarios completos obtenidos", completeUsers.size());
                                        return ApiResponse.<List<CompleteUserResponse>>success(
                                                  "Usuarios completos obtenidos exitosamente",
                                                  completeUsers);
                                   });
                    })
                    .onErrorReturn(
                              ApiResponse.<List<CompleteUserResponse>>error("Error obteniendo usuarios completos"));
     }

     @Override
     public Mono<ApiResponse<List<CompleteUserResponse>>> getCompleteUsersByRole(String organizationId,
               RolesUsers role) {
          log.info("🔍 Obteniendo usuarios completos con rol {} para organización: {}", role, organizationId);

          return userRepository.findByOrganizationIdAndRoleAndDeletedAtIsNull(organizationId, role)
                    .collectList()
                    .flatMap(users -> {
                         if (users.isEmpty()) {
                              log.warn("⚠️ No se encontraron usuarios con rol {} para la organización: {}", role,
                                        organizationId);
                              return Mono.just(ApiResponse.<List<CompleteUserResponse>>success(
                                        "No se encontraron usuarios con el rol especificado",
                                        List.of()));
                         }

                         return Mono.fromCallable(() -> users.stream()
                                   .map(this::mapToCompleteUserResponse)
                                   .toList())
                                   .map(completeUsers -> {
                                        log.info("✅ {} usuarios completos con rol {} obtenidos", completeUsers.size(),
                                                  role);
                                        return ApiResponse.<List<CompleteUserResponse>>success(
                                                  "Usuarios completos obtenidos exitosamente",
                                                  completeUsers);
                                   });
                    })
                    .onErrorReturn(ApiResponse
                              .<List<CompleteUserResponse>>error("Error obteniendo usuarios completos por rol"));
     }

     @Override
     public Mono<ApiResponse<CompleteUserResponse>> getCompleteUserById(String userId) {
          log.info("🔍 Obteniendo información completa del usuario: {}", userId);

          return userRepository.findByIdAndDeletedAtIsNull(userId)
                    .map(this::mapToCompleteUserResponse)
                    .map(completeUser -> {
                         log.info("✅ Usuario completo obtenido: {}", completeUser.getUserCode());
                         return ApiResponse.success("Usuario completo obtenido exitosamente", completeUser);
                    })
                    .switchIfEmpty(Mono.just(ApiResponse.error("Usuario no encontrado", null)))
                    .onErrorReturn(ApiResponse.error("Error obteniendo usuario completo", null));
     }

     @Override
     public Mono<ApiResponse<CompleteUserResponse>> getCompleteUserByIdIncludingDeleted(String userId) {
          log.info("🔍 Obteniendo información completa del usuario (incluyendo eliminados): {}", userId);

          return userRepository.findById(userId) // Sin filtro de deletedAt
                    .map(this::mapToCompleteUserResponse)
                    .map(completeUser -> {
                         log.info("✅ Usuario completo obtenido (incluyendo eliminados): {}",
                                   completeUser.getUserCode());
                         return ApiResponse.success("Usuario completo obtenido exitosamente", completeUser);
                    })
                    .switchIfEmpty(Mono.just(ApiResponse.error("Usuario no encontrado", null)))
                    .onErrorReturn(ApiResponse.error("Error obteniendo usuario completo", null));
     }

     /**
      * Mapea un User a CompleteUserResponse con información completa de relaciones
      * TODO: Implementar lookup real de Organization, Zone y Street
      */
     private CompleteUserResponse mapToCompleteUserResponse(User user) {
          return CompleteUserResponse.builder()
                    .id(user.getId())
                    .userCode(user.getUserCode())
                    .firstName(user.getPersonalInfo() != null ? user.getPersonalInfo().getFirstName() : null)
                    .lastName(user.getPersonalInfo() != null ? user.getPersonalInfo().getLastName() : null)
                    .documentType(user.getPersonalInfo() != null ? user.getPersonalInfo().getDocumentType().toString()
                              : null)
                    .documentNumber(user.getPersonalInfo() != null ? user.getPersonalInfo().getDocumentNumber() : null)
                    .email(user.getContact() != null ? user.getContact().getEmail() : null)
                    .phone(user.getContact() != null ? user.getContact().getPhone() : null)
                    .address(user.getContact() != null && user.getContact().getAddress() != null
                              ? user.getContact().getAddress().getFullAddress()
                              : null)
                    .roles(user.getRoles() != null ? user.getRoles() : Set.of())
                    .status(user.getStatus())
                    .createdAt(user.getCreatedAt())
                    .updatedAt(user.getUpdatedAt())
                    // TODO: Por ahora devolvemos objetos mock, luego implementar lookup real
                    .organization(createMockOrganization(user.getOrganizationId()))
                    .zone(createMockZone(user.getContact()))
                    .street(createMockStreet(user.getContact()))
                    .build();
     }

     /**
      * Crea un objeto mock de organización
      * TODO: Reemplazar con lookup real al microservicio correspondiente
      */
     private Object createMockOrganization(String orgId) {
          return Map.of(
                    "organizationId", orgId != null ? orgId : "",
                    "organizationCode", "JASS001",
                    "organizationName", "JASS Rinconada de Conta",
                    "legalRepresentative", "Isael Fatama Godoy",
                    "address", "Av. Rinconada de Conta",
                    "phone", "987456454",
                    "status", "ACTIVE");
     }

     /**
      * Crea un objeto mock de zona
      * TODO: Reemplazar con lookup real al microservicio correspondiente
      */
     private Object createMockZone(Contact contact) {
          String zoneIdValue = contact != null && contact.getAddress() != null
                    ? contact.getAddress().getZoneId()
                    : null;

          return Map.of(
                    "zoneId", zoneIdValue != null ? zoneIdValue : "",
                    "zoneCode", "ZN0001",
                    "zoneName", "Zona Sur",
                    "description", "Zona sur Rinconada de Conta",
                    "status", "ACTIVE");
     }

     /**
      * Crea un objeto mock de calle
      * TODO: Reemplazar con lookup real al microservicio correspondiente
      */
     private Object createMockStreet(Contact contact) {
          String streetIdValue = contact != null && contact.getAddress() != null
                    ? contact.getAddress().getStreetId()
                    : null;

          return Map.of(
                    "streetId", streetIdValue != null ? streetIdValue : "",
                    "streetCode", "CAL006",
                    "streetName", "Calle Bajo",
                    "streetType", "Avenida",
                    "status", "ACTIVE");
     }
}
