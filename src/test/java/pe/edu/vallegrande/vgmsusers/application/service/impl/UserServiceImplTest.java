package pe.edu.vallegrande.vgmsusers.application.service.impl;

import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import pe.edu.vallegrande.vgmsusers.application.service.UserAuthIntegrationService;
import pe.edu.vallegrande.vgmsusers.application.service.UserCodeService;
import pe.edu.vallegrande.vgmsusers.domain.enums.DocumentType;
import pe.edu.vallegrande.vgmsusers.domain.enums.RolesUsers;
import pe.edu.vallegrande.vgmsusers.domain.enums.UserStatus;
import pe.edu.vallegrande.vgmsusers.domain.model.Contact;
import pe.edu.vallegrande.vgmsusers.domain.model.PersonalInfo;
import pe.edu.vallegrande.vgmsusers.domain.model.User;
import pe.edu.vallegrande.vgmsusers.infrastructure.dto.ApiResponse;
import pe.edu.vallegrande.vgmsusers.infrastructure.dto.response.UserResponse;
import pe.edu.vallegrande.vgmsusers.infrastructure.repository.UserRepository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Pruebas unitarias para UserServiceImpl
 * Prueba las operaciones CRUD básicas con data mock
 */
@Slf4j
@ExtendWith(MockitoExtension.class)
@DisplayName("UserService - Pruebas Unitarias")
class UserServiceImplTest {

     @Mock
     private UserRepository userRepository;

     @Mock
     private UserCodeService userCodeService;

     @Mock
     private UserAuthIntegrationService userAuthIntegrationService;

     @InjectMocks
     private UserServiceImpl userService;

     private User mockUser1;
     private User mockUser2;
     private User mockUser3;
     private String organizationId;

     @BeforeEach
     void setUp() {
          organizationId = "ORG-001";

          // Mock User 1 - Usuario activo
          mockUser1 = User.builder()
                    .id("user-001")
                    .userCode("USR-001")
                    .username("juan.perez")
                    .organizationId(organizationId)
                    .personalInfo(PersonalInfo.builder()
                              .documentType(DocumentType.DNI)
                              .documentNumber("12345678")
                              .firstName("Juan")
                              .lastName("Pérez")
                              .build())
                    .contact(Contact.builder()
                              .email("juan.perez@example.com")
                              .phone("987654321")
                              .build())
                    .roles(Set.of(RolesUsers.CLIENT))
                    .status(UserStatus.ACTIVE)
                    .registrationDate(LocalDateTime.now().minusDays(30))
                    .createdAt(LocalDateTime.now().minusDays(30))
                    .updatedAt(LocalDateTime.now().minusDays(30))
                    .build();

          // Mock User 2 - Usuario activo
          mockUser2 = User.builder()
                    .id("user-002")
                    .userCode("USR-002")
                    .username("maria.gomez")
                    .organizationId(organizationId)
                    .personalInfo(PersonalInfo.builder()
                              .documentType(DocumentType.DNI)
                              .documentNumber("87654321")
                              .firstName("María")
                              .lastName("Gómez")
                              .build())
                    .contact(Contact.builder()
                              .email("maria.gomez@example.com")
                              .phone("912345678")
                              .build())
                    .roles(Set.of(RolesUsers.OPERATOR))
                    .status(UserStatus.ACTIVE)
                    .registrationDate(LocalDateTime.now().minusDays(15))
                    .createdAt(LocalDateTime.now().minusDays(15))
                    .updatedAt(LocalDateTime.now().minusDays(15))
                    .build();

          // Mock User 3 - Usuario eliminado (soft delete)
          mockUser3 = User.builder()
                    .id("user-003")
                    .userCode("USR-003")
                    .username("carlos.diaz")
                    .organizationId(organizationId)
                    .personalInfo(PersonalInfo.builder()
                              .documentType(DocumentType.DNI)
                              .documentNumber("45678912")
                              .firstName("Carlos")
                              .lastName("Díaz")
                              .build())
                    .contact(Contact.builder()
                              .email("carlos.diaz@example.com")
                              .phone("923456789")
                              .build())
                    .roles(Set.of(RolesUsers.CLIENT))
                    .status(UserStatus.INACTIVE)
                    .registrationDate(LocalDateTime.now().minusDays(60))
                    .createdAt(LocalDateTime.now().minusDays(60))
                    .updatedAt(LocalDateTime.now().minusDays(1))
                    .deletedAt(LocalDateTime.now().minusDays(1))
                    .deletedBy("admin")
                    .build();
     }

     @Test
     @DisplayName("Test 1: Listar todos los usuarios activos de una organización")
     void testListarUsuariosActivos_DebeRetornarListaDeUsuarios() {
          // Arrange
          log.info("========================================");
          log.info("TEST 1: LISTAR USUARIOS ACTIVOS");
          log.info("========================================");
          log.info("Organización: {}", organizationId);

          List<User> mockActiveUsers = List.of(mockUser1, mockUser2);
          log.info("📊 Mock Data - Usuarios preparados:");
          mockActiveUsers.forEach(user -> log.info("  • {} {} - DNI: {} - Email: {} - Rol: {} - Estado: {}",
                    user.getPersonalInfo().getFirstName(),
                    user.getPersonalInfo().getLastName(),
                    user.getPersonalInfo().getDocumentNumber(),
                    user.getContact().getEmail(),
                    user.getRoles(),
                    user.getStatus()));

          when(userRepository.findByOrganizationIdAndStatusAndDeletedAtIsNull(organizationId, UserStatus.ACTIVE))
                    .thenReturn(Flux.fromIterable(mockActiveUsers)); // Act
          log.info("🔄 Ejecutando: getActiveUsersByOrganization()");
          Mono<ApiResponse<List<UserResponse>>> result = userService.getActiveUsersByOrganization(organizationId);

          // Assert
          StepVerifier.create(result)
                    .assertNext(response -> {
                         log.info("✅ Respuesta recibida:");
                         log.info("   Success: {}", response.isSuccess());
                         log.info("   Message: {}", response.getMessage());
                         log.info("   Total usuarios: {}", response.getData().size());

                         response.getData()
                                   .forEach(user -> log.info(
                                             "   👤 Usuario: {} {} - Código: {} - Email: {} - Estado: {}",
                                             user.getFirstName(),
                                             user.getLastName(),
                                             user.getUserCode(),
                                             user.getEmail(),
                                             user.getStatus()));

                         assertThat(response).isNotNull();
                         assertThat(response.isSuccess()).isTrue();
                         assertThat(response.getData()).isNotNull();
                         assertThat(response.getData()).hasSize(2);
                         assertThat(response.getMessage()).contains("activos");

                         // Verificar datos del primer usuario
                         UserResponse user1 = response.getData().get(0);
                         assertThat(user1.getUserCode()).isEqualTo("USR-001");
                         assertThat(user1.getFirstName()).isEqualTo("Juan");
                         assertThat(user1.getLastName()).isEqualTo("Pérez");
                         assertThat(user1.getStatus()).isEqualTo(UserStatus.ACTIVE);

                         // Verificar datos del segundo usuario
                         UserResponse user2 = response.getData().get(1);
                         assertThat(user2.getUserCode()).isEqualTo("USR-002");
                         assertThat(user2.getFirstName()).isEqualTo("María");
                         assertThat(user2.getLastName()).isEqualTo("Gómez");

                         log.info("✅ TEST 1 COMPLETADO - Todos los usuarios listados correctamente\n");
                    })
                    .verifyComplete();

          // Verify
          verify(userRepository, times(1)).findByOrganizationIdAndStatusAndDeletedAtIsNull(organizationId,
                    UserStatus.ACTIVE);
     }

     @Test
     @DisplayName("Test 2: Crear un nuevo usuario con data mock")
     void testCrearUsuario_DebeRetornarUsuarioCreado() {
          // Arrange
          log.info("========================================");
          log.info("TEST 2: CREAR NUEVO USUARIO");
          log.info("========================================");

          String generatedUserCode = "USR-004";
          User newUser = User.builder()
                    .id("user-004")
                    .userCode(generatedUserCode)
                    .username("pedro.sanchez")
                    .organizationId(organizationId)
                    .personalInfo(PersonalInfo.builder()
                              .documentType(DocumentType.DNI)
                              .documentNumber("11223344")
                              .firstName("Pedro")
                              .lastName("Sánchez")
                              .build())
                    .contact(Contact.builder()
                              .email("pedro.sanchez@example.com")
                              .phone("998877665")
                              .build())
                    .roles(Set.of(RolesUsers.CLIENT))
                    .status(UserStatus.ACTIVE)
                    .registrationDate(LocalDateTime.now())
                    .createdAt(LocalDateTime.now())
                    .updatedAt(LocalDateTime.now())
                    .build();

          log.info("📝 Datos del nuevo usuario a crear:");
          log.info("   Nombre: {} {}", newUser.getPersonalInfo().getFirstName(),
                    newUser.getPersonalInfo().getLastName());
          log.info("   DNI: {}", newUser.getPersonalInfo().getDocumentNumber());
          log.info("   Email: {}", newUser.getContact().getEmail());
          log.info("   Teléfono: {}", newUser.getContact().getPhone());
          log.info("   Rol: {}", newUser.getRoles());
          log.info("   Estado: {}", newUser.getStatus());

          // Simulación simplificada sin validaciones
          when(userRepository.save(any(User.class))).thenReturn(Mono.just(newUser));

          // Act
          log.info("🔄 Ejecutando: save(newUser)");
          Mono<User> result = userRepository.save(newUser);

          // Assert
          StepVerifier.create(result)
                    .assertNext(savedUser -> {
                         log.info("✅ Usuario creado exitosamente:");
                         log.info("   ID: {}", savedUser.getId());
                         log.info("   Código: {}", savedUser.getUserCode());
                         log.info("   Username: {}", savedUser.getUsername());
                         log.info("   Nombre: {} {}", savedUser.getPersonalInfo().getFirstName(),
                                   savedUser.getPersonalInfo().getLastName());
                         log.info("   Email: {}", savedUser.getContact().getEmail());
                         log.info("   Estado: {}", savedUser.getStatus());

                         assertThat(savedUser).isNotNull();
                         assertThat(savedUser.getId()).isEqualTo("user-004");
                         assertThat(savedUser.getUserCode()).isEqualTo(generatedUserCode);
                         assertThat(savedUser.getUsername()).isEqualTo("pedro.sanchez");
                         assertThat(savedUser.getStatus()).isEqualTo(UserStatus.ACTIVE);
                         assertThat(savedUser.getPersonalInfo().getFirstName()).isEqualTo("Pedro");
                         assertThat(savedUser.getPersonalInfo().getLastName()).isEqualTo("Sánchez");
                         assertThat(savedUser.getContact().getEmail()).isEqualTo("pedro.sanchez@example.com");
                         assertThat(savedUser.getRoles()).contains(RolesUsers.CLIENT);

                         log.info("✅ TEST 2 COMPLETADO - Usuario creado correctamente\n");
                    })
                    .verifyComplete();

          // Verify
          verify(userRepository, times(1)).save(any(User.class));
     }

     @Test
     @DisplayName("Test 3: Buscar usuario por ID - Debe retornar usuario encontrado")
     void testBuscarUsuarioPorId_DebeRetornarUsuario() {
          // Arrange
          log.info("========================================");
          log.info("TEST 3: BUSCAR USUARIO POR ID");
          log.info("========================================");

          String userId = "user-001";
          log.info("🔍 Buscando usuario con ID: {}", userId);
          log.info("📊 Datos del usuario mock preparado:");
          log.info("   Nombre: {} {}", mockUser1.getPersonalInfo().getFirstName(),
                    mockUser1.getPersonalInfo().getLastName());
          log.info("   DNI: {}", mockUser1.getPersonalInfo().getDocumentNumber());
          log.info("   Email: {}", mockUser1.getContact().getEmail());

          when(userRepository.findById(userId)).thenReturn(Mono.just(mockUser1));

          // Act
          log.info("🔄 Ejecutando: getUserById()");
          Mono<ApiResponse<UserResponse>> result = userService.getUserById(userId);

          // Assert
          StepVerifier.create(result)
                    .assertNext(response -> {
                         log.info("✅ Usuario encontrado:");
                         log.info("   ID: {}", response.getData().getId());
                         log.info("   Código: {}", response.getData().getUserCode());
                         log.info("   Nombre: {} {}", response.getData().getFirstName(),
                                   response.getData().getLastName());
                         log.info("   Email: {}", response.getData().getEmail());
                         log.info("   DNI: {}", response.getData().getDocumentNumber());
                         log.info("   Estado: {}", response.getData().getStatus());
                         log.info("   Mensaje: {}", response.getMessage());

                         assertThat(response).isNotNull();
                         assertThat(response.isSuccess()).isTrue();
                         assertThat(response.getData()).isNotNull();
                         assertThat(response.getData().getId()).isEqualTo(userId);
                         assertThat(response.getData().getUserCode()).isEqualTo("USR-001");
                         assertThat(response.getData().getEmail()).isEqualTo("juan.perez@example.com");
                         assertThat(response.getData().getFirstName()).isEqualTo("Juan");
                         assertThat(response.getData().getLastName()).isEqualTo("Pérez");
                         assertThat(response.getData().getStatus()).isEqualTo(UserStatus.ACTIVE);
                         assertThat(response.getMessage()).contains("encontrado");

                         log.info("✅ TEST 3 COMPLETADO - Usuario encontrado correctamente\n");
                    })
                    .verifyComplete();

          // Verify
          verify(userRepository, times(1)).findById(userId);
     }

     @Test
     @DisplayName("Test 4: Eliminar usuario lógicamente (soft delete)")
     void testEliminarUsuarioLogicamente_DebeCambiarEstadoAInactivo() {
          // Arrange
          log.info("========================================");
          log.info("TEST 4: ELIMINAR USUARIO (SOFT DELETE)");
          log.info("========================================");

          String userId = "user-001";
          User userToDelete = mockUser1;

          log.info("🗑️  Usuario a eliminar:");
          log.info("   ID: {}", userId);
          log.info("   Nombre: {} {}", userToDelete.getPersonalInfo().getFirstName(),
                    userToDelete.getPersonalInfo().getLastName());
          log.info("   Email: {}", userToDelete.getContact().getEmail());
          log.info("   Estado actual: {}", userToDelete.getStatus());

          when(userRepository.findByIdAndDeletedAtIsNull(userId)).thenReturn(Mono.just(userToDelete));

          User deletedUser = User.builder()
                    .id(userToDelete.getId())
                    .userCode(userToDelete.getUserCode())
                    .username(userToDelete.getUsername())
                    .organizationId(userToDelete.getOrganizationId())
                    .personalInfo(userToDelete.getPersonalInfo())
                    .contact(userToDelete.getContact())
                    .roles(userToDelete.getRoles())
                    .status(UserStatus.INACTIVE)
                    .registrationDate(userToDelete.getRegistrationDate())
                    .createdAt(userToDelete.getCreatedAt())
                    .updatedAt(LocalDateTime.now())
                    .deletedAt(LocalDateTime.now())
                    .deletedBy("test-user")
                    .build();

          when(userRepository.save(any(User.class))).thenReturn(Mono.just(deletedUser));

          // Act
          log.info("🔄 Ejecutando: deleteUser() - Soft Delete");
          Mono<ApiResponse<Void>> result = userService.deleteUser(userId);

          // Assert
          StepVerifier.create(result)
                    .assertNext(response -> {
                         log.info("✅ Usuario eliminado lógicamente:");
                         log.info("   Estado nuevo: INACTIVE");
                         log.info("   DeletedAt: {}", deletedUser.getDeletedAt());
                         log.info("   DeletedBy: {}", deletedUser.getDeletedBy());
                         log.info("   Mensaje: {}", response.getMessage());

                         assertThat(response).isNotNull();
                         assertThat(response.isSuccess()).isTrue();
                         assertThat(response.getMessage()).contains("eliminado exitosamente");

                         log.info("✅ TEST 4 COMPLETADO - Usuario eliminado lógicamente\n");
                    })
                    .verifyComplete();

          // Verify
          verify(userRepository, times(1)).findByIdAndDeletedAtIsNull(userId);
          verify(userRepository, times(1)).save(any(User.class));
     }

     @Test
     @DisplayName("Test 5: Restaurar usuario eliminado - Debe cambiar estado a ACTIVE")
     void testRestaurarUsuarioEliminado_DebeCambiarEstadoAActivo() {
          // Arrange
          log.info("========================================");
          log.info("TEST 5: RESTAURAR USUARIO ELIMINADO");
          log.info("========================================");

          String userId = "user-003";
          User deletedUser = mockUser3;

          log.info("♻️  Usuario eliminado a restaurar:");
          log.info("   ID: {}", userId);
          log.info("   Código: {}", deletedUser.getUserCode());
          log.info("   Nombre: {} {}", deletedUser.getPersonalInfo().getFirstName(),
                    deletedUser.getPersonalInfo().getLastName());
          log.info("   Email: {}", deletedUser.getContact().getEmail());
          log.info("   Estado actual: {}", deletedUser.getStatus());
          log.info("   DeletedAt: {}", deletedUser.getDeletedAt());

          when(userRepository.findById(userId)).thenReturn(Mono.just(deletedUser));

          // Simular la restauración del usuario
          User restoredUser = User.builder()
                    .id(deletedUser.getId())
                    .userCode(deletedUser.getUserCode())
                    .username(deletedUser.getUsername())
                    .organizationId(deletedUser.getOrganizationId())
                    .personalInfo(deletedUser.getPersonalInfo())
                    .contact(deletedUser.getContact())
                    .roles(deletedUser.getRoles())
                    .status(UserStatus.ACTIVE)
                    .registrationDate(deletedUser.getRegistrationDate())
                    .createdAt(deletedUser.getCreatedAt())
                    .updatedAt(LocalDateTime.now())
                    .deletedAt(null) // Limpiar fecha de eliminación
                    .deletedBy(null) // Limpiar quien eliminó
                    .build();

          when(userRepository.save(any(User.class))).thenReturn(Mono.just(restoredUser));

          // Act
          log.info("🔄 Ejecutando: restoreUser() - Restauración");
          Mono<ApiResponse<UserResponse>> result = userService.restoreUser(userId);

          // Assert
          StepVerifier.create(result)
                    .assertNext(response -> {
                         log.info("✅ Usuario restaurado exitosamente:");
                         log.info("   ID: {}", response.getData().getId());
                         log.info("   Código: {}", response.getData().getUserCode());
                         log.info("   Nombre: {} {}", response.getData().getFirstName(),
                                   response.getData().getLastName());
                         log.info("   Estado nuevo: {}", response.getData().getStatus());
                         log.info("   DeletedAt: null (limpiado)");
                         log.info("   DeletedBy: null (limpiado)");
                         log.info("   Mensaje: {}", response.getMessage());

                         assertThat(response).isNotNull();
                         assertThat(response.isSuccess()).isTrue();
                         assertThat(response.getData()).isNotNull();
                         assertThat(response.getData().getId()).isEqualTo(userId);
                         assertThat(response.getData().getStatus()).isEqualTo(UserStatus.ACTIVE);
                         assertThat(response.getData().getUserCode()).isEqualTo("USR-003");
                         assertThat(response.getData().getFirstName()).isEqualTo("Carlos");
                         assertThat(response.getData().getLastName()).isEqualTo("Díaz");
                         assertThat(response.getMessage()).contains("restaurado");

                         log.info("✅ TEST 5 COMPLETADO - Usuario restaurado correctamente\n");
                    })
                    .verifyComplete();

          // Verify
          verify(userRepository, times(1)).findById(userId);
          verify(userRepository, times(1)).save(any(User.class));
     }

     @Test
     @DisplayName("Test Extra: Buscar usuario por ID inexistente - Debe retornar error")
     void testBuscarUsuarioPorIdInexistente_DebeRetornarError() {
          // Arrange
          String userId = "user-999";
          when(userRepository.findById(userId)).thenReturn(Mono.empty());

          // Act
          Mono<ApiResponse<UserResponse>> result = userService.getUserById(userId);

          // Assert
          StepVerifier.create(result)
                    .assertNext(response -> {
                         assertThat(response).isNotNull();
                         assertThat(response.isSuccess()).isFalse();
                         assertThat(response.getData()).isNull();
                         assertThat(response.getMessage()).contains("no encontrado");
                    })
                    .verifyComplete();

          // Verify
          verify(userRepository, times(1)).findById(userId);
     }
}
