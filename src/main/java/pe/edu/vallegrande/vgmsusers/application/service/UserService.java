package pe.edu.vallegrande.vgmsusers.application.service;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import pe.edu.vallegrande.vgmsusers.domain.enums.RolesUsers;
import pe.edu.vallegrande.vgmsusers.domain.enums.UserStatus;
import pe.edu.vallegrande.vgmsusers.infrastructure.dto.ApiResponse;
import pe.edu.vallegrande.vgmsusers.infrastructure.dto.request.CreateUserRequest;
import pe.edu.vallegrande.vgmsusers.infrastructure.dto.request.UpdateUserPatchRequest;
import pe.edu.vallegrande.vgmsusers.infrastructure.dto.request.UpdateUserRequest;
import pe.edu.vallegrande.vgmsusers.infrastructure.dto.response.UserCreationResponse;
import pe.edu.vallegrande.vgmsusers.infrastructure.dto.response.UserResponse;
import pe.edu.vallegrande.vgmsusers.infrastructure.dto.response.CompleteUserResponse;
import reactor.core.publisher.Mono;

import java.util.List;

/**
 * Servicio principal para gestión de usuarios
 * Implementa todas las operaciones CRUD con generación automática de códigos
 */
public interface UserService {

     /**
      * Crear un nuevo usuario con código generado automáticamente
      */
     Mono<ApiResponse<UserResponse>> createUser(CreateUserRequest request);

     /**
      * Crear un nuevo usuario con código generado automáticamente y retornar
      * credenciales
      */
     Mono<ApiResponse<UserCreationResponse>> createUserWithCredentials(CreateUserRequest request);

     /**
      * Obtener usuario por ID
      */
     Mono<ApiResponse<UserResponse>> getUserById(String id);

     /**
      * Obtener usuario por código
      */
     Mono<ApiResponse<UserResponse>> getUserByCode(String userCode);

     /**
      * Obtener usuario por username (para MS-AUTHENTICATION)
      */
     Mono<ApiResponse<UserResponse>> getUserByUsername(String username);

     /**
      * Listar usuarios por organización con paginación
      */
     Mono<ApiResponse<Page<UserResponse>>> getUsersByOrganization(String organizationId, Pageable pageable);

     /**
      * Listar usuarios por rol
      */
     Mono<ApiResponse<List<UserResponse>>> getUsersByRole(String organizationId, RolesUsers role);

     /**
      * Actualizar usuario
      */
     Mono<ApiResponse<UserResponse>> updateUser(String id, UpdateUserRequest request);

     /**
      * Actualización parcial de usuario (solo campos específicos)
      */
     Mono<ApiResponse<UserResponse>> patchUser(String id, UpdateUserPatchRequest request);

     /**
      * Eliminar usuario (soft delete - cambiar a INACTIVE)
      */
     Mono<ApiResponse<Void>> deleteUser(String id);

     /**
      * Eliminar usuario físicamente (hard delete - remover de BD)
      */
     Mono<ApiResponse<Void>> deleteUserPermanently(String id);

     /**
      * Restaurar usuario eliminado (cambiar de INACTIVE a ACTIVE)
      */
     Mono<ApiResponse<UserResponse>> restoreUser(String id);

     /**
      * Listar usuarios activos por organización
      */
     Mono<ApiResponse<List<UserResponse>>> getActiveUsersByOrganization(String organizationId);

     /**
      * Listar usuarios inactivos por organización
      */
     Mono<ApiResponse<List<UserResponse>>> getInactiveUsersByOrganization(String organizationId);

     /**
      * Listar todos los usuarios (activos e inactivos) por organización
      */
     Mono<ApiResponse<List<UserResponse>>> getAllUsersByOrganization(String organizationId);

     /**
      * Cambiar estado de usuario
      */
     Mono<ApiResponse<UserResponse>> changeUserStatus(String id, UserStatus status);

     /**
      * Verificar si un email está disponible
      */
     Mono<ApiResponse<Boolean>> getUserByEmail(String email);

     /**
      * Verificar si un DNI ya está registrado
      */
     Mono<ApiResponse<Boolean>> getUserByDocumentNumber(String documentNumber);

     /**
      * Verificar si un teléfono ya está registrado
      */
     Mono<ApiResponse<Boolean>> getUserByPhone(String phone);

     Mono<ApiResponse<Boolean>> getEmailAvailability(String email);

     /**
      * NUEVO: Contar usuarios con rol SUPER_ADMIN (para validar primer usuario)
      */
     Mono<Long> countSuperAdmins();

     /**
      * MÉTODOS PARA INFORMACIÓN COMPLETA (INTERNOS CON JWE)
      */

     /**
      * Obtener información completa de usuarios por organización (para APIs
      * internas)
      */
     Mono<ApiResponse<List<CompleteUserResponse>>> getCompleteUsersByOrganization(String organizationId);

     /**
      * Obtener información completa de usuarios por rol (para APIs internas)
      */
     Mono<ApiResponse<List<CompleteUserResponse>>> getCompleteUsersByRole(String organizationId, RolesUsers role);

     /**
      * Obtener información completa de un usuario específico (para APIs internas)
      */
     Mono<ApiResponse<CompleteUserResponse>> getCompleteUserById(String userId);

     /**
      * Obtener información completa de un usuario específico INCLUYENDO usuarios
      * eliminados (para APIs internas)
      */
     Mono<ApiResponse<CompleteUserResponse>> getCompleteUserByIdIncludingDeleted(String userId);
}