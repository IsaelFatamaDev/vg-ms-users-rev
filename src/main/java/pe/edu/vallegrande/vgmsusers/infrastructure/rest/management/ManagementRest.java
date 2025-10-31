package pe.edu.vallegrande.vgmsusers.infrastructure.rest.management;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import pe.edu.vallegrande.vgmsusers.application.service.UserService;
import pe.edu.vallegrande.vgmsusers.application.service.UserCodeService;
import pe.edu.vallegrande.vgmsusers.infrastructure.client.ReniecClient;
import pe.edu.vallegrande.vgmsusers.infrastructure.dto.request.CreateUserRequest;
import pe.edu.vallegrande.vgmsusers.infrastructure.dto.response.UserResponse;
import pe.edu.vallegrande.vgmsusers.infrastructure.dto.response.UserCreationResponse;
import pe.edu.vallegrande.vgmsusers.infrastructure.dto.ApiResponse;
import pe.edu.vallegrande.vgmsusers.infrastructure.exception.ValidationException;
import pe.edu.vallegrande.vgmsusers.domain.enums.RolesUsers;
import pe.edu.vallegrande.vgmsusers.domain.enums.UserStatus;
import reactor.core.publisher.Mono;

import jakarta.validation.Valid;
import java.util.List;

/**
 * REST Controller SEGURO para funciones de SUPER_ADMIN
 * - Solo accesible para usuarios con rol SUPER_ADMIN
 * - Validación JWT obligatoria
 * - Permite gestionar administradores y todas las funciones del sistema
 */
@RestController
@RequestMapping("/api/management")
@RequiredArgsConstructor
@Slf4j
public class ManagementRest {

    private final UserService userService;
    private final UserCodeService userCodeService;
    private final ReniecClient reniecService;

    /**
     * Crear administradores o super administradores (solo SUPER_ADMIN)
     * POST /api/management/admins
     * Requiere rol SUPER_ADMIN (validado a nivel de clase)
     * Valida DNI con RENIEC y usa datos reales para nombres
     */
    @PostMapping("/admins")
    public Mono<ApiResponse<UserCreationResponse>> createAdmin(
            @Valid @RequestBody CreateUserRequest request) { // Recibir contexto de seguridad

        // Validar DNI con RENIEC y obtener datos reales
        log.info("[SUPER_ADMIN] Validando DNI {} con RENIEC", request.getDocumentNumber());
        return reniecService.getPersonalDataByDni(request.getDocumentNumber())
                .doOnNext(reniecData -> {
                    log.info("[SUPER_ADMIN] 📄 Datos RENIEC recibidos para administrador: {}",
                            reniecData);

                    // Actualizar request con datos reales de RENIEC
                    request.setFirstName(reniecData.getFirstName());
                    request.setLastName(
                            reniecData.getFirstLastName() + " " + reniecData.getSecondLastName());

                    // Generar username inteligente basado en datos RENIEC
                    String generatedUsername = generateIntelligentUsername(
                            reniecData.getFirstName(),
                            reniecData.getFirstLastName(),
                            reniecData.getSecondLastName());

                    // Email es obligatorio para Keycloak - usar username generado si no se
                    // proporciona
                    if (request.getEmail() == null || request.getEmail().trim().isEmpty()) {
                        request.setEmail(generatedUsername); // Usar username como email para
                        // Keycloak
                    }

                    log.info("[SUPER_ADMIN] Username generado: {}", generatedUsername);
                })
                .onErrorMap(error -> {
                    log.error("[SUPER_ADMIN] Error validando DNI con RENIEC: {}",
                            error.getMessage());
                    return new ValidationException(
                            "DNI no válido según RENIEC: " + error.getMessage());
                })
                .flatMap(reniecData -> {
                    // Si no se especifican roles, asignar ADMIN por defecto
                    if (request.getRoles() == null || request.getRoles().isEmpty()) {
                        request.setRoles(java.util.Set.of(RolesUsers.ADMIN));
                        log.info("[SUPER_ADMIN] Asignando rol ADMIN por defecto");
                    }

                    // Validar que solo se pueden crear ADMIN o SUPER_ADMIN
                    boolean hasValidRole = request.getRoles().stream()
                            .allMatch(role -> role == RolesUsers.ADMIN || role == RolesUsers.SUPER_ADMIN);

                    if (!hasValidRole) {
                        log.error("[SUPER_ADMIN] Intento de crear usuario con roles inválidos: {}",
                                request.getRoles());
                        throw new ValidationException(
                                "Este endpoint solo permite crear usuarios con rol ADMIN o SUPER_ADMIN");
                    }

                    log.info("[SUPER_ADMIN] Creando administrador con datos validados de RENIEC");
                    return userService.createUserWithCredentials(request);
                })
                .flatMap(response -> {
                    if (response.isSuccess()) {
                        UserCreationResponse data = response.getData();

                        return Mono.just(
                                ApiResponse.success("Administrador creado exitosamente", response.getData()));
                    } else {
                        log.error("[SUPER_ADMIN] Error creando administrador: {}", response.getMessage());
                        return Mono.error(new ValidationException(
                                "Error creando administrador: " + response.getMessage()));
                    }
                });
    }

    /**
     * Listar todos los administradores del sistema
     * GET /api/management/admins
     */
    @GetMapping("/admins")
    public Mono<ApiResponse<Page<UserResponse>>> getAllAdmins(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String organizationId) {

        Pageable pageable = PageRequest.of(page, size);

        if (organizationId != null) {
            return userService.getUsersByRole(organizationId, RolesUsers.ADMIN)
                    .flatMap(response -> {
                        if (response.isSuccess()) {
                            Page<UserResponse> pageResponse = new org.springframework.data.domain.PageImpl<>(
                                    response.getData(), pageable, response.getData().size());
                            return Mono.just(ApiResponse.success("Administradores obtenidos exitosamente",
                                    pageResponse));
                        } else {
                            return Mono.error(new ValidationException(
                                    "Error obteniendo administradores: " + response.getMessage()));
                        }
                    });
        }

        return Mono.just(ApiResponse.success("Funcionalidad en desarrollo", null));
    }

    /**
     * Cambiar estado de cualquier usuario (privilegio de SUPER_ADMIN)
     * PATCH /api/management/users/{id}/status
     */
    @PatchMapping("/users/{id}/status")
    public Mono<ApiResponse<UserResponse>> changeAnyUserStatus(
            @PathVariable String id,
            @RequestParam UserStatus status) {

        log.info("[SUPER_ADMIN] Cambiando estado del usuario {} a {}", id, status);

        return userService.changeUserStatus(id, status)
                .flatMap(response -> {
                    if (response.isSuccess()) {
                        return Mono.just(ApiResponse.success("Estado del usuario cambiado exitosamente",
                                response.getData()));
                    } else {
                        return Mono.error(new ValidationException(
                                "Error cambiando estado del usuario: " + response.getMessage()));
                    }
                });
    }

    /**
     * Eliminar cualquier usuario (privilegio de SUPER_ADMIN)
     * DELETE /api/management/users/{id}
     */
    @DeleteMapping("/users/{id}")
    public Mono<ApiResponse<Void>> deleteAnyUser(@PathVariable String id) {
        log.warn("[SUPER_ADMIN] Eliminando usuario: {}", id);

        return userService.deleteUser(id)
                .flatMap(response -> {
                    if (response.isSuccess()) {
                        return Mono.just(ApiResponse.success("Usuario eliminado exitosamente", null));
                    } else {
                        return Mono.error(
                                new ValidationException("Error eliminando usuario: " + response.getMessage()));
                    }
                });
    }

    /**
     * Gestionar códigos de usuarios - reiniciar contadores
     * DELETE /api/management/user-codes/reset/{organizationId}
     */
    @DeleteMapping("/user-codes/reset/{organizationId}")
    public Mono<ApiResponse<Void>> resetUserCodeCounter(@PathVariable String organizationId) {
        log.warn("[SUPER_ADMIN] Reiniciando contador de códigos para organización: {}", organizationId);

        return userCodeService.resetCounter(organizationId)
                .then(Mono.just(ApiResponse.success("Contador reiniciado exitosamente", null)));
    }

    /**
     * Eliminar administrador permanentemente (hard delete)
     * DELETE /api/management/admins/{id}/permanent
     */
    @DeleteMapping("/admins/{id}/permanent")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public Mono<ApiResponse<Void>> deleteAdminPermanently(@PathVariable String id) {
        log.info("SUPER_ADMIN eliminando administrador permanentemente: {}", id);

        return userService.deleteUserPermanently(id)
                .flatMap(response -> {
                    if (response.isSuccess()) {
                        return Mono.just(ApiResponse.success("Administrador eliminado permanentemente", null));
                    } else {
                        return Mono.error(new ValidationException(
                                "Error eliminando administrador: " + response.getMessage()));
                    }
                });
    }

    /**
     * Restaurar administrador eliminado
     * PUT /api/management/admins/{id}/restore
     */
    @PutMapping("/admins/{id}/restore")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public Mono<ApiResponse<UserResponse>> restoreAdmin(@PathVariable String id) {
        log.info("SUPER_ADMIN restaurando administrador: {}", id);

        return userService.restoreUser(id)
                .flatMap(response -> {
                    if (response.isSuccess()) {
                        return Mono.just(ApiResponse.success("Administrador restaurado exitosamente",
                                response.getData()));
                    } else {
                        return Mono.error(new ValidationException(
                                "Error restaurando administrador: " + response.getMessage()));
                    }
                });
    }

    /**
     * Listar administradores activos
     * GET /api/management/admins/active
     */
    @GetMapping("/admins/active")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public Mono<ApiResponse<List<UserResponse>>> getActiveAdmins() {
        log.info("SUPER_ADMIN consultando administradores activos");

        return userService.getUsersByRole("", RolesUsers.ADMIN)
                .flatMap(response -> {
                    if (response.isSuccess()) {
                        return Mono.just(ApiResponse.success("Administradores activos obtenidos exitosamente",
                                response.getData()));
                    } else {
                        return Mono.error(new ValidationException(
                                "Error obteniendo administradores activos: " + response.getMessage()));
                    }
                });
    }

    /**
     * Listar administradores inactivos
     * GET /api/management/admins/inactive
     */
    @GetMapping("/admins/inactive")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public Mono<ApiResponse<List<UserResponse>>> getInactiveAdmins() {
        log.info("SUPER_ADMIN consultando administradores inactivos");

        // TODO: Implementar método para obtener usuarios inactivos por rol
        return Mono.just(ApiResponse.success("Funcionalidad en desarrollo", null));
    }

    /**
     * Ver estadísticas globales del sistema (privilegio de SUPER_ADMIN)
     * GET /api/management/stats
     */
    @GetMapping("/stats")
    public Mono<ApiResponse<Object>> getSystemStats() {
        // TODO: Implementar estadísticas del sistema
        return Mono.just(ApiResponse.success("Estadísticas del sistema en desarrollo", null));
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
        log.debug("Generando username para: {} | {} | {}", firstName, firstLastName, secondLastName);

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
        log.info("Username generado: {} para persona: {} {} {}",
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
                log.debug("Palabra válida encontrada en primer apellido: {}", word);
                return word;
            }
        }

        // Si todas las palabras son de 2 letras o menos, usar la última
        String lastWord = words[words.length - 1];
        log.debug("Usando última palabra del primer apellido: {}", lastWord);
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
                .replaceAll("Á", "A").replaceAll("É", "E").replaceAll("Í", "I")
                .replaceAll("Ó", "O").replaceAll("Ú", "U").replaceAll("Ñ", "N")
                .replaceAll("á", "a").replaceAll("é", "e").replaceAll("í", "i")
                .replaceAll("ó", "o").replaceAll("ú", "u").replaceAll("ñ", "n")
                .replaceAll("[^A-Za-z0-9\\s]", "");
    }
}