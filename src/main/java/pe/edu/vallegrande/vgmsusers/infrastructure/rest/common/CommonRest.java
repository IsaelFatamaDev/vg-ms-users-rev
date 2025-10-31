package pe.edu.vallegrande.vgmsusers.infrastructure.rest.common;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;
import pe.edu.vallegrande.vgmsusers.application.service.UserService;
import pe.edu.vallegrande.vgmsusers.domain.enums.RolesUsers;
import pe.edu.vallegrande.vgmsusers.infrastructure.dto.ApiResponse;
import pe.edu.vallegrande.vgmsusers.infrastructure.dto.ReniecResponseDto;
import pe.edu.vallegrande.vgmsusers.infrastructure.dto.request.CreateFirstUserRequest;
import pe.edu.vallegrande.vgmsusers.infrastructure.dto.request.CreateUserRequest;
import pe.edu.vallegrande.vgmsusers.infrastructure.dto.response.RoleInfoResponse;
import pe.edu.vallegrande.vgmsusers.infrastructure.dto.response.UserBasicInfoResponse;
import pe.edu.vallegrande.vgmsusers.infrastructure.dto.response.UserCreationResponse;
import pe.edu.vallegrande.vgmsusers.infrastructure.dto.response.UserResponse;
import pe.edu.vallegrande.vgmsusers.infrastructure.exception.NotFoundException;
import pe.edu.vallegrande.vgmsusers.infrastructure.client.ReniecClient;
import reactor.core.publisher.Mono;

import java.util.Arrays;
import java.util.List;
import java.util.Set;

/**
 * REST Controller para funciones comunes y públicas
 * Endpoints accesibles por cualquier usuario autenticado o públicos
 */
@Slf4j
@RestController
@RequestMapping("/api/common")
@RequiredArgsConstructor
public class CommonRest {

        private final UserService userService;
        private final ReniecClient reniecClient;

        /**
         * Health check del microservicio
         * GET /api/common/health
         */
        @GetMapping("/health")
        public Mono<ApiResponse<String>> healthCheck() {
                return Mono.just(ApiResponse.success("MS-USERS funcionando correctamente",
                                "Microservicio de Usuarios - v2.0.0"));
        }

        /**
         * Obtener información básica de usuario por código (sin datos sensibles)
         * GET /api/common/user/code/{userCode}/basic
         */
        @GetMapping("/user/code/{userCode}/basic")
        public Mono<ApiResponse<UserBasicInfoResponse>> getUserBasicInfo(@PathVariable String userCode) {
                log.info("Consultando información básica del usuario con código: {}", userCode);

                return userService.getUserByCode(userCode)
                                .flatMap(response -> {
                                        if (response.isSuccess()) {
                                                UserResponse user = response.getData();

                                                UserBasicInfoResponse basicInfo = UserBasicInfoResponse.builder()
                                                                .userCode(user.getUserCode())
                                                                .firstName(user.getFirstName())
                                                                .lastName(user.getLastName())
                                                                .status(user.getStatus().toString())
                                                                .organizationId(user.getOrganizationId())
                                                                .build();

                                                return Mono.just(ApiResponse.success("Información básica obtenida",
                                                                basicInfo));
                                        } else {
                                                return Mono.error(new NotFoundException("Usuario no encontrado"));
                                        }
                                });
        }

        /**
         * Verificar si un código de usuario existe
         * GET /api/common/user/code/{userCode}/exists
         */
        @GetMapping("/user/code/{userCode}/exists")
        public Mono<ApiResponse<Boolean>> checkUserCodeExists(@PathVariable String userCode) {
                return userService.getUserByCode(userCode)
                                .map(response -> ApiResponse.success("Verificación completada", response.isSuccess()))
                                .onErrorReturn(ApiResponse.success("Verificación completada", false));
        }

        /**
         * Verificar si un email está disponible
         * GET /api/common/user/email/{email}/available
         */
        @GetMapping("/user/email/{email}/available")
        public Mono<ApiResponse<Boolean>> checkEmailAvailable(@PathVariable String email) {
                return userService.getUserByEmail(email)
                                .map(response -> {
                                        if (response.isSuccess()) {
                                                return ApiResponse.success("Verificación de email completada",
                                                                response.getData());
                                        } else {
                                                return ApiResponse.success("Verificación de email completada", false);
                                        }
                                })
                                .onErrorReturn(ApiResponse.success("Verificación de email completada", false));
        }

        /**
         * NUEVO: Obtener usuario por username (para MS-AUTHENTICATION)
         * GET /api/common/user/username/{username}
         */
        @GetMapping("/user/username/{username}")
        public Mono<ApiResponse<UserResponse>> getUserByUsername(@PathVariable String username) {
                log.info("🔍 MS-AUTHENTICATION consultando usuario por username: {}", username);

                return userService.getUserByUsername(username)
                                .doOnSuccess(response -> {
                                        if (response.isSuccess()) {
                                                log.info(" Usuario encontrado por username: {}", username);
                                        } else {
                                                log.warn(" Usuario NO encontrado por username: {}", username);
                                        }
                                })
                                .doOnError(error -> log.error("💥 Error buscando usuario por username {}: {}",
                                                username, error.getMessage()));
        }

        /**
         * Obtener información sobre los roles disponibles
         * GET /api/common/roles
         */
        @GetMapping("/roles")
        public Mono<ApiResponse<List<RoleInfoResponse>>> getAvailableRoles() {
                List<RoleInfoResponse> roles = Arrays.asList(
                                RoleInfoResponse.builder()
                                                .name("SUPER_ADMIN")
                                                .description("Administrador supremo del sistema")
                                                .permissions(Arrays.asList("manage_admins", "manage_all_users",
                                                                "system_config"))
                                                .build(),
                                RoleInfoResponse.builder()
                                                .name("ADMIN")
                                                .description("Administrador de organización")
                                                .permissions(Arrays.asList("manage_clients", "view_org_users",
                                                                "generate_codes"))
                                                .build(),
                                RoleInfoResponse.builder()
                                                .name("CLIENT")
                                                .description("Usuario final del sistema")
                                                .permissions(Arrays.asList("view_profile", "update_profile"))
                                                .build());

                return Mono.just(ApiResponse.success("Información de roles obtenida", roles));
        }

        /**
         * NUEVO: Crear primer usuario del sistema (SUPER_ADMIN)
         * POST /api/common/setup/first-user
         * Este endpoint es público para configuración inicial del sistema
         */
        @PostMapping("/setup/first-user")
        public Mono<ApiResponse<UserCreationResponse>> createFirstUser(@RequestBody CreateFirstUserRequest request) {
                log.info("🚀 Creando primer usuario del sistema: {}", request.getEmail());

                // Verificar si ya existe algún SUPER_ADMIN
                return userService.countSuperAdmins()
                                .flatMap(count -> {
                                        if (count > 0) {
                                                log.warn("⚠️ Ya existe un SUPER_ADMIN en el sistema");
                                                return Mono.just(ApiResponse.<UserCreationResponse>error(
                                                                "Ya existe un administrador supremo en el sistema"));
                                        }

                                        // Crear el primer usuario como SUPER_ADMIN
                                        CreateUserRequest createRequest = CreateUserRequest.builder()
                                                        .firstName(request.getFirstName())
                                                        .lastName(request.getLastName())
                                                        .documentType(request.getDocumentType())
                                                        .documentNumber(request.getDocumentNumber())
                                                        .email(request.getEmail())
                                                        .phone(request.getPhone())
                                                        .address(request.getAddress())
                                                        .organizationId(request.getOrganizationId())
                                                        .roles(Set.of(RolesUsers.SUPER_ADMIN))
                                                        .build();

                                        return userService.createUserWithCredentials(createRequest)
                                                        .map(response -> {
                                                                if (response.isSuccess()) {
                                                                        UserCreationResponse creationData = response
                                                                                        .getData();
                                                                        log.info(" Primer usuario creado exitosamente: {} con username: {}",
                                                                                        creationData.getUserInfo()
                                                                                                        .getUserCode(),
                                                                                        creationData.getUsername());

                                                                        // Crear respuesta completa con credenciales
                                                                        return ApiResponse.success(
                                                                                        "Primer usuario del sistema creado exitosamente. Username: "
                                                                                                        +
                                                                                                        creationData.getUsername()
                                                                                                        + ", Contraseña temporal: "
                                                                                                        +
                                                                                                        creationData.getTemporaryPassword(),
                                                                                        creationData); // Devolver
                                                                                                       // UserCreationResponse
                                                                                                       // completo
                                                                } else {
                                                                        return ApiResponse.<UserCreationResponse>error(
                                                                                        response.getMessage());
                                                                }
                                                        });
                                })
                                .onErrorResume(error -> {
                                        log.error("💥 Error creando primer usuario: {}", error.getMessage());
                                        return Mono.just(ApiResponse.<UserCreationResponse>error(
                                                        "Error creando primer usuario: " + error.getMessage()));
                                });
        }

        /**
         * Endpoint para validar conectividad desde otros microservicios
         */
        @GetMapping("/ping")
        public Mono<String> ping() {
                return Mono.just("pong");
        }

        /**
         * NUEVO: Consulta de RENIEC por DNI (endpoint público)
         * GET /api/common/users/reniec/dni/{dni}
         * Este endpoint permite consultar datos de RENIEC sin autenticación
         */
        @GetMapping("/users/reniec/dni/{dni}")
        public Mono<ApiResponse<ReniecResponseDto>> getReniecDataByDni(@PathVariable String dni) {
                log.info("🔍 Consultando datos de RENIEC para DNI: {}", dni);

                // Validar formato del DNI
                if (dni == null || dni.length() != 8 || !dni.matches("\\d{8}")) {
                        log.warn("⚠️ DNI inválido por formato: {}", dni);
                        return Mono.just(ApiResponse.error("DNI debe tener exactamente 8 dígitos numéricos"));
                }

                return reniecClient.getPersonalDataByDni(dni)
                                .map(personalData -> {
                                        // Convertir PersonalDataDto a ReniecResponseDto para mantener consistencia con
                                        // el frontend
                                        ReniecResponseDto reniecResponse = new ReniecResponseDto();
                                        reniecResponse.setFirstName(personalData.getFirstName());
                                        reniecResponse.setFirstLastName(personalData.getFirstLastName());
                                        reniecResponse.setSecondLastName(personalData.getSecondLastName());
                                        reniecResponse.setFullName(personalData.getFullName());
                                        reniecResponse.setDocumentNumber(personalData.getDocumentNumber());

                                        log.info("✅ Datos de RENIEC obtenidos exitosamente para DNI: {}", dni);
                                        return ApiResponse.success("Datos de RENIEC obtenidos exitosamente",
                                                        reniecResponse);
                                })
                                .onErrorResume(error -> {
                                        log.error("❌ Error consultando RENIEC para DNI {}: {}", dni,
                                                        error.getMessage());

                                        String errorMessage;
                                        if (error.getMessage().contains("DNI inválido")) {
                                                errorMessage = "DNI inválido o con formato incorrecto";
                                        } else if (error.getMessage().contains("No se encontraron datos")) {
                                                errorMessage = "No se encontraron datos para el DNI proporcionado en RENIEC";
                                        } else if (error.getMessage().contains("no disponible")) {
                                                errorMessage = "Servicio de RENIEC no disponible temporalmente";
                                        } else {
                                                errorMessage = "Error al consultar datos de RENIEC: "
                                                                + error.getMessage();
                                        }

                                        return Mono.just(ApiResponse.<ReniecResponseDto>error(errorMessage));
                                });
        }

        /**
         * Verificar si un DNI ya está registrado
         * GET /api/common/user/dni/{dni}/exists
         */
        @GetMapping("/user/dni/{dni}/exists")
        public Mono<ApiResponse<Boolean>> checkDniExists(@PathVariable String dni) {
                log.info("🔍 Verificando si DNI existe: {}", dni);

                return userService.getUserByDocumentNumber(dni)
                                .map(response -> {
                                        boolean exists = response.getData(); // ✅ CORREGIDO: usar getData() no
                                                                             // isSuccess()
                                        log.info("📋 DNI {} {} en el sistema", dni, exists ? "EXISTE" : "NO EXISTE");
                                        return ApiResponse.success("Verificación de DNI completada", exists);
                                })
                                .onErrorReturn(ApiResponse.success("Verificación de DNI completada", false));
        }

        /**
         * Verificar si un teléfono ya está registrado
         * GET /api/common/user/phone/{phone}/exists
         */
        @GetMapping("/user/phone/{phone}/exists")
        public Mono<ApiResponse<Boolean>> checkPhoneExists(@PathVariable String phone) {
                log.info("🔍 Verificando si teléfono existe: {}", phone);

                return userService.getUserByPhone(phone)
                                .map(response -> {
                                        boolean exists = response.getData(); // ✅ CORREGIDO: usar getData() no
                                                                             // isSuccess()
                                        log.info("📱 Teléfono {} {} en el sistema", phone,
                                                        exists ? "EXISTE" : "NO EXISTE");
                                        return ApiResponse.success("Verificación de teléfono completada", exists);
                                })
                                .onErrorReturn(ApiResponse.success("Verificación de teléfono completada", false));
        }

        /**
         * Verificar si un email ya está registrado
         * GET /api/common/user/email/{email}/exists
         */
        @GetMapping("/user/email/{email}/exists")
        public Mono<ApiResponse<Boolean>> checkEmailExists(@PathVariable String email) {
                log.info("🔍 Verificando si email existe: {}", email);

                return userService.getUserByEmail(email)
                                .map(response -> {
                                        boolean exists = response.getData(); // ✅ CORREGIDO: usar getData() no
                                                                             // isSuccess()
                                        log.info("📧 Email {} {} en el sistema", email,
                                                        exists ? "EXISTE" : "NO EXISTE");
                                        return ApiResponse.success("Verificación de email completada", exists);
                                })
                                .onErrorReturn(ApiResponse.success("Verificación de email completada", false));
        }
}